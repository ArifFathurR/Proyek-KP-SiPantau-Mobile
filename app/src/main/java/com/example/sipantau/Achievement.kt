package com.example.sipantau

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.AchievementAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityAchievementBinding
import com.example.sipantau.model.AchievementResponse
import com.example.sipantau.model.UserAchievement
import com.example.sipantau.model.UserData
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Achievement : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementBinding
    private lateinit var token: String
    private var sobatId: Int = 0

    private var achievedList: List<UserAchievement> = emptyList()
    private var unachievedList: List<UserAchievement> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        val user = Gson().fromJson(userJson, UserData::class.java)
        sobatId = user.sobat_id.toInt()

        binding.btnKembali.setOnClickListener { finish() }

        loadAchievement()
    }

    private fun loadAchievement() {

        ApiClient.instance.getAchievement("Bearer $token", sobatId)
            .enqueue(object : Callback<AchievementResponse> {

                override fun onResponse(
                    call: Call<AchievementResponse>,
                    response: Response<AchievementResponse>
                ) {
                    if (response.isSuccessful) {

                        val data = response.body()
                        if (data != null) {
                            // Pisahkan achievement
                            achievedList = data.achievement.filter { it.achieved }
                            unachievedList = data.achievement.filter { !it.achieved }

                            showTercapai()
                            setupTabs()
                        }

                    } else {
                        Toast.makeText(this@Achievement, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AchievementResponse>, t: Throwable) {
                    Toast.makeText(this@Achievement, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    // ---------------------------------------
    // TAB SECTION
    // ---------------------------------------

    private fun setupTabs() {
        binding.tabTercapai.setOnClickListener { showTercapai() }
        binding.tabBelumTercapai.setOnClickListener { showBelumTercapai() }
    }

    private fun showTercapai() {
        binding.recylerView.layoutManager = LinearLayoutManager(this)
        binding.recylerView.adapter = AchievementAdapter(achievedList)
        highlightTab(true)
    }

    private fun showBelumTercapai() {
        binding.recylerView.layoutManager = LinearLayoutManager(this)
        binding.recylerView.adapter = AchievementAdapter(unachievedList)
        highlightTab(false)
    }

    private fun highlightTab(isTercapai: Boolean) {
        if (isTercapai) {
            binding.tabTercapai.setCardBackgroundColor(0xFFB3D9FF.toInt())
            binding.tabBelumTercapai.setCardBackgroundColor(0x00FFFFFF)
        } else {
            binding.tabBelumTercapai.setCardBackgroundColor(0xFFB3D9FF.toInt())
            binding.tabTercapai.setCardBackgroundColor(0x00FFFFFF)
        }
    }
}
