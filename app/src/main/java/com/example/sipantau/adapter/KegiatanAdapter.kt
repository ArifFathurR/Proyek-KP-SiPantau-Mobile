package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.databinding.ItemListKegiatanBinding
import com.example.sipantau.model.Kegiatan

class KegiatanAdapter(
    private var listKegiatan: List<Kegiatan>,
    private val onItemClick: (Kegiatan) -> Unit,
    private val onDetailClick: (Kegiatan) -> Unit

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
            detailproses.text = item.nama_kegiatan_detail_proses
            judul.text = item.nama_kegiatan
            tanggal.text = "${item.tanggal_mulai} s.d ${item.tanggal_selesai}"
            textView.text = item.keterangan_wilayah
            targetRealisasi.text = "${item.total_realisasi_kumulatif} / ${item.target}"

            root.setOnClickListener { onItemClick(item) }
            btnDetail.setOnClickListener { onDetailClick(item) }
        }
    }

    override fun getItemCount() = listKegiatan.size

    // Safe update: hapus duplikat berdasarkan primary id (fallback)
    fun updateData(newData: List<Kegiatan>) {
        listKegiatan = newData.distinctBy {
            it.id_kegiatan_detail_proses ?: it.id_pcl ?: it.id_pml ?: 0
        }.filter { (it.id_kegiatan_detail_proses ?: it.id_pcl ?: it.id_pml ?: 0) != 0 }
        notifyDataSetChanged()
    }
}
