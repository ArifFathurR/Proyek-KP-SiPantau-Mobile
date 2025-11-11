package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.ProgresAdapter
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityPantauProgresBinding
import com.example.sipantau.localData.repository.PantauProgresRepository
import com.example.sipantau.model.PantauProgres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PantauProgres : AppCompatActivity() {
    private lateinit var binding: ActivityPantauProgresBinding
    private lateinit var progresAdapter: ProgresAdapter
    private lateinit var repository: PantauProgresRepository
    private var idPcl: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantauProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPcl = intent.getIntExtra("id_pcl", 0)
        repository = PantauProgresRepository(this)

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

        binding.swipeRefresh.setOnRefreshListener { loadProgres() }
        binding.btnTambah.setOnClickListener {
            val intent = Intent(this, TambahProgres::class.java)
            intent.putExtra("id_pcl", idPcl)
            startActivity(intent)
        }
        binding.btnKembali.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadProgres()
        // optional: coba sinkronisasi pending ketika resume
        lifecycleScope.launch { repository.syncPending() }
    }

    private fun loadProgres() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val entities = repository.getProgres(idPcl)
                val models = entities.map {
                    PantauProgres(
                        id_pantau_progess = it.server_id,
                        id_pcl = it.id_pcl,
                        jumlah_realisasi_absolut = it.jumlah_realisasi_absolut,
                        jumlah_realisasi_kumulatif = it.jumlah_realisasi_kumulatif,
                        catatan_aktivitas = it.catatan_aktivitas,
                        created_at = it.created_at ?: "-"
                    )
                }
                progresAdapter.updateData(models)
                if (models.isEmpty()) {
                    Toast.makeText(this@PantauProgres, "Belum ada progres", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantauProgres, "Gagal memuat progres: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun hapusProgres(serverId: Int?) {
        if (serverId == null) {
            Toast.makeText(this, "ID progres tidak ditemukan (server id null)", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = repository.deleteProgresByServerOrLocal(serverId, null)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@PantauProgres, "Progres berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadProgres()
                } else {
                    Toast.makeText(this@PantauProgres, "Gagal menghapus progres", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
