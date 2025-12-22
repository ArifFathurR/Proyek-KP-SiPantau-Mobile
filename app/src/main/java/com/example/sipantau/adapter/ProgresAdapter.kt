package com.example.sipantau.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.DetailProgresActivity
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
            tvJumlahRealisasi.text = "Realisasi Kumulatif: ${item.jumlah_realisasi_kumulatif ?: 0}"
            jmlRealisasiHarian.text = "Realisasi Harian: ${item.jumlah_realisasi_absolut ?: 0}"
            tvDetailProgress2.text = item.catatan_aktivitas ?: "-"
            tgl.text = item.created_at ?: "-"

            // Button Hapus
            btnHapus.setOnClickListener {
                onItemClick(item)
            }

            // ROOT CLICK - Open Detail Progress Activity
            root.setOnClickListener {
                val context = root.context
                val intent = Intent(context, DetailProgresActivity::class.java).apply {
                    // Kirim semua data yang diperlukan
                    putExtra("id_pantau_progess", item.id_pantau_progess ?: 0)
                    putExtra("jumlah_realisasi_absolut", item.jumlah_realisasi_absolut ?: 0)
                    putExtra("jumlah_realisasi_kumulatif", item.jumlah_realisasi_kumulatif ?: 0)
                    putExtra("catatan_aktivitas", item.catatan_aktivitas ?: "")
                    putExtra("created_at", item.created_at ?: "")

                    // Data tambahan jika ada
                    putExtra("id_pcl", item.id_pcl ?: 0)

                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = listProgres.size

    fun updateData(newData: List<PantauProgres>) {
        listProgres = newData
        notifyDataSetChanged()
    }
}