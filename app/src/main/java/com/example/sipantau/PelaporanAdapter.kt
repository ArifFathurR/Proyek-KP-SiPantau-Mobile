package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sipantau.R
import com.example.sipantau.databinding.ItemListLaporanBinding
import com.example.sipantau.model.PelaporanResponse

class PelaporanAdapter(private val list: List<PelaporanResponse>) :
    RecyclerView.Adapter<PelaporanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemListLaporanBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.judul.text = item.nama_kegiatan_detail ?: "-"
        holder.binding.tanggal.text = item.tanggal_transaksi
        holder.binding.textView.text = item.resume ?: "-"

        // Load gambar
        val imageUrl = "http://10.14.12.35:8080/${item.imagepath}"
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.default_image)
            .error(R.drawable.default_image)
            .into(holder.binding.gambar)

        holder.binding.btnDelete.visibility = View.VISIBLE
    }
}
