package com.example.sipantau.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.Dasboard
import com.example.sipantau.DashboardPML
import com.example.sipantau.databinding.ActivityPilihRoleBinding

class Role : AppCompatActivity() {
    private lateinit var binding: ActivityPilihRoleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPilihRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPml.setOnClickListener {
            // set active role = PML
            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            prefs.edit().putString(LoginActivity.PREF_ACTIVE_ROLE, "PML").apply()
            startActivity(Intent(this, DashboardPML::class.java))
            finish()
        }

        binding.btnPcl.setOnClickListener {
            // set active role = PCL
            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            prefs.edit().putString(LoginActivity.PREF_ACTIVE_ROLE, "PCL").apply()
            startActivity(Intent(this, Dasboard::class.java))
            finish()
        }
    }
}
