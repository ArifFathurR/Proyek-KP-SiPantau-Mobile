package com.example.sipantau.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.R
import com.example.sipantau.databinding.DetailAchievementBinding
import com.example.sipantau.databinding.ItemListAchievementBinding
import com.example.sipantau.model.UserAchievement
import com.google.android.material.bottomsheet.BottomSheetDialog

class AchievementAdapter(
    private val list: List<UserAchievement>
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemListAchievementBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListAchievementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        with(holder.binding) {
            namaAchievement.text = item.nama_achievement
            kategori.text = item.kategori

            if (item.achieved) {
                progressBar.progress = 100
                tvPersentase.text = "100%"
            } else {
                progressBar.progress = 0
                tvPersentase.text = "0%"
            }

            root.setOnClickListener {
                showDetailPopup(root.context, item)
            }
        }

        // Jika sudah tercapai → beri warna biru
        if (item.achieved) {
            holder.binding.icAchievement.setImageResource(R.drawable.badge)
        } else {
            holder.binding.icAchievement.setImageResource(R.drawable.padlock)
        }
    }

    private fun showDetailPopup(context: Context, achievement: UserAchievement) {

        val dialog = BottomSheetDialog(context)

        // Gunakan binding untuk popup
        val binding = DetailAchievementBinding.inflate(LayoutInflater.from(context))

        binding.namaAchievement.text = achievement.nama_achievement
        binding.deskripsi.text = achievement.deskripsi ?: "Tidak ada deskripsi"

        dialog.setContentView(binding.root)
        dialog.show()
    }
}
