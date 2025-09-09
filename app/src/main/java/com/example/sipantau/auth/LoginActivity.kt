package com.example.sipantau.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.MainActivity
import com.example.sipantau.api.ApiClient
import com.example.sipantau.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah user sudah login sebelumnya
        val prefs = getSharedPreferences("MyAppPref", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null)
        if (token != null) {
            // Kalau token ada → langsung masuk MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol login ditekan
        binding.btnLogin.setOnClickListener {
            val sobatId = binding.edtIdPengguna.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (sobatId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Sobat ID dan Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(sobatId, password)
        }
    }

    private fun doLogin(sobatId: String, password: String) {
        ApiClient.instance.login(sobatId, password)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.status == "success") {
                            // Simpan token ke SharedPreferences
                            val prefs = getSharedPreferences("MyAppPref", MODE_PRIVATE)
                            prefs.edit().putString("TOKEN", body.token).apply()

                            Toast.makeText(
                                this@LoginActivity,
                                "Login berhasil, selamat datang ${body.user.nama_user}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Pindah ke MainActivity
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                body?.message ?: "Login gagal",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
