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
        setContentView(R.layout.splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

            if (!token.isNullOrEmpty()) {
                // ✅ Masih login (walaupun offline)
                startActivity(Intent(this, Dasboard::class.java))
            } else {
                // ❌ Belum login
                startActivity(Intent(this, WallcomeActivity::class.java))
            }

            finish()
        }, 2000)
    }
}
