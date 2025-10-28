package com.example.sipantau.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.Dasboard
import com.example.sipantau.api.ApiClient
import com.example.sipantau.databinding.ActivityLoginBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        const val PREF_NAME = "MyAppPref"
        const val PREF_TOKEN = "TOKEN"
        const val PREF_USER = "USER_DATA"  // 🔹 untuk menyimpan data user
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(PREF_TOKEN, null)
        if (!token.isNullOrEmpty()) {
            startActivity(Intent(this, Dasboard::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.edtIdPengguna.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(email, password)
        }
    }

    private fun doLogin(email: String, password: String) {
        ApiClient.instance.login(email, password)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body != null && body.status == "success") {
                            // ✅ Login berhasil
                            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            val editor = prefs.edit()

                            editor.putString(PREF_TOKEN, body.token)
                            editor.putString(PREF_USER, Gson().toJson(body.user))
                            editor.apply()

                            Toast.makeText(
                                this@LoginActivity,
                                "Login berhasil, selamat datang ${body.user?.nama_user}",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(Intent(this@LoginActivity, Dasboard::class.java))
                            finish()

                        } else {
                            // ❌ Login gagal (user tidak ditemukan / password salah)
                            val message = body?.message ?: "Email atau password salah"
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        // ❌ Server error (contoh: 404, 500)
                        val errorMsg = when (response.code()) {
                            401 -> "Email atau password salah"
                            404 -> "User tidak ditemukan"
                            else -> "Terjadi kesalahan (${response.code()})"
                        }
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Gagal koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
