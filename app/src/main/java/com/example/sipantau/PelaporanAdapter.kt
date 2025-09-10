package com.example.sipantau

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sipantau.databinding.ItemListLaporanBinding
import com.example.sipantau.model.PelaporanResponse

class PelaporanAdapter(
    private val list: MutableList<PelaporanResponse>,
    private val onDeleteClick: (PelaporanResponse, Int) -> Unit
) : RecyclerView.Adapter<PelaporanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemListLaporanBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.judul.text = item.nama_kegiatan_detail ?: "-"
        holder.binding.tanggal.text = item.tanggal_transaksi
        holder.binding.textView.text = item.resume ?: "-"

        val imageUrl = "http://10.0.2.2:8080/${item.imagepath}"
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(com.example.sipantau.R.drawable.default_image)
            .error(com.example.sipantau.R.drawable.default_image)
            .into(holder.binding.gambar)

        holder.binding.btnDelete.visibility = View.VISIBLE
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item, position)
        }
    }

    // 🔥 Tambahan untuk update list tanpa recreate adapter
    fun updateData(newList: List<PelaporanResponse>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    // 🔥 Hapus item di posisi tertentu
    fun removeItem(position: Int) {
        if (position >= 0 && position < list.size) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
