package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityMainBinding
import com.example.sipantau.model.DeleteResponse
import com.example.sipantau.model.PelaporanResponse
import com.example.sipantau.model.PelaporanWrapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PelaporanAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private val prefs by lazy { getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE) }
    private val token: String by lazy { prefs.getString(LoginActivity.PREF_TOKEN, "") ?: "" }

    private var isLoading = false // supaya tidak double load

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

        layoutManager = LinearLayoutManager(this)
        binding.recylerView.layoutManager = layoutManager

        adapter = PelaporanAdapter(mutableListOf()) { item, position ->
            deletePelaporan(item, position)
        }
        binding.recylerView.adapter = adapter

        // 🔥 load data pertama kali
        fetchPelaporan()


        // 🔥 SwipeRefreshLayout listener
        binding.swipeRefresh.setOnRefreshListener {
            // tampilkan animasi 2 detik
            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefresh.isRefreshing = false
                fetchPelaporan()
            }, 2000)
        }

        // 🔥 listener scroll: kalau ke paling atas, reload data
        binding.recylerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (firstVisibleItem == 0 && !isLoading) {
                    fetchPelaporan()
                }
            }
        })

        binding.btnTambah.setOnClickListener{
            startActivity(Intent(this, TambahLaporanActivity::class.java))
        }

    }

    private fun fetchPelaporan() {
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silahkan login kembali.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        ApiClient.instance.getPelaporan("Bearer $token")
            .enqueue(object : Callback<PelaporanWrapper> {
                override fun onResponse(call: Call<PelaporanWrapper>, response: Response<PelaporanWrapper>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val data = response.body()?.tampildata?.toMutableList() ?: mutableListOf()
                        adapter.updateData(data) // ✅ update data
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PelaporanWrapper>, t: Throwable) {
                    isLoading = false
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deletePelaporan(item: PelaporanResponse, position: Int) {
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silahkan login kembali.", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.instance.deletePelaporan("Bearer $token", item.id_sipantau_transaksi.toInt())
            .enqueue(object : Callback<DeleteResponse> {
                override fun onResponse(call: Call<DeleteResponse>, response: Response<DeleteResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.status) {
                            Toast.makeText(this@MainActivity, body.message, Toast.LENGTH_SHORT).show()
                            adapter.removeItem(position) // ✅ hapus langsung dari adapter
                        } else {
                            Toast.makeText(this@MainActivity, body?.message ?: "Gagal hapus data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal hapus data: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DeleteResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


}
