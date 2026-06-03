package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.ProgresIndustriDigitalAdapter
import com.example.sipantau.databinding.ActivityLaporIndustriDigitalBinding
import com.example.sipantau.localData.repository.ProgresIndustriDigitalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgresIndustriDigital : AppCompatActivity() {

    private lateinit var binding: ActivityLaporIndustriDigitalBinding
    private lateinit var adapter: ProgresIndustriDigitalAdapter
    private lateinit var repository: ProgresIndustriDigitalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporIndustriDigitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ProgresIndustriDigitalRepository(this)

        // Header statis (sesuai XML)
        binding.kegiatanDetail.text = "Survei Ekonomi 2026"
        binding.kegiatanDetailProses.text = "Laporan Progres Industri Digital"

        // Setup adapter
        adapter = ProgresIndustriDigitalAdapter(emptyList()) { item ->

            AlertDialog.Builder(this)
                .setTitle("Hapus Data")
                .setMessage("Yakin ingin menghapus data ini?")
                .setPositiveButton("Ya") { _, _ ->
                    hapusProgres(item.server_id, item.local_id)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@ProgresIndustriDigital)
            adapter = this@ProgresIndustriDigital.adapter
        }

        // Swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }

        // Tombol tambah
        binding.btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahProgresIndustriDigital::class.java))
        }

        // Tombol kembali
        binding.btnKembali.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()

        // Sync data offline
        lifecycleScope.launch {
            repository.syncPending()
        }
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val data = repository.getProgres()

                // Update RecyclerView
                adapter.updateData(data)

                // ================================
                // HITUNG TOTAL KUMULATIF
                // ================================
                val totalKumulatif = data
                    .maxByOrNull { it.jumlah_realisasi_kumulatif ?: 0 }
                    ?.jumlah_realisasi_kumulatif ?: 0

                binding.RealisasiKumulatif.text =
                    "Realisasi Kumulatif: $totalKumulatif"

                if (data.isEmpty()) {
                    Toast.makeText(
                        this@ProgresIndustriDigital,
                        "Belum ada data progres",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ProgresIndustriDigital,
                    "Gagal memuat data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    /**
     * OPTIONAL: DELETE DATA
     * Bisa kamu aktifkan kalau mau fitur hapus
     */
    private fun hapusProgres(serverId: Int?, localId: Long?) {

        lifecycleScope.launch(Dispatchers.IO) {

            val success = repository.deleteProgres(serverId, localId)

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@ProgresIndustriDigital,
                        "Data berhasil dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
                } else {
                    Toast.makeText(
                        this@ProgresIndustriDigital,
                        "Gagal menghapus data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}