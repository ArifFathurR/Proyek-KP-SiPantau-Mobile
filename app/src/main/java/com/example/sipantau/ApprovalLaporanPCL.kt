package com.example.sipantau

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PclAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ApprovalLaporanPclBinding
import com.example.sipantau.model.ApproveData
import com.example.sipantau.model.ApproveResponse
import com.example.sipantau.model.Pcl
import com.example.sipantau.model.PclResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApprovalLaporanPCL : AppCompatActivity() {

    private lateinit var binding: ApprovalLaporanPclBinding
    private lateinit var pclAdapter: PclAdapter

    private var listBelum = listOf<Pcl>()
    private var listSudah = listOf<Pcl>()

    private var idPml: Int = 0
    private var idKegiatanDetailProses: Int = 0   // DIKIRIM KE KinerjaHarianDetail

    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ApprovalLaporanPclBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil ID dari Intent
        idPml = intent.getIntExtra("id_pml", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        if (idPml == 0) {
            Toast.makeText(this, "ID PML tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil token
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupTabs()
        loadPclFromApi()

        binding.btnKembali.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        pclAdapter = PclAdapter(
            data = emptyList(),
            onDetailClick = { item ->
                val idPclInt = item.id_pcl ?: 0
                if (idPclInt == 0) return@PclAdapter

                val intent = Intent(this, KinerjaHarianDetail::class.java).apply {
                    putExtra("id_pcl", idPclInt)
                    putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
                }
                startActivity(intent)
            },
            onApproveClick = { item ->
                showApproveDialog(item)
            }
        )

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@ApprovalLaporanPCL)
            adapter = pclAdapter
        }
    }

    private fun setupTabs() {
        binding.tabAktif.setOnClickListener {
            pclAdapter.updateData(listBelum)
            binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabAktif.text = "Belum (${listBelum.size})"
        }

        binding.tabTidakAktif.setOnClickListener {
            pclAdapter.updateData(listSudah)
            binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabTidakAktif.text = "Sudah (${listSudah.size})"
        }
    }

    private fun loadPclFromApi() {
        val auth = "Bearer $token"

        ApiClient.instance.getPcl(auth, idPml)
            .enqueue(object : Callback<PclResponse> {
                override fun onResponse(call: Call<PclResponse>, response: Response<PclResponse>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@ApprovalLaporanPCL,
                            "Gagal memuat data: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val data = response.body()?.data ?: emptyList()

                    listSudah = data.filter { it.status_approval == "1" || it.status_approval.equals("approved", ignoreCase = true) }
                    listBelum = data.filter { it.status_approval != "1" && !it.status_approval.equals("approved", ignoreCase = true) }

                    binding.tvTabAktif.text = "Belum (${listBelum.size})"
                    binding.tvTabTidakAktif.text = "Sudah (${listSudah.size})"

                    pclAdapter.updateData(listBelum)
                    binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
                    binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
                }

                override fun onFailure(call: Call<PclResponse>, t: Throwable) {
                    Toast.makeText(this@ApprovalLaporanPCL, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showApproveDialog(pcl: Pcl) {
        val persentaseValue = pcl.persentase.replace("%", "").toFloatOrNull() ?: 0f
        if (persentaseValue < 100f) {
            AlertDialog.Builder(this)
                .setTitle("Tidak Dapat Approve")
                .setMessage("PCL ${pcl.nama_pcl} belum mencapai target 100%.\n\nProgress saat ini: ${pcl.persentase}")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Approve")
            .setMessage(
                "Apakah Anda yakin ingin menyetujui PCL ini?\n\n" +
                        "Nama PCL: ${pcl.nama_pcl}\n" +
                        "Target: ${pcl.target}\n" +
                        "Realisasi: ${pcl.total_realisasi_kumulatif}\n" +
                        "Progress: ${pcl.persentase}"
            )
            .setPositiveButton("Ya, Setujui") { _, _ -> performApprove(pcl) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performApprove(pcl: Pcl) {
        val idPclInt = pcl.id_pcl ?: 0
        if (idPclInt == 0) return

        val body = mapOf("status_approval" to "1") // atau "1" sesuai backend

        binding.btnKembali.isEnabled = false
        val authHeader = "Bearer $token"

        ApiClient.instance.approvePcl(authHeader, idPclInt, body)
            .enqueue(object : Callback<ApproveResponse> {
                override fun onResponse(call: Call<ApproveResponse>, response: Response<ApproveResponse>) {
                    binding.btnKembali.isEnabled = true
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@ApprovalLaporanPCL, "Berhasil menyetujui ${pcl.nama_pcl}", Toast.LENGTH_SHORT).show()
                        loadPclFromApi()
                    } else {
                        val message = response.body()?.message ?: response.message()
                        Toast.makeText(this@ApprovalLaporanPCL, "Gagal approve: $message", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApproveResponse>, t: Throwable) {
                    binding.btnKembali.isEnabled = true
                    Toast.makeText(this@ApprovalLaporanPCL, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
