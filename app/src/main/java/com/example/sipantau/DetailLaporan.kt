package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.sipantau.databinding.DetailKegiatanBinding

class DetailLaporan : AppCompatActivity() {

    private lateinit var binding: DetailKegiatanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailKegiatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ambil data dari intent
        val judul = intent.getStringExtra("judul") ?: "-"
        val tanggal = intent.getStringExtra("tanggal") ?: "-"
        val resume = intent.getStringExtra("resume") ?: "-"
        val gambarPath = intent.getStringExtra("gambar")

        // tampilkan ke layout
        binding.resume.text = resume
        binding.tanggal.text = tanggal
        binding.kabKota.text = "Belum ada field kabupaten" // nanti bisa diganti kalau API sediakan
        // ubah judul text di atas card
        // karena di XML judul tidak punya id → lebih aman tambahkan id di XML
        // misal android:id="@+id/judulKegiatan"
        binding.judul.text = judul

        val imageUrl = "http://10.0.2.2:8080/${gambarPath?.trimStart('/')}"
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.default_image)
            .error(R.drawable.default_image)
            .into(binding.gambar)

        // tombol kembali
        binding.btnKembali.setOnClickListener {
            finish()
        }

        binding.btnKembali.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
