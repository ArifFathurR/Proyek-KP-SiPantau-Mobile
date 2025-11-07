package com.example.sipantau

import android.app.AlertDialog
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

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null).orEmpty()

        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        pelaporanAdapter = PelaporanAdapter(emptyList()) { laporan ->
            // ✅ Tampilkan dialog konfirmasi hapus
            AlertDialog.Builder(this)
                .setTitle("Hapus Laporan")
                .setMessage("Yakin ingin menghapus laporan '${laporan.resume}' ?")
                .setPositiveButton("Ya") { _, _ ->
                    hapusLaporan(laporan.id_sipantau_transaksi)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@PantauAktivitas)
            adapter = pelaporanAdapter
        }

        binding.btnKembali.setOnClickListener { finish() }

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

        binding.swipeRefresh.setOnRefreshListener { getPelaporan() }

        getPelaporan()
    }

    private fun getPelaporan() {
        val apiService = ApiClient.instance

        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        binding.swipeRefresh.isRefreshing = true

        apiService.getLaporan("Bearer $token", idPcl).enqueue(object : Callback<PelaporanResponse> {
            override fun onResponse(call: Call<PelaporanResponse>, response: Response<PelaporanResponse>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val laporanList = response.body()?.data ?: emptyList()
                    pelaporanAdapter.updateData(laporanList)
                    if (laporanList.isEmpty()) {
                        Toast.makeText(this@PantauAktivitas, "Belum ada laporan.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PantauAktivitas, "Gagal memuat laporan (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PelaporanResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@PantauAktivitas, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hapusLaporan(id: Int?) {
        if (id == null) {
            Toast.makeText(this, "ID laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = ApiClient.instance
        apiService.hapusLaporan("Bearer $token", id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PantauAktivitas, "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show()
                    getPelaporan() // refresh data setelah delete
                } else {
                    Toast.makeText(this@PantauAktivitas, "Gagal menghapus laporan (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@PantauAktivitas, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getPelaporan()
    }
}
