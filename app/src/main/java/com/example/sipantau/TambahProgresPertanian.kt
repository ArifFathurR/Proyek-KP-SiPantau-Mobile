package com.example.sipantau

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.databinding.TambahProgresIndustriDigitalBinding
import com.example.sipantau.localData.entity.ProgresPertanianEntity
import com.example.sipantau.localData.entity.SubslsLocalEntity
import com.example.sipantau.localData.repository.ProgresPertanianRepository
import com.example.sipantau.localData.repository.SubslsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TambahProgresPertanian : AppCompatActivity() {

    private lateinit var binding: TambahProgresIndustriDigitalBinding
    private lateinit var repository: ProgresPertanianRepository
    private lateinit var subslsRepo: SubslsRepository
    private var selectedSubslsId: String? = null
    private var idPcl: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TambahProgresIndustriDigitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ProgresPertanianRepository(this)
        subslsRepo = SubslsRepository(this)
        idPcl = intent.getIntExtra("id_pcl", 0)

        loadSubsls()

        binding.btnKembali.setOnClickListener { finish() }

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
                    Toast.makeText(this@TambahProgresPertanian, "Error memuat Sub-SLS: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun showSubslsDropdown(list: List<SubslsLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_subsls }
            val adapter = ArrayAdapter(this@TambahProgresPertanian, android.R.layout.simple_dropdown_item_1line, names)
            binding.spinnerSubsls.setAdapter(adapter)

            binding.spinnerSubsls.setOnItemClickListener { _, _, pos, _ ->
                selectedSubslsId = list[pos].id_subsls
            }
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

        if (selectedSubslsId.isNullOrEmpty()) {
            Toast.makeText(this, "Sub-SLS wajib dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val now = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val entity = ProgresPertanianEntity(
            local_id = 0L,
            server_id = null,
            jumlah_realisasi_absolut = jml.toInt(),
            jumlah_realisasi_kumulatif = null,
            catatan_aktivitas = catatan,
            id_subsls = selectedSubslsId,
            created_at = now,
            updated_at = now,
            is_synced = false
        )

        lifecycleScope.launch {

            val (success, _) = repository.insertProgres(entity)

            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TambahProgresPertanian,
                        "✅ Progres berhasil dikirim",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                return@launch
            }

            // fallback (optional)
            withContext(Dispatchers.Main) {
                setLoading(false)
                Toast.makeText(
                    this@TambahProgresPertanian,
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
