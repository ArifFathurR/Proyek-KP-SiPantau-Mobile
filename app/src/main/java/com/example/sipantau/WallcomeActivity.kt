package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.WellcomeLoginBinding

class WallcomeActivity : AppCompatActivity() {
    private lateinit var binding: WellcomeLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WellcomeLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginMitra.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}