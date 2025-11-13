package com.example.sipantau

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.R
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

    private lateinit var binding: ActivityFeedbackTestimoniBinding
    private var ratingValue = 0
    private lateinit var starViews: List<ImageView>
    private val feedbackList = mutableListOf<Feedback>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Ambil token dari SharedPreferences
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        // ✅ Inisialisasi RecyclerView
        feedbackAdapter = FeedbackAdapter(feedbackList)
        binding.rvTestimoni.layoutManager = LinearLayoutManager(this)
        binding.rvTestimoni.adapter = feedbackAdapter

        // ✅ Daftar ImageView bintang
        starViews = listOf(
            binding.star1, binding.star2, binding.star3,
            binding.star4, binding.star5
        )

        // ✅ Event klik untuk bintang rating
        for ((index, star) in starViews.withIndex()) {
            star.setOnClickListener { setRating(index + 1) }
        }

        // ✅ Tombol simpan feedback
        binding.btnSimpan.setOnClickListener {
            val feedbackText = binding.edtResume.text.toString().trim()

            if (feedbackText.isEmpty()) {
                Toast.makeText(this, "Feedback tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ratingValue == 0) {
                Toast.makeText(this, "Silakan beri rating terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (token != null) {
                kirimFeedback(token, feedbackText, ratingValue)
            } else {
                Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Ambil semua feedback
        if (token != null) {
            ambilDataFeedback(token)
        } else {
            Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setRating(rating: Int) {
        ratingValue = rating
        for (i in starViews.indices) {
            val starRes = if (i < rating) R.drawable.star else R.drawable.star_null
            starViews[i].setImageResource(starRes)
        }
    }

    private fun ambilDataFeedback(token: String) {
        ApiClient.instance.getFeedback("Bearer $token")
            .enqueue(object : Callback<FeedbackResponse> {
                override fun onResponse(call: Call<FeedbackResponse>, response: Response<FeedbackResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!.data
                        feedbackList.clear()
                        feedbackList.addAll(data.filter { it.feedback.isNotBlank() })
                        feedbackAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@FeedbackUser, "Gagal memuat data (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun kirimFeedback(token: String, feedback: String, rating: Int) {
        ApiClient.instance.CreateFeedback("Bearer $token", 0, feedback, rating)
            .enqueue(object : Callback<FeedbackCreateResponse> {
                override fun onResponse(call: Call<FeedbackCreateResponse>, response: Response<FeedbackCreateResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@FeedbackUser, "✅ Feedback berhasil ditambahkan", Toast.LENGTH_SHORT).show()

                        // Kosongkan input dan reset rating
                        binding.edtResume.text?.clear()
                        setRating(0)

                        // 🔄 Refresh daftar feedback
                        ambilDataFeedback(token)
                    } else {
                        Toast.makeText(this@FeedbackUser, "Gagal mengirim feedback (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackCreateResponse>, t: Throwable) {
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
