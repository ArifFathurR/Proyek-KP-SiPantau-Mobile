package com.example.sipantau.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.DetailProgresActivity
import com.example.sipantau.databinding.ItemListPantauProgresBinding
import com.example.sipantau.localData.entity.ProgresKeluargaEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProgresKeluargaAdapter(
    private var list: List<ProgresKeluargaEntity>,
    private val onDeleteClick: ((ProgresKeluargaEntity) -> Unit)? = null
) : RecyclerView.Adapter<ProgresKeluargaAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemListPantauProgresBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListPantauProgresBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        with(holder.binding) {

            // =========================
            // DATA BINDING
            // =========================
            jmlRealisasiHarian.text =
                "Realisasi: ${item.jumlah_realisasi_absolut}"

            tvJumlahRealisasi.text =
                "Kumulatif: ${item.jumlah_realisasi_kumulatif ?: 0}"

            tvDetailProgress2.text =
                item.catatan_aktivitas ?: "-"
            tvDetailProgress3.text = "Id Sub-sls : ${item.id_subsls ?: "-"}"

            tgl.text =
                formatDayAndDate(item.created_at ?: "")

            // =========================
            // BUTTON HAPUS (OPTIONAL)
            // =========================
            btnHapus.setOnClickListener {
                onDeleteClick?.invoke(item)
            }

            // =========================
            // ROOT CLICK → DETAIL
            // =========================
            root.setOnClickListener {
                val context = root.context
                val intent = Intent(context, DetailProgresActivity::class.java).apply {

                    // mapping data → intent
                    putExtra("id_pantau_progess", item.server_id ?: 0)
                    putExtra("jumlah_realisasi_absolut", item.jumlah_realisasi_absolut)
                    putExtra("jumlah_realisasi_kumulatif", item.jumlah_realisasi_kumulatif ?: 0)
                    putExtra("catatan_aktivitas", item.catatan_aktivitas ?: "")
                    putExtra("created_at", item.created_at ?: "")

                    // khusus lapor keluarga (tidak ada id_pcl)
                    putExtra("is_lapor_keluarga", true)
                }

                context.startActivity(intent)
            }
        }
    }

    fun updateData(newData: List<ProgresKeluargaEntity>) {
        list = newData
        notifyDataSetChanged()
    }

    /**
     * Format tanggal → "Senin, 23-12-2025"
     */
    private fun formatDayAndDate(createdAt: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

            val dateTime = LocalDateTime.parse(createdAt, inputFormatter)

            val outputFormatter = DateTimeFormatter.ofPattern(
                "EEEE, dd-MM-yyyy",
                Locale("id", "ID")
            )

            dateTime.format(outputFormatter)

        } catch (e: Exception) {
            "-"
        }
    }
}