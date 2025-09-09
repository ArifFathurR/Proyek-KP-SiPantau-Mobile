package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol logout
        binding.btnKembali.setOnClickListener {
            val prefs = getSharedPreferences("MyAppPref", MODE_PRIVATE)
            prefs.edit().clear().apply() // hapus session

            // Arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
        }
    }
}
