package com.example.sipantau

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.KegiatanSayaBinding
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KegiatanSaya : AppCompatActivity() {

    private lateinit var binding: KegiatanSayaBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter

    private var listAktif = emptyList<Kegiatan>()
    private var listTidakAktif = emptyList<Kegiatan>()
    private var isDataLoaded = false // ✅ Flag mencegah double load

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KegiatanSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Setup RecyclerView
        kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
            val idPcl = kegiatan.id_pcl
            val idKegiatanDetailProses = kegiatan.id_kegiatan_detail_proses
            if (idPcl != null) {
                val intent = Intent(this, PantauAktivitas::class.java)
                intent.putExtra("id_pcl", idPcl)
                intent.putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
                startActivity(intent)
            } else {
                Toast.makeText(this, "ID PCL tidak ditemukan pada kegiatan ini", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@KegiatanSaya)
            adapter = kegiatanAdapter
        }

        // 🔹 Setup tab filter
        binding.tabAktif.setOnClickListener { showAktif() }
        binding.tabTidakAktif.setOnClickListener { showTidakAktif() }

        // 🔹 Load data kegiatan dari API
        loadKegiatan()
    }

    private fun showAktif() {
        kegiatanAdapter.updateData(listAktif)
        binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
        binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
    }

    private fun showTidakAktif() {
        kegiatanAdapter.updateData(listTidakAktif)
        binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
        binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
    }

    /** 🔹 Load data kegiatan dari API */
    private fun loadKegiatan() {
        if (isDataLoaded) return // ✅ Prevent double load

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        ApiClient.instance.getKegiatan("Bearer $token")
            .enqueue(object : Callback<KegiatanResponse> {
                override fun onResponse(
                    call: Call<KegiatanResponse>,
                    response: Response<KegiatanResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val kegiatanResponse = response.body()!!
                        val all = kegiatanResponse.kegiatan.distinctBy { it.id_kegiatan_detail_proses } // ✅ Remove duplicates

                        listAktif = all.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = all.filter { it.status_kegiatan == "tidak aktif" }

                        showAktif() // Tampilkan tab aktif default
                        isDataLoaded = true
                    } else {
                        Toast.makeText(
                            this@KegiatanSaya,
                            "Gagal memuat data kegiatan (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(
                        this@KegiatanSaya,
                        "Gagal terhubung ke server: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
