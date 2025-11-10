package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.ProgresAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityPantauProgresBinding
import com.example.sipantau.model.PantauProgresListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PantauProgres : AppCompatActivity() {
    private lateinit var binding: ActivityPantauProgresBinding
    private lateinit var progresAdapter: ProgresAdapter
    private var idPcl: Int? = null
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantauProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null).orEmpty()
        idPcl = intent.getIntExtra("id_pcl", 0)

        progresAdapter = ProgresAdapter(emptyList()) { progres ->
            AlertDialog.Builder(this)
                .setTitle("Hapus Progres")
                .setMessage("Yakin ingin menghapus progres ini?")
                .setPositiveButton("Ya") { _, _ ->
                    hapusProgres(progres.id_pantau_progess)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@PantauProgres)
            adapter = progresAdapter
        }

        // Tarik untuk refresh
        binding.swipeRefresh.setOnRefreshListener {
            getProgres()
        }

        binding.btnTambah.setOnClickListener {
            if (idPcl == null){
                Toast.makeText(this@PantauProgres,"Id Pcl tidak di temukan", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, TambahProgres::class.java)
            intent.putExtra("id_pcl", idPcl)

            startActivity(intent)
        }

        binding.btnKembali.setOnClickListener { finish() }
    }

    /** 🔄 Ambil daftar progres berdasarkan id_pcl */
    private fun getProgres() {
        val apiService = ApiClient.instance
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            binding.swipeRefresh.isRefreshing = false
            return
        }

        binding.swipeRefresh.isRefreshing = true
        apiService.getProgres("Bearer $token", idPcl).enqueue(object : Callback<PantauProgresListResponse> {
            override fun onResponse(
                call: Call<PantauProgresListResponse>,
                response: Response<PantauProgresListResponse>
            ) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val progresList = response.body()?.data ?: emptyList()
                    progresAdapter.updateData(progresList)
                    if (progresList.isEmpty()) {
                        Toast.makeText(this@PantauProgres, "Belum ada progres", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PantauProgres, "Gagal memuat progres (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PantauProgresListResponse>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@PantauProgres, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** 🗑️ Fungsi hapus progres */
    private fun hapusProgres(id: Int?) {
        if (id == null) {
            Toast.makeText(this, "ID progres tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = ApiClient.instance
        apiService.hapusProgres("Bearer $token", id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PantauProgres, "Progres berhasil dihapus", Toast.LENGTH_SHORT).show()
                    getProgres() // 🔄 Refresh otomatis
                } else {
                    val code = response.code()
                    val errorMsg = when (code) {
                        404 -> "Data tidak ditemukan"
                        403 -> "Akses ditolak"
                        else -> "Gagal menghapus progres ($code)"
                    }
                    Toast.makeText(this@PantauProgres, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@PantauProgres, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getProgres()
    }
}
