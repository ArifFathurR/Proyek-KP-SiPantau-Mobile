package com.example.sipantau

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.auth.Role

class Splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        Handler(Looper.getMainLooper()).postDelayed({

            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
            val activeRole = prefs.getString(LoginActivity.PREF_ACTIVE_ROLE, null)

            if (!token.isNullOrEmpty()) {
                // Jika user sudah login sebelumnya → cek role terakhir
                when (activeRole) {
                    "PML" -> {
                        startActivity(Intent(this, DashboardPML::class.java))
                    }
                    "PCL" -> {
                        startActivity(Intent(this, Dasboard::class.java))
                    }
                    else -> {
                        // Jika user punya 2 role dan belum memilih
                        startActivity(Intent(this, Role::class.java))
                    }
                }

            } else {
                // Belum login → ke Welcome
                startActivity(Intent(this, WallcomeActivity::class.java))
            }

            finish()

        }, 2000)
    }
}
