package com.example.sipantau

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.databinding.DetailProgresBinding

class DetailProgresActivity : AppCompatActivity() {

    private lateinit var binding: DetailProgresBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailProgresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup back button
        binding.btnKembali.setOnClickListener {
            finish()
        }

        // Ambil data dari Intent
        val idPantauProgess = intent.getIntExtra("id_pantau_progess", 0)
        val namaKegiatan = intent.getStringExtra("nama_kegiatan") ?: ""
        val namaKegiatanDetailProses = intent.getStringExtra("nama_kegiatan_detail_proses") ?: ""
        val jumlahRealisasiAbsolut = intent.getIntExtra("jumlah_realisasi_absolut", 0)
        val jumlahRealisasiKumulatif = intent.getIntExtra("jumlah_realisasi_kumulatif", 0)
        val catatanAktivitas = intent.getStringExtra("catatan_aktivitas") ?: ""
        val createdAt = intent.getStringExtra("created_at") ?: ""
        val updatedAt = intent.getStringExtra("updated_at") ?: ""

        // Data tambahan
        val idPcl = intent.getIntExtra("id_pcl", 0)
        val idKegiatan = intent.getIntExtra("id_kegiatan", 0)
        val idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        // Set data ke views
        binding.jmlRealisasiHarian.text = jumlahRealisasiAbsolut.toString()
        binding.resume.text = catatanAktivitas.ifEmpty { "-" }
        binding.tanggal.text = createdAt

        // Optional: Tampilkan data tambahan jika diperlukan
        // binding.tvRealisasiKumulatif.text = "Realisasi Kumulatif: $jumlahRealisasiKumulatif"
        // binding.tvUpdatedAt.text = "Diperbarui: $updatedAt"
    }
}