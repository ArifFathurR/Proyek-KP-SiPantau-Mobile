package com.example.sipantau

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var sobatId: Long = 0

    private var allAchievements: List<UserAchievement> = emptyList()
    private var displayedAchievements: MutableList<UserAchievement> = mutableListOf()
    private lateinit var adapter: AchievementAdapter

    private var isLoading = false
    private val pageSize = 10
    private var currentIndex = 0
    private var currentTabAchieved = true // true = Tercapai, false = Belum Tercapai

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        val user = Gson().fromJson(userJson, UserData::class.java)
        sobatId = user.sobat_id.toLong()

        binding.btnKembali.setOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        setupSwipeRefresh()

        loadAchievement()
    }

    private fun setupRecyclerView() {
        adapter = AchievementAdapter(displayedAchievements)
        val layoutManager = LinearLayoutManager(this)
        binding.recylerView.layoutManager = layoutManager
        binding.recylerView.adapter = adapter

        // Lazy loading
        binding.recylerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && totalItemCount <= lastVisible + 2) {
                    loadMoreItems()
                }
            }
        })
    }

    private fun setupTabs() {
        binding.tabTercapai.setOnClickListener {
            currentTabAchieved = true
            resetLazyLoading()
            highlightTab(true)
        }
        binding.tabBelumTercapai.setOnClickListener {
            currentTabAchieved = false
            resetLazyLoading()
            highlightTab(false)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadAchievement()
        }
    }

    private fun loadAchievement() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.instance.getAchievement("Bearer $token", sobatId)
            .enqueue(object : Callback<AchievementResponse> {
                override fun onResponse(
                    call: Call<AchievementResponse>,
                    response: Response<AchievementResponse>
                ) {
                    binding.swipeRefresh.isRefreshing = false
                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null) {
                            allAchievements = data.achievement
                            resetLazyLoading()
                        }
                    } else {
                        Toast.makeText(this@Achievement, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AchievementResponse>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@Achievement, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun resetLazyLoading() {
        currentIndex = 0
        displayedAchievements.clear()
        loadMoreItems()
    }

    private fun loadMoreItems() {
        if (isLoading) return
        isLoading = true

        val filtered = allAchievements.filter { it.achieved == currentTabAchieved }
        val nextIndex = (currentIndex + pageSize).coerceAtMost(filtered.size)
        if (currentIndex >= nextIndex) {
            isLoading = false
            return
        }

        displayedAchievements.addAll(filtered.subList(currentIndex, nextIndex))
        currentIndex = nextIndex
        adapter.notifyDataSetChanged()
        isLoading = false
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
