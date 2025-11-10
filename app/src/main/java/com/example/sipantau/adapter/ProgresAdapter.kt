package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.databinding.ItemListPantauProgresBinding
import com.example.sipantau.model.PantauProgres

class ProgresAdapter(
    private var listProgres: List<PantauProgres>,
    private val onItemClick: (PantauProgres) -> Unit
) : RecyclerView.Adapter<ProgresAdapter.ProgresViewHolder>() {

    inner class ProgresViewHolder(val binding: ItemListPantauProgresBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgresViewHolder {
        val binding = ItemListPantauProgresBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgresViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgresViewHolder, position: Int) {
        val item = listProgres[position]
        with(holder.binding) {
            tvJumlahRealisasi.text = "Jumlah Realisasi: ${item.jumlah_realisasi_kumulatif ?: 0}"
            jmlRealisasiHarian.text = "Realisasi Harian: ${item.jumlah_realisasi_absolut ?: 0}"
            tvDetailProgress2.text = item.catatan_aktivitas ?: "-"
            tgl.text = item.created_at ?: "-"

            btnHapus.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = listProgres.size

    fun updateData(newData: List<PantauProgres>) {
        listProgres = newData
        notifyDataSetChanged()
    }
}
