package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.api.ApiClient
import com.example.sipantau.api.ApiService
import com.example.sipantau.databinding.FragmentProfileBinding
import com.example.sipantau.model.UserData
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Profile : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var api: ApiService
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        api = ApiClient.instance
        prefs = requireActivity().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)

        loadProfileData()

        binding.btnSimpan.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }

        return binding.root
    }

    // ==========================
    // LOAD DATA PROFIL DARI LOCAL
    // ==========================
    private fun loadProfileData() {
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)

        if (userJson != null) {
            val user = Gson().fromJson(userJson, UserData::class.java)

            binding.tvNama.setText((user.nama_user))
            binding.tvEmail.setText(user.email)
            binding.edtNama.setText(user.nama_user)
            binding.edtEmail.setText(user.email)
            binding.edtNomorHp.setText(user.hp)

            binding.edtEmail.isEnabled = false
        }
    }

    // ==========================
    // VALIDASI INPUT
    // ==========================
    private fun saveProfile() {
        val nama = binding.edtNama.text.toString().trim()
        val hp = binding.edtNomorHp.text.toString().trim()
        val password = binding.edtPasswordBaru.text.toString().trim()

        if (nama.isEmpty()) {
            binding.edtNama.error = "Nama tidak boleh kosong"
            return
        }

        updateToServer(nama, hp, if (password.isEmpty()) null else password)
    }

    // ==========================
    // UPDATE PROFIL VIA @Field
    // ==========================
    private fun updateToServer(nama: String, hp: String, password: String?) {

        val token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""

        api.editProfile(
            "Bearer $token",
            nama,
            hp,
            password
        ).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {

                    saveLocalChanges(nama, hp)

                    Toast.makeText(
                        requireContext(),
                        "Profil berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gagal update profil",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ==========================
    // UPDATE SHAREDPREFERENCES
    // ==========================
    private fun saveLocalChanges(nama: String, hp: String) {
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)

        if (userJson != null) {
            val user = Gson().fromJson(userJson, UserData::class.java)
            user.nama_user = nama
            user.hp = hp
            prefs.edit().putString(LoginActivity.PREF_USER, Gson().toJson(user)).apply()
        }
    }

    // ==========================
    // LOGOUT (TETAP ADA)
    // ==========================
    private fun performLogout() {
        prefs.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
