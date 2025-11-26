package com.example.sipantau

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PclAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ApprovalLaporanPclBinding
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

        pclAdapter = PclAdapter(emptyList()) { item ->

            val idPclInt = item.id_pcl
            if (idPclInt == null) {
                Toast.makeText(this, "ID PCL tidak valid: ${item.id_pcl}", Toast.LENGTH_SHORT).show()
                return@PclAdapter
            }

            val intent = Intent(this, KinerjaHarianDetail::class.java).apply {
                putExtra("id_pcl", idPclInt)
                putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
            }

            startActivity(intent)
        }

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@ApprovalLaporanPCL)
            adapter = pclAdapter
        }
    }

    private fun setupTabs() {
        // TAB BELUM APPROVAL
        binding.tabAktif.setOnClickListener {
            pclAdapter.updateData(listBelum)
            binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        // TAB SUDAH APPROVAL
        binding.tabTidakAktif.setOnClickListener {
            pclAdapter.updateData(listSudah)
            binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun loadPclFromApi() {
        val auth = "Bearer $token"

        ApiClient.instance.getPcl(auth, idPml)
            .enqueue(object : Callback<PclResponse> {
                override fun onResponse(call: Call<PclResponse>, response: Response<PclResponse>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@ApprovalLaporanPCL, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val data = response.body()?.data ?: emptyList()

                    // Filter berdasarkan status
                    listBelum = data.filter { it.status_approval == "0" }
                    listSudah = data.filter { it.status_approval == "1" }

                    // Set default tab = Belum
                    pclAdapter.updateData(listBelum)
                }

                override fun onFailure(call: Call<PclResponse>, t: Throwable) {
                    Toast.makeText(this@ApprovalLaporanPCL, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
