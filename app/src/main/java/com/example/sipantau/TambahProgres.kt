package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.TambahProgresBinding
import com.example.sipantau.model.PantauProgresCreateResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TambahProgres : AppCompatActivity() {

    private lateinit var binding: TambahProgresBinding
    private var idPcl: Int = 0
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TambahProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Ambil data id_pcl dari intent
        idPcl = intent.getIntExtra("id_pcl", 0)

        // 🔹 Ambil token dari user login
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        // Jika token kosong → minta login ulang
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.btnSimpan.setOnClickListener {
            simpanProgres()
        }
    }

    private fun simpanProgres() {
        val jmlRealisasi = binding.edtJmlRealisasi.text.toString().trim()
        val catatan = binding.edtCatatan.text.toString().trim()

        if (jmlRealisasi.isEmpty()) {
            Toast.makeText(this, "Jumlah realisasi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (catatan.isEmpty()) {
            Toast.makeText(this, "Catatan aktivitas wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = ApiClient.instance

        // 🔹 Bungkus data ke dalam RequestBody
        val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idPcl.toString())
        val jmlRealisasiBody = RequestBody.create("text/plain".toMediaTypeOrNull(), jmlRealisasi)
        val catatanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), catatan)

        // 🔹 Kirim data ke server
        apiService.createProgres("Bearer $token", idPclBody, jmlRealisasiBody, catatanBody)
            .enqueue(object : Callback<PantauProgresCreateResponse> {
                override fun onResponse(
                    call: Call<PantauProgresCreateResponse>,
                    response: Response<PantauProgresCreateResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TambahProgres, "✅ Progres berhasil dikirim", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val msg = when (response.code()) {
                            401 -> "Token tidak valid, silakan login ulang"
                            else -> "❌ Gagal mengirim progres (${response.code()})"
                        }
                        Toast.makeText(this@TambahProgres, msg, Toast.LENGTH_SHORT).show()

                        // Jika token tidak valid, arahkan ke login
                        if (response.code() == 401) {
                            startActivity(Intent(this@TambahProgres, LoginActivity::class.java))
                            finish()
                        }
                    }
                }

                override fun onFailure(call: Call<PantauProgresCreateResponse>, t: Throwable) {
                    Toast.makeText(this@TambahProgres, "⚠️ Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
