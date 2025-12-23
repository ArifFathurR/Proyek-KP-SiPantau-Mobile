package com.example.sipantau

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.adapter.FeedbackAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityFeedbackTestimoniBinding
import com.example.sipantau.model.Feedback
import com.example.sipantau.model.FeedbackCreateResponse
import com.example.sipantau.model.FeedbackResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedbackUser : AppCompatActivity() {

    companion object {
        private const val TAG = "FeedbackUser"
    }

    private lateinit var binding: ActivityFeedbackTestimoniBinding
    private var ratingValue = 0
    private lateinit var starViews: List<ImageView>

    private var allFeedbacks: List<Feedback> = emptyList()
    private val displayedFeedbacks = mutableListOf<Feedback>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    private var isLoading = false
    private var isSubmitting = false
    private val pageSize = 10
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnKembali.setOnClickListener { finish() }

        setupRecyclerView()
        setupSwipeRefresh(token)
        setupStars()
        setupBtnSimpan(token)

        // Load awal
        ambilDataFeedback(token)
    }

    private fun setupRecyclerView() {
        feedbackAdapter = FeedbackAdapter(displayedFeedbacks)
        val layoutManager = LinearLayoutManager(this)
        binding.rvTestimoni.layoutManager = layoutManager
        binding.rvTestimoni.adapter = feedbackAdapter

        // Lazy loading
        binding.rvTestimoni.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    private fun setupSwipeRefresh(token: String) {
        binding.swipeRefresh.setOnRefreshListener {
            ambilDataFeedback(token)
        }
    }

    private fun setupStars() {
        starViews = listOf(
            binding.star1, binding.star2, binding.star3, binding.star4, binding.star5
        )
        for ((index, star) in starViews.withIndex()) {
            star.setOnClickListener {
                setRating(index + 1)
            }
        }
    }

    private fun setupBtnSimpan(token: String) {
        binding.btnSimpan.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            val feedbackText = binding.edtResume.text.toString().trim()
            if (feedbackText.isEmpty()) {
                Toast.makeText(this, "Feedback tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ratingValue == 0) {
                Toast.makeText(this, "Silakan beri rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            kirimFeedback(token, feedbackText, ratingValue)
        }
    }

    private fun setRating(rating: Int) {
        ratingValue = rating
        for (i in starViews.indices) {
            val res = if (i < rating) R.drawable.star else R.drawable.star_null
            starViews[i].setImageResource(res)
        }
    }

    private fun ambilDataFeedback(token: String) {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.instance.getFeedback("Bearer $token")
            .enqueue(object : Callback<FeedbackResponse> {
                override fun onResponse(
                    call: Call<FeedbackResponse>,
                    response: Response<FeedbackResponse>
                ) {
                    binding.swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body() != null) {
                        allFeedbacks = response.body()!!.data.filter { it.feedback.isNotBlank() }
                        resetLazyLoading()
                    } else {
                        Toast.makeText(this@FeedbackUser, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resetLazyLoading() {
        currentIndex = 0
        displayedFeedbacks.clear()
        loadMoreItems()
    }

    private fun loadMoreItems() {
        if (isLoading) return
        isLoading = true

        val nextIndex = (currentIndex + pageSize).coerceAtMost(allFeedbacks.size)
        if (currentIndex >= nextIndex) {
            isLoading = false
            return
        }

        displayedFeedbacks.addAll(allFeedbacks.subList(currentIndex, nextIndex))
        currentIndex = nextIndex
        feedbackAdapter.notifyDataSetChanged()
        isLoading = false
    }

    private fun kirimFeedback(token: String, feedback: String, rating: Int) {
        // Set loading state
        isSubmitting = true
        setLoadingState(true)

        ApiClient.instance.CreateFeedback("Bearer $token", 0, feedback, rating)
            .enqueue(object : Callback<FeedbackCreateResponse> {
                override fun onResponse(
                    call: Call<FeedbackCreateResponse>,
                    response: Response<FeedbackCreateResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FeedbackUser, "✅ Feedback berhasil ditambahkan", Toast.LENGTH_SHORT).show()

                        // Clear form
                        binding.edtResume.text?.clear()
                        setRating(0)

                        // Refresh data dengan lazy loading
                        ambilDataFeedbackWithLazyLoad(token)
                    } else {
                        Toast.makeText(this@FeedbackUser, "Gagal mengirim feedback (${response.code()})", Toast.LENGTH_SHORT).show()
                        setLoadingState(false)
                        isSubmitting = false
                    }
                }

                override fun onFailure(call: Call<FeedbackCreateResponse>, t: Throwable) {
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                    isSubmitting = false
                }
            })
    }

    private fun ambilDataFeedbackWithLazyLoad(token: String) {
        // Tampilkan loading indicator
        binding.swipeRefresh.isRefreshing = true

        ApiClient.instance.getFeedback("Bearer $token")
            .enqueue(object : Callback<FeedbackResponse> {
                override fun onResponse(
                    call: Call<FeedbackResponse>,
                    response: Response<FeedbackResponse>
                ) {
                    binding.swipeRefresh.isRefreshing = false
                    setLoadingState(false)
                    isSubmitting = false

                    if (response.isSuccessful && response.body() != null) {
                        allFeedbacks = response.body()!!.data.filter { it.feedback.isNotBlank() }
                        resetLazyLoading()

                        // Scroll ke atas untuk melihat feedback baru
                        binding.rvTestimoni.smoothScrollToPosition(0)
                    } else {
                        Toast.makeText(this@FeedbackUser, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                    setLoadingState(false)
                    isSubmitting = false
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "Mengirim..." else "Simpan"
        binding.edtResume.isEnabled = !isLoading

        // Disable star rating saat loading
        starViews.forEach { it.isEnabled = !isLoading }
    }
}