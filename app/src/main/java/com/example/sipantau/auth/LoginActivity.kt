package com.example.sipantau.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.Dasboard
import com.example.sipantau.DashboardPML
import com.example.sipantau.auth.Role
import com.example.sipantau.api.ApiClient
import com.example.sipantau.databinding.ActivityLoginBinding
import com.example.sipantau.model.UserData
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        const val PREF_NAME = "MyAppPref"
        const val PREF_TOKEN = "TOKEN"
        const val PREF_USER = "USER_DATA"
        const val PREF_ACTIVE_ROLE = "ACTIVE_ROLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(PREF_TOKEN, null)
        val userJson = prefs.getString(PREF_USER, null)
        val activeRole = prefs.getString(PREF_ACTIVE_ROLE, null)

        // Jika sudah login + role tersimpan -> langsung masuk sesuai role
        if (!token.isNullOrEmpty() && !userJson.isNullOrEmpty() && !activeRole.isNullOrEmpty()) {
            when (activeRole) {
                "PML" -> startActivity(Intent(this, DashboardPML::class.java))
                "PCL" -> startActivity(Intent(this, Dasboard::class.java))
                else -> startActivity(Intent(this, Role::class.java))
            }
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
        // Tampilkan loading
        setLoadingState(true)

        ApiClient.instance.login(email, password)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    // Sembunyikan loading
                    setLoadingState(false)

                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body != null && body.status == "success") {

                            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            val editor = prefs.edit()

                            // Simpan token + user json
                            editor.putString(PREF_TOKEN, body.token)
                            editor.putString(PREF_USER, Gson().toJson(body.user))

                            // Tentukan role awal jika hanya memiliki 1 role
                            val user = body.user
                            val hasPml = !user?.id_pml.isNullOrEmpty()
                            val hasPcl = !user?.id_pcl.isNullOrEmpty()

                            when {
                                hasPml && hasPcl -> {
                                    // Simpan token & user, tapi biarkan user memilih role di layar Role
                                    editor.remove(PREF_ACTIVE_ROLE)
                                    editor.apply()
                                    startActivity(Intent(this@LoginActivity, Role::class.java))
                                }
                                hasPml -> {
                                    editor.putString(PREF_ACTIVE_ROLE, "PML")
                                    editor.apply()
                                    startActivity(Intent(this@LoginActivity, DashboardPML::class.java))
                                }
                                hasPcl -> {
                                    editor.putString(PREF_ACTIVE_ROLE, "PCL")
                                    editor.apply()
                                    startActivity(Intent(this@LoginActivity, Dasboard::class.java))
                                }
                                else -> {
                                    // Jika user tidak punya role yang valid, default ke Dasboard
                                    editor.remove(PREF_ACTIVE_ROLE)
                                    editor.apply()
                                    startActivity(Intent(this@LoginActivity, Dasboard::class.java))
                                }
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                "Login berhasil, selamat datang ${user?.nama_user}",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                body?.message ?: "Email atau password salah",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> "Email atau password salah"
                            404 -> "User tidak ditemukan"
                            else -> "Terjadi kesalahan (${response.code()})"
                        }
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // leazy loading
                    setLoadingState(false)
                    Toast.makeText(this@LoginActivity, "Gagal koneksi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "" else "Log In"

        // Tampilkan/sembunyikan ProgressBar
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Disable input fields saat loading
        binding.edtIdPengguna.isEnabled = !isLoading
        binding.edtPassword.isEnabled = !isLoading
    }
}