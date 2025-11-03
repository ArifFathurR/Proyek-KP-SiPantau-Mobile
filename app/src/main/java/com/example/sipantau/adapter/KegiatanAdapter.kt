package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.databinding.ItemListKegiatanBinding
import com.example.sipantau.model.Kegiatan

class KegiatanAdapter(
    private var listKegiatan: List<Kegiatan>,
    private val onItemClick: (Kegiatan) -> Unit
) : RecyclerView.Adapter<KegiatanAdapter.KegiatanViewHolder>() {

    inner class KegiatanViewHolder(val binding: ItemListKegiatanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KegiatanViewHolder {
        val binding = ItemListKegiatanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KegiatanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KegiatanViewHolder, position: Int) {
        val item = listKegiatan[position]
        with(holder.binding) {
            judul.text = item.nama_kegiatan_detail_proses
            tanggal.text = "${item.tanggal_mulai} s.d ${item.tanggal_selesai}"
            textView.text = item.keterangan_wilayah

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = listKegiatan.size

    fun updateData(newData: List<Kegiatan>) {
        listKegiatan = newData
        notifyDataSetChanged()
    }
}
