package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
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

        // ✅ Cek apakah user masih login
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // ✅ Tampilkan nama user
        showLoggedInUserName()

        // ⚠️ Cek koneksi dan tampilkan Toast setelah layout muncul
        binding.root.post {
            if (!isOnline()) {
                Toast.makeText(
                    this@Dasboard,
                    "⚠️ Kamu sedang offline. Beberapa fitur mungkin tidak tersedia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // 🔹 Logout jika gambar profil ditekan
        binding.gambarProfil.setOnClickListener {
            logoutUser()
        }
    }

    /** Cek apakah user masih login */
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    /** Tampilkan nama user */
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

    /** Logout user */
    private fun logoutUser() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        prefs.edit()
            .remove(LoginActivity.PREF_TOKEN)
            .remove(LoginActivity.PREF_USER)
            .apply()

        navigateToLogin()
    }

    /** Navigasi ke login */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Deteksi koneksi internet */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
