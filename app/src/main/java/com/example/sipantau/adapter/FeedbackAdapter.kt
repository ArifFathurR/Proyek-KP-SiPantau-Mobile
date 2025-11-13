package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.R
import com.example.sipantau.databinding.ItemFeedbackBinding
import com.example.sipantau.model.Feedback

class FeedbackAdapter(private val feedbackList: List<Feedback>) :
    RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    inner class FeedbackViewHolder(val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val item = feedbackList[position]
        val b = holder.binding

        b.tvFeedback.text = item.feedback ?: "-"

        // Ubah tampilan rating
        val stars = listOf(
            b.star1Small, b.star2Small, b.star3Small, b.star4Small, b.star5Small
        )

        for (i in stars.indices) {
            val starRes = if (i < (item.rating ?: 0)) R.drawable.star else R.drawable.star_null
            stars[i].setImageResource(starRes)
        }
    }

    override fun getItemCount(): Int = feedbackList.size
}
