package com.example.sipantau

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.TambahProgresBinding
import com.example.sipantau.localData.entity.PantauProgresEntity
import com.example.sipantau.localData.repository.PantauProgresRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TambahProgres : AppCompatActivity() {

    private lateinit var binding: TambahProgresBinding
    private lateinit var repository: PantauProgresRepository
    private var idPcl: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TambahProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKembali.setOnClickListener { finish() }

        idPcl = intent.getIntExtra("id_pcl", 0)
        repository = PantauProgresRepository(this)

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

        // ================= LOADING ON =================
        setLoading(true)

        val now = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val entity = PantauProgresEntity(
            local_id = 0L,
            server_id = null,
            id_pcl = idPcl,
            jumlah_realisasi_absolut = jmlRealisasi.toInt(),
            jumlah_realisasi_kumulatif = null,
            catatan_aktivitas = catatan,
            created_at = now,
            is_synced = false
        )

        lifecycleScope.launch {

            val (success, _) = repository.insertProgres(entity)

            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TambahProgres,
                        "✅ Progres berhasil dikirim",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                return@launch
            }

            try {
                val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
                val tokenRaw = prefs.getString(LoginActivity.PREF_TOKEN, null)
                val token = "Bearer $tokenRaw"

                val rbIdPcl = idPcl.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val rbJml = jmlRealisasi.toRequestBody("text/plain".toMediaTypeOrNull())
                val rbCat = catatan.toRequestBody("text/plain".toMediaTypeOrNull())

                val apiResponse = withContext(Dispatchers.IO) {
                    ApiClient.instance.createProgres(
                        token,
                        rbIdPcl,
                        rbJml,
                        rbCat
                    ).execute()
                }

                withContext(Dispatchers.Main) {
                    setLoading(false)

                    if (!apiResponse.isSuccessful) {
                        val errorBody = apiResponse.errorBody()?.string()
                        Toast.makeText(
                            this@TambahProgres,
                            extractApiError(errorBody),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@TambahProgres,
                            "Progres berhasil dikirim",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@TambahProgres,
                        "Tersimpan secara lokal (akan dikirim saat online)",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    // ================= LOADING STATE =================
    private fun setLoading(isLoading: Boolean) {
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "Menyimpan..." else "Simpan Laporan"
        binding.progressBtn.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun extractApiError(raw: String?): String {
        return try {
            val json = JSONObject(raw ?: "{}")
            json.optJSONObject("messages")?.optString("error")
                ?: json.optString("message", "Terjadi kesalahan")
        } catch (e: Exception) {
            "Terjadi kesalahan"
        }
    }
}
