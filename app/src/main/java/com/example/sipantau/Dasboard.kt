package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardBinding
import com.example.sipantau.model.UserData
import com.google.gson.Gson

class Dasboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Cek apakah user masih login
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // 🔹 Tampilkan nama user
        showLoggedInUserName()

        // 🔹 Logout jika gambar profil ditekan
        binding.gambarProfil.setOnClickListener {
            logoutUser()
        }
    }

    /**
     * Cek apakah user masih login (token masih ada)
     */
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    /**
     * Tampilkan nama user dari SharedPreferences
     */
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

    /**
     * Logout user: hapus token & data user
     */
    private fun logoutUser() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        prefs.edit()
            .remove(LoginActivity.PREF_TOKEN)
            .remove(LoginActivity.PREF_USER)
            .apply()

        navigateToLogin()
    }

    /**
     * Navigasi ke halaman login
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
