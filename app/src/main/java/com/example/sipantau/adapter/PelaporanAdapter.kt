package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sipantau.R
import com.example.sipantau.databinding.ItemListLaporanBinding
import com.example.sipantau.model.LaporanData

class PelaporanAdapter(
    private var pelaporanList: List<LaporanData>,
    private val onDeleteClick: (LaporanData) -> Unit
) : RecyclerView.Adapter<PelaporanAdapter.PelaporanViewHolder>() {

    inner class PelaporanViewHolder(val binding: ItemListLaporanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PelaporanViewHolder {
        val binding = ItemListLaporanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PelaporanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PelaporanViewHolder, position: Int) {
        val item = pelaporanList[position]
        val context = holder.itemView.context

        with(holder.binding) {
            judul.text = item.nama_kegiatan
            tanggal.text = item.created_at
            textView.text = item.resume
            kegiatan.text = item.nama_kegiatan_detail_proses

            // ✅ Tambahkan prefix "http://10.0.2.2:8081/" jika path belum mengandung http
            val imageUrl = if (!item.imagepath.isNullOrEmpty()) {
                if (item.imagepath.startsWith("http")) {
                    item.imagepath
                } else {
                    "http://192.168.137.36:8080/${item.imagepath}"
                }
            } else {
                null
            }

            // Load gambar pakai Glide
            if (imageUrl != null) {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_image)
                    .error(R.drawable.default_image)
                    .into(gambar)
            } else {
                gambar.setImageResource(R.drawable.default_image)
            }

            // Tombol delete
            btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun getItemCount(): Int = pelaporanList.size

    fun updateData(newList: List<LaporanData>) {
        pelaporanList = newList
        notifyDataSetChanged()
    }
}
