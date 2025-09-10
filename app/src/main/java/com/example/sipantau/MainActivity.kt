package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PelaporanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityMainBinding
import com.example.sipantau.model.PelaporanResponse
import com.example.sipantau.model.PelaporanWrapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PelaporanAdapter

    private val prefs by lazy { getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE) }
    private val token: String by lazy { prefs.getString(LoginActivity.PREF_TOKEN, "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Logout
        binding.btnKembali.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
        }

        binding.recylerView.layoutManager = LinearLayoutManager(this)
        fetchPelaporan()

        binding.btnTambah.setOnClickListener{
            startActivity(Intent(this, TambahLaporanActivity::class.java))
        }
    }

    private fun fetchPelaporan() {
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silahkan login kembali.", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.instance.getPelaporan("Bearer $token")
            .enqueue(object : Callback<PelaporanWrapper> {
                override fun onResponse(call: Call<PelaporanWrapper>, response: Response<PelaporanWrapper>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.tampildata ?: emptyList()
                        adapter = PelaporanAdapter(data)
                        binding.recylerView.adapter = adapter
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PelaporanWrapper>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }


}
