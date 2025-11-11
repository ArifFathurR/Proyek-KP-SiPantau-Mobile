package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.TambahProgresBinding
import com.example.sipantau.localData.entity.PantauProgresEntity
import com.example.sipantau.localData.repository.PantauProgresRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        idPcl = intent.getIntExtra("id_pcl", 0)
        repository = PantauProgresRepository(this)

        binding.btnSimpan.setOnClickListener { simpanProgres() }
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

        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

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

        // insert via repository
        lifecycleScope.launch {
            val (success, localId) = repository.insertProgres(entity)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@TambahProgres, "✅ Progres berhasil dikirim", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TambahProgres, "Tersimpan secara lokal (akan dikirim saat online)", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }
}
