package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.R
import com.example.sipantau.databinding.ItemListAchievementBinding
import com.example.sipantau.model.UserAchievement

class AchievementAdapter(
    private val list: List<UserAchievement>
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListAchievementBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ach = list[position]

        holder.binding.namaAchievement.text = ach.nama_achievement
        holder.binding.kategori.text = ach.kategori

        // Warna progress
        val progressValue = if (ach.achieved) 100 else 0
        holder.binding.progressBar.progress = progressValue
        holder.binding.tvPersentase.text = "$progressValue%"

        // Jika sudah tercapai → beri warna biru
        if (ach.achieved) {
            holder.binding.icAchievement.setImageResource(R.drawable.badge)
        } else {
            holder.binding.icAchievement.setImageResource(R.drawable.padlock)
        }
    }
}
