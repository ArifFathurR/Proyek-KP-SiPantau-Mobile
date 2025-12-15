package com.example.sipantau

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.sipantau.databinding.DetailLaporanBinding

class DetailLaporanActivity : AppCompatActivity() {

    private lateinit var binding: DetailLaporanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup back button
        binding.btnKembali.setOnClickListener {
            finish()
        }

        // Ambil data dari Intent
        val namaKegiatan = intent.getStringExtra("nama_kegiatan") ?: ""
        val namaKegiatanDetailProses = intent.getStringExtra("nama_kegiatan_detail_proses") ?: ""
        val resume = intent.getStringExtra("resume") ?: ""
        val createdAt = intent.getStringExtra("created_at") ?: ""
        val namaKabupaten = intent.getStringExtra("nama_kabupaten") ?: ""
        val namaKecamatan = intent.getStringExtra("nama_kecamatan") ?: ""
        val namaDesa = intent.getStringExtra("nama_desa") ?: ""
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val imageUrl = intent.getStringExtra("image_url") ?: ""
        val isPending = intent.getBooleanExtra("isPending", false)

        // Set data ke views
        binding.judul.text = namaKegiatanDetailProses.ifEmpty { namaKegiatan }
        binding.resume.text = resume
        binding.tanggal.text = createdAt
        binding.kabKota.text = namaKabupaten

        // Load image
        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(binding.gambar)
        } else {
            binding.gambar.setImageResource(R.drawable.default_image)
        }

        // Optional: Tampilkan lokasi
        // binding.tvLokasi.text = "Lat: $latitude, Long: $longitude"

        // Optional: Tampilkan kecamatan dan desa
        // binding.tvKecamatan.text = namaKecamatan
        // binding.tvDesa.text = namaDesa
    }
}