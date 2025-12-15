package com.example.sipantau

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.databinding.DetailKegiatanBinding

class DetailKegiatan : AppCompatActivity() {

    private lateinit var binding: DetailKegiatanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailKegiatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data dari intent
        val namaKegiatan = intent.getStringExtra("nama_kegiatan")
        val namaKegiatanDetailProses = intent.getStringExtra("nama_kegiatan_detail_proses")
        val tanggalMulai = intent.getStringExtra("tanggal_mulai")
        val tanggalSelesai = intent.getStringExtra("tanggal_selesai")
        val keteranganWilayah = intent.getStringExtra("keterangan_wilayah")
        val namaKabupaten = intent.getStringExtra("nama_kabupaten")

        // Tampilkan data
        displayData(
            namaKegiatanDetailProses,
            keteranganWilayah,
            tanggalMulai,
            tanggalSelesai
        )

        // Button back
        binding.btnKembali.setOnClickListener {
            finish()
        }
    }

    private fun displayData(
        namaKegiatanDetailProses: String?,
        keteranganWilayah: String?,
        tanggalMulai: String?,
        tanggalSelesai: String?
    ) {
        with(binding) {
            // Judul - Nama Kegiatan Detail Proses
            judul.text = namaKegiatanDetailProses ?: "Detail Kegiatan"

            // Resume - Keterangan Wilayah
            resume.text = keteranganWilayah ?: "-"

            // Tanggal - Periode kegiatan
            tanggal.text = if (!tanggalMulai.isNullOrEmpty() && !tanggalSelesai.isNullOrEmpty()) {
                "$tanggalMulai s.d $tanggalSelesai"
            } else {
                "-"
            }
        }
    }
}