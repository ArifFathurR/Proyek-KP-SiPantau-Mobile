package com.example.sipantau

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.databinding.ActivityTambahLaporanBinding

class TambahLaporan :AppCompatActivity() {
    private lateinit var binding: ActivityTambahLaporanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}