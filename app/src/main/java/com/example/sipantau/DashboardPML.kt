package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardPmlBinding
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardPML : AppCompatActivity() {

    private lateinit var binding: DashboardPmlBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardPmlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        showLoggedInUserName()

        kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
            val intent = Intent(this, PantauAktivitasPML::class.java)
            intent.putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses)
            intent.putExtra("id_pml", kegiatan.id_pml)
            startActivity(intent)
        }

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@DashboardPML)
            adapter = kegiatanAdapter
        }

        loadKegiatanFromAPI()

        binding.gambarProfil.setOnClickListener { logoutUser() }

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

    // ====================================================================
    private fun loadKegiatanFromAPI() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val auth = "Bearer $token"

        ApiClient.instance.getKegiatan(auth)
            .enqueue(object : Callback<KegiatanResponse> {
                override fun onResponse(call: Call<KegiatanResponse>, response: Response<KegiatanResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.kegiatan_pml ?: emptyList()

                        listAktif = data.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }

                        kegiatanAdapter.updateData(listAktif)
                    } else {
                        Toast.makeText(this@DashboardPML, "Gagal memuat kegiatan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(this@DashboardPML, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ====================================================================
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(LoginActivity.PREF_TOKEN, null).isNullOrEmpty()
    }

    private fun showLoggedInUserName() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        if (userJson != null) {
            val user = Gson().fromJson(userJson, com.example.sipantau.model.UserData::class.java)
            binding.nama.text = "Halo, ${user.nama_user}"
        }
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
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
