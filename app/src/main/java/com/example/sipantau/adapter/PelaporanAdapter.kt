package com.example.sipantau.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sipantau.DetailLaporanActivity
import com.example.sipantau.R
import com.example.sipantau.databinding.ItemListLaporanBinding
import com.example.sipantau.model.DisplayLaporan

class PelaporanAdapter(
    private var items: List<DisplayLaporan>,
    private val onDeleteClick: (DisplayLaporan) -> Unit,
    private val onSendClick: (DisplayLaporan) -> Unit
) : RecyclerView.Adapter<PelaporanAdapter.VH>() {

    inner class VH(val b: ItemListLaporanBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemListLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        with(holder.b) {

            // Text fields
            judul.text = item.nama_kegiatan_detail_proses ?: "Laporan"
            tanggal.text = item.created_at
            textView.text = item.resume
            kegiatan.text = item.nama_kegiatan_detail_proses ?: ""

            // IMAGE: pending -> localImagePath, else server (image_url or imagepath)
            val rawImg = if (item.isPending) item.localImagePath else (item.image_url ?: item.imagepath)

            if (!rawImg.isNullOrEmpty()) {
                var img = rawImg.trim()

                // Replace localhost to emulator host. Keep port if present.
                if (img.contains("localhost")) {
                    img = img.replace("localhost", "riau.web.bps.go.id/sipantau/")
                }

                // If local file path (pending), Glide can load file path directly
                Glide.with(root.context)
                    .load(img)
                    .placeholder(R.drawable.default_image)
                    .error(R.drawable.default_image)
                    .into(gambar)
            } else {
                gambar.setImageResource(R.drawable.default_image)
            }

            // DELETE button (always visible)
            btnDelete.setOnClickListener { onDeleteClick(item) }

            // SEND button visible only for pending items
            btnSend.visibility = if (item.isPending) View.VISIBLE else View.GONE

            // when clicked invoke activity's handler
            btnSend.setOnClickListener {
                // disable button briefly to prevent double clicks
                it.isEnabled = false
                onSendClick(item)
                // re-enable after callback returns control
                it.isEnabled = true
            }

            // ROOT CLICK - Open Detail Activity
            root.setOnClickListener {
                val context = root.context
                val intent = Intent(context, DetailLaporanActivity::class.java).apply {
                    // Kirim semua data yang diperlukan
                    putExtra("nama_kegiatan", item.nama_kegiatan)
                    putExtra("nama_kegiatan_detail_proses", item.nama_kegiatan_detail_proses)
                    putExtra("resume", item.resume)
                    putExtra("created_at", item.created_at)
                    putExtra("nama_kabupaten", item.nama_kabupaten)
                    putExtra("nama_kecamatan", item.nama_kecamatan)
                    putExtra("nama_desa", item.nama_desa)
                    putExtra("latitude", item.latitude)
                    putExtra("longitude", item.longitude)

                    // Image handling
                    val imageUrl = if (item.isPending) {
                        item.localImagePath
                    } else {
                        var img = (item.image_url ?: item.imagepath) ?: ""
                        if (img.contains("localhost")) {
                            img = img.replace("localhost", "riau.web.bps.go.id/sipantau/")
                        }
                        img
                    }
                    putExtra("image_url", imageUrl)
                    putExtra("isPending", item.isPending)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newList: List<DisplayLaporan>) {
        items = newList
        notifyDataSetChanged()
    }
}