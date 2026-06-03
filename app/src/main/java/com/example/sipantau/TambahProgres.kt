package com.example.sipantau

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.TambahProgresBinding
import com.example.sipantau.localData.entity.PantauProgresEntity
import com.example.sipantau.localData.entity.SubslsLocalEntity
import com.example.sipantau.localData.repository.PantauProgresRepository
import com.example.sipantau.localData.repository.SubslsRepository
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
    private lateinit var subslsRepo: SubslsRepository
    private var idPcl: Int = 0
    private var selectedSubslsId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TambahProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKembali.setOnClickListener { finish() }

        idPcl = intent.getIntExtra("id_pcl", 0)
        repository = PantauProgresRepository(this)
        subslsRepo = SubslsRepository(this)
        loadSubsls()

        binding.btnSimpan.setOnClickListener {
            simpanProgres()
        }
    }

    private fun loadSubsls() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val list = subslsRepo.getSubsls(idPcl = if (idPcl > 0) idPcl else null)
                showSubslsDropdown(list)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahProgres, "Error memuat Sub-SLS: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun showSubslsDropdown(list: List<SubslsLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_subsls }
            val adapter = ArrayAdapter(this@TambahProgres, android.R.layout.simple_dropdown_item_1line, names)
            binding.spinnerSubsls.setAdapter(adapter)

            binding.spinnerSubsls.setOnItemClickListener { _, _, pos, _ ->
                selectedSubslsId = list[pos].id_subsls
            }
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

        if (selectedSubslsId.isNullOrEmpty()) {
            Toast.makeText(this, "Sub-SLS wajib dipilih", Toast.LENGTH_SHORT).show()
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
            id_subsls = selectedSubslsId,
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
                val rbSubsls = selectedSubslsId.orEmpty().toRequestBody("text/plain".toMediaTypeOrNull())

                val apiResponse = withContext(Dispatchers.IO) {
                    ApiClient.instance.createProgres(
                        token,
                        rbIdPcl,
                        rbJml,
                        rbCat,
                        rbSubsls
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
