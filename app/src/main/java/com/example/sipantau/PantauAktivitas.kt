package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PelaporanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityMainBinding
import com.example.sipantau.model.LaporanData
import com.example.sipantau.model.PelaporanResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PantauAktivitas : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pelaporanAdapter: PelaporanAdapter
    private var idPcl: Int? = null
    private var idKegiatanDetailProses: Int? = null
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Ambil token dari SharedPreferences
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null).orEmpty()

        // ✅ Ambil id_pcl dan id_kegiatan_detail_proses dari Intent
        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        // ✅ Setup RecyclerView
        pelaporanAdapter = PelaporanAdapter(emptyList()) { laporan ->
            Toast.makeText(
                this,
                "Hapus laporan: ${laporan.nama_kegiatan_detail_proses}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@PantauAktivitas)
            adapter = pelaporanAdapter
        }

        // ✅ Tombol kembali
        binding.btnKembali.setOnClickListener { finish() }

        // ✅ Tombol tambah laporan
        binding.btnTambah.setOnClickListener {
            if (idPcl == null || idPcl == 0) {
                Toast.makeText(this, "ID PCL tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (idKegiatanDetailProses == null || idKegiatanDetailProses == 0) {
                Toast.makeText(this, "ID Kegiatan Detail Proses tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, TambahLaporan::class.java).apply {
                putExtra("id_pcl", idPcl)
                putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
            }
            startActivity(intent)
        }

        // ✅ Swipe Refresh untuk memuat ulang data
        binding.swipeRefresh.setOnRefreshListener {
            getPelaporan()
        }

        // ✅ Muat data awal
        getPelaporan()
    }

    private fun getPelaporan() {
        val apiService = ApiClient.instance

        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        binding.swipeRefresh.isRefreshing = true // tampilkan animasi refresh

        apiService.getLaporan("Bearer $token", idPcl).enqueue(object : Callback<PelaporanResponse> {
            override fun onResponse(
                call: Call<PelaporanResponse>,
                response: Response<PelaporanResponse>
            ) {
                binding.swipeRefresh.isRefreshing = false // hentikan animasi refresh

                if (response.isSuccessful) {
                    val laporanList = response.body()?.data ?: emptyList<LaporanData>()
                    pelaporanAdapter.updateData(laporanList)

                    if (laporanList.isEmpty()) {
                        Toast.makeText(this@PantauAktivitas, "Belum ada laporan.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(
                        this@PantauAktivitas,
                        "Gagal memuat laporan (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<PelaporanResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@PantauAktivitas,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // 🔁 Muat ulang data setiap kali halaman aktif lagi (setelah tambah laporan)
        getPelaporan()
    }
}
