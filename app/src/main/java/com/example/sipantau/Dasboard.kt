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
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.auth.Role
import com.example.sipantau.databinding.DashboardBinding
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.TotalKegPClResponse
import com.example.sipantau.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.launch

class Dasboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ====== CEK LOGIN ======
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }



        showLoggedInUserName()

        // ====== SETUP ADAPTER ======
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
            layoutManager = LinearLayoutManager(this@Dasboard)
            adapter = kegiatanAdapter
        }

        // Load data offline-online
        binding.root.post { loadKegiatan() }

        // ====== BUTTON LISTENER ======
        binding.gambarProfil.setOnClickListener { logoutUser() }

        binding.btnPantauAktivitas.setOnClickListener {
            startActivity(Intent(this, KegiatanSaya::class.java))
        }

        binding.btnPantauProgress.setOnClickListener {
            startActivity(Intent(this, ProgresKegiatanSaya::class.java))
        }

        binding.btnKinerjaHarian.setOnClickListener {
            val intent = Intent(this, KinerjaHarian::class.java)
            startActivity(intent)
        }

        binding.btnFeedback.setOnClickListener {
            val intent = Intent(this, FeedbackUser::class.java)
            startActivity(intent)
        }

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

        loadTotalKegiatanPcl()
    }

    // ====================================================================

    private fun loadKegiatan() {
        val repo = KegiatanRepository(this)

        lifecycleScope.launch {
            val data: List<KegiatanEntity> = repo.getKegiatan()

            listAktif = data.filter { it.status_kegiatan == "aktif" }.map { it.toKegiatanModel() }
            listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }.map { it.toKegiatanModel() }

            kegiatanAdapter.updateData(listAktif)

            if (data.isEmpty() && !isOnline()) {
                Toast.makeText(
                    this@Dasboard,
                    "⚠️ Kamu sedang offline. Data lokal kosong.",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    // ====================================================================
    // LOGIN HANDLER
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    // CEK APAKAH ID PML ADA
    private fun isUserPML(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        if (userJson != null) {
            val user = Gson().fromJson(userJson, UserData::class.java)
            return user.id_pml != null  // Jika id_pml tidak null → user adalah PML
        }
        return false
    }

    private fun showLoggedInUserName() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        binding.nama.text = if (userJson != null) {
            val user = Gson().fromJson(userJson, UserData::class.java)
            "Halo, ${user.nama_user}"
        } else "Halo, Pengguna"
    }

    private fun logoutUser() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(LoginActivity.PREF_TOKEN)
            .remove(LoginActivity.PREF_USER)
            .apply()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ====================================================================

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadTotalKegiatanPcl() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        if (token.isNullOrEmpty()) return

        ApiClient.instance.getTotalKegPcl("Bearer $token").enqueue(object : retrofit2.Callback<TotalKegPClResponse> {
            override fun onResponse(
                call: retrofit2.Call<TotalKegPClResponse>,
                response: retrofit2.Response<TotalKegPClResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val total = response.body()!!.total_kegiatan_pcl
                    // tampilkan ke TextView
                    binding.jmlKeg.text = total.toString()
                } else {
                    binding.jmlKeg.text = "0"
                }
            }

            override fun onFailure(call: retrofit2.Call<TotalKegPClResponse>, t: Throwable) {
                binding.jmlKeg.text = "0"
            }
        })
    }

}
