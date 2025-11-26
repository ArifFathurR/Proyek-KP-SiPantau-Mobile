package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.KegiatanSayaBinding
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.api.ApiClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PantauAktivitasPML : AppCompatActivity() {

    private lateinit var binding: KegiatanSayaBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter

    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KegiatanSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabs()

        // Default: ambil dari API
        loadKegiatanFromAPI()


    }

    private fun setupRecyclerView() {
        kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
            if (kegiatan.id_pml != null) {
                val intent = Intent(this, ApprovalLaporanPCL::class.java)
                intent.putExtra("id_pml", kegiatan.id_pml)
                intent.putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses)
                startActivity(intent)
            } else {
                Toast.makeText(this, "ID PML tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@PantauAktivitasPML)
            adapter = kegiatanAdapter
        }
    }

    private fun setupTabs() {
        binding.tabAktif.setOnClickListener {
            kegiatanAdapter.updateData(listAktif)
            binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        binding.tabTidakAktif.setOnClickListener {
            kegiatanAdapter.updateData(listTidakAktif)
            binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }
    }

    // ============================================================
    // 🔵 Ambil dari API seperti contoh yang kamu berikan
    // ============================================================
    private fun loadKegiatanFromAPI() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val auth = "Bearer $token"

        ApiClient.instance.getKegiatan(auth)
            .enqueue(object : Callback<KegiatanResponse> {

                override fun onResponse(
                    call: Call<KegiatanResponse>,
                    response: Response<KegiatanResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.kegiatan_pml ?: emptyList()

                        listAktif = data.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }

                        kegiatanAdapter.updateData(listAktif)

                    } else {
                        Toast.makeText(
                            this@PantauAktivitasPML,
                            "Gagal memuat kegiatan dari server",
                            Toast.LENGTH_SHORT
                        ).show()

                        // fallback offline
                        loadFromLocal()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(
                        this@PantauAktivitasPML,
                        "Tidak bisa terhubung: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // fallback offline
                    loadFromLocal()
                }
            })
    }

    // ============================================================
    // 🟡 Fallback: ambil dari local Room jika offline
    // ============================================================
    private fun loadFromLocal() {
        val repo = KegiatanRepository(this)

        lifecycleScope.launch {
            val data: List<KegiatanEntity> = repo.getKegiatan()

            if (data.isEmpty()) {
                Toast.makeText(
                    this@PantauAktivitasPML,
                    "⚠️ Offline. Data lokal kosong.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val models = data.map { it.toKegiatanModel() }

            listAktif = models.filter { it.status_kegiatan == "aktif" }
            listTidakAktif = models.filter { it.status_kegiatan == "tidak aktif" }

            kegiatanAdapter.updateData(listAktif)
        }
    }

    private fun KegiatanEntity.toKegiatanModel() = Kegiatan(
        id_pcl = this.id_pcl,
        id_pml = this.id_pml,
        id_kegiatan_detail_proses = this.id_kegiatan_detail_proses,
        target = this.target,
        status_approval = this.status_approval,
        nama_kegiatan = this.nama_kegiatan,
        nama_kegiatan_detail_proses = this.nama_kegiatan_detail_proses,
        tanggal_mulai = this.tanggal_mulai,
        tanggal_selesai = this.tanggal_selesai,
        nama_kabupaten = this.nama_kabupaten,
        status_kegiatan = this.status_kegiatan,
        keterangan_wilayah = this.keterangan_wilayah
    )

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
