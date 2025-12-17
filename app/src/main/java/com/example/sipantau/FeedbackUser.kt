package com.example.sipantau

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val feedbackList = mutableListOf<Feedback>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate dipanggil")

        binding.btnKembali.setOnClickListener {
            finish()
        }

        // =============================
        // AMBIL TOKEN
        // =============================
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        Log.d(TAG, "Token: $token")

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // =============================
        // SETUP RECYCLER VIEW
        // =============================
        feedbackAdapter = FeedbackAdapter(feedbackList)
        binding.rvTestimoni.layoutManager = LinearLayoutManager(this)
        binding.rvTestimoni.adapter = feedbackAdapter

        Log.d(TAG, "RecyclerView & Adapter diinisialisasi")

        // =============================
        // STAR RATING
        // =============================
        starViews = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )

        for ((index, star) in starViews.withIndex()) {
            star.setOnClickListener {
                setRating(index + 1)
                Log.d(TAG, "Rating dipilih: ${index + 1}")
            }
        }

        // =============================
        // SIMPAN FEEDBACK
        // =============================
        binding.btnSimpan.setOnClickListener {
            val feedbackText = binding.edtResume.text.toString().trim()

            Log.d(TAG, "Feedback dikirim: \"$feedbackText\", rating=$ratingValue")

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

        // =============================
        // LOAD FEEDBACK
        // =============================
        ambilDataFeedback(token)
    }

    // =====================================================
    // RATING
    // =====================================================
    private fun setRating(rating: Int) {
        ratingValue = rating
        for (i in starViews.indices) {
            val res = if (i < rating) R.drawable.star else R.drawable.star_null
            starViews[i].setImageResource(res)
        }
    }

    // =====================================================
    // AMBIL DATA FEEDBACK
    // =====================================================
    private fun ambilDataFeedback(token: String) {

        Log.d(TAG, "Memanggil API getFeedback")

        ApiClient.instance.getFeedback("Bearer $token")
            .enqueue(object : Callback<FeedbackResponse> {

                override fun onResponse(
                    call: Call<FeedbackResponse>,
                    response: Response<FeedbackResponse>
                ) {
                    Log.d(TAG, "Response code: ${response.code()}")
                    Log.d(TAG, "Response body: ${response.body()}")

                    if (response.isSuccessful && response.body() != null) {

                        val data = response.body()!!.data
                        Log.d(TAG, "Jumlah data dari API: ${data.size}")

                        data.forEachIndexed { index, item ->
                            Log.d(
                                TAG,
                                "Item[$index] feedback='${item.feedback}', rating=${item.rating}"
                            )
                        }

                        feedbackList.clear()
                        feedbackList.addAll(data.filter { it.feedback.isNotBlank() })

                        Log.d(TAG, "Jumlah data setelah filter: ${feedbackList.size}")

                        feedbackAdapter.notifyDataSetChanged()

                    } else {
                        Log.e(TAG, "Response gagal / body null")
                        Toast.makeText(
                            this@FeedbackUser,
                            "Gagal memuat data (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                    Log.e(TAG, "API getFeedback gagal", t)
                    Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =====================================================
    // KIRIM FEEDBACK
    // =====================================================
    private fun kirimFeedback(token: String, feedback: String, rating: Int) {

        Log.d(TAG, "Mengirim feedback ke server")

        ApiClient.instance.CreateFeedback(
            "Bearer $token",
            0,
            feedback,
            rating
        ).enqueue(object : Callback<FeedbackCreateResponse> {

            override fun onResponse(
                call: Call<FeedbackCreateResponse>,
                response: Response<FeedbackCreateResponse>
            ) {
                Log.d(TAG, "CreateFeedback code: ${response.code()}")
                Log.d(TAG, "CreateFeedback body: ${response.body()}")

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@FeedbackUser,
                        "✅ Feedback berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.edtResume.text?.clear()
                    setRating(0)

                    // refresh list
                    ambilDataFeedback(token)

                } else {
                    Toast.makeText(
                        this@FeedbackUser,
                        "Gagal mengirim feedback (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<FeedbackCreateResponse>, t: Throwable) {
                Log.e(TAG, "CreateFeedback gagal", t)
                Toast.makeText(this@FeedbackUser, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
