package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.auth.LoginActivity

class Splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash) // pakai XML splash yang kamu buat

        // Tunda 2 detik sebelum pindah halaman
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("MyAppPref", MODE_PRIVATE)
            val token = prefs.getString("TOKEN", null)

            if (token != null) {
                // Jika sudah login → MainActivity
                startActivity(Intent(this, Dasboard::class.java))
            } else {
                // Jika belum login → LoginActivity
                startActivity(Intent(this, WallcomeActivity::class.java))
            }
            finish()
        }, 2000) // 2000ms = 2 detik
    }
}
