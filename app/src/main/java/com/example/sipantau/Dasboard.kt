package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardBinding
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.UserData
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Dasboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Cek login
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // ✅ Tampilkan nama user
        showLoggedInUserName()

        // ✅ Siapkan RecyclerView dan klik item
        kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
            // Ambil id_pcl dari item yang diklik
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
            layoutManager = LinearLayoutManager(this@Dasboard)
            adapter = kegiatanAdapter
        }

        // ✅ Cek koneksi & load data
        binding.root.post {
            if (!isOnline()) {
                Toast.makeText(
                    this@Dasboard,
                    "⚠️ Kamu sedang offline. Beberapa fitur mungkin tidak tersedia.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                loadKegiatan()
            }
        }

        // 🔹 Tombol logout
        binding.gambarProfil.setOnClickListener {
            logoutUser()
        }

        binding.btnPantauProgres.setOnClickListener {
            val intent= Intent(this, KegiatanSaya::class.java)
            startActivity(intent)
        }

        // 🔹 Tab filter kegiatan
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

    /** 🔹 Load data kegiatan dari API */
    private fun loadKegiatan() {
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
                        val all = kegiatanResponse.kegiatan

                        listAktif = all.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = all.filter { it.status_kegiatan == "tidak aktif" }

                        kegiatanAdapter.updateData(listAktif)
                    } else {
                        Toast.makeText(
                            this@Dasboard,
                            "Gagal memuat data kegiatan (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(
                        this@Dasboard,
                        "Gagal terhubung ke server: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** 🔹 Cek login */
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    /** 🔹 Tampilkan nama user */
    private fun showLoggedInUserName() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        if (userJson != null) {
            val user = Gson().fromJson(userJson, UserData::class.java)
            binding.nama.text = "Halo, ${user.nama_user}"
        } else {
            binding.nama.text = "Halo, Pengguna"
        }
    }

    /** 🔹 Logout */
    private fun logoutUser() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        prefs.edit()
            .remove(LoginActivity.PREF_TOKEN)
            .remove(LoginActivity.PREF_USER)
            .apply()
        navigateToLogin()
    }

    /** 🔹 Navigasi ke login */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** 🔹 Deteksi koneksi internet */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
