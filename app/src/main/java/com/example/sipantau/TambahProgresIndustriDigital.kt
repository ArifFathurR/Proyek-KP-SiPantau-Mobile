package com.example.sipantau

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.databinding.TambahProgresIndustriDigitalBinding
import com.example.sipantau.localData.entity.ProgresIndustriDigitalEntity
import com.example.sipantau.localData.repository.ProgresIndustriDigitalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TambahProgresIndustriDigital : AppCompatActivity() {

    private lateinit var binding: TambahProgresIndustriDigitalBinding
    private lateinit var repository: ProgresIndustriDigitalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TambahProgresIndustriDigitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ProgresIndustriDigitalRepository(this)
        binding.subsls.visibility = View.GONE

        binding.btnKembali.setOnClickListener { finish() }

        binding.btnSimpan.setOnClickListener {
            simpanProgres()
        }
    }

    private fun simpanProgres() {

        val jml = binding.edtJmlRealisasi.text.toString().trim()
        val catatan = binding.edtCatatan.text.toString().trim()

        // VALIDASI
        if (jml.isEmpty()) {
            Toast.makeText(this, "Jumlah realisasi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (catatan.isEmpty()) {
            Toast.makeText(this, "Catatan aktivitas wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val now = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val entity = ProgresIndustriDigitalEntity(
            local_id = 0L,
            server_id = null,
            jumlah_realisasi_absolut = jml.toInt(),
            jumlah_realisasi_kumulatif = null,
            catatan_aktivitas = catatan,
            created_at = now,
            updated_at = now,
            is_synced = false
        )

        lifecycleScope.launch {

            val (success, _) = repository.insertProgres(entity)

            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TambahProgresIndustriDigital,
                        "✅ Progres berhasil dikirim",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                return@launch
            }

            // fallback (optional, sama pola lama)
            withContext(Dispatchers.Main) {
                setLoading(false)
                Toast.makeText(
                    this@TambahProgresIndustriDigital,
                    "Tersimpan secara lokal (akan dikirim saat online)",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    // LOADING STATE
    private fun setLoading(isLoading: Boolean) {
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "Menyimpan..." else "Simpan Laporan"
        binding.progressBtn.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun extractApiError(raw: String?): String {
        return try {
            val json = JSONObject(raw ?: "{}")
            json.optString("message", "Terjadi kesalahan")
        } catch (e: Exception) {
            "Terjadi kesalahan"
        }
    }
}