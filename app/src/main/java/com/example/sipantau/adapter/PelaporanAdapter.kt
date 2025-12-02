package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            judul.text = item.nama_kegiatan ?: "Laporan"
            tanggal.text = item.created_at
            textView.text = item.resume
            kegiatan.text = item.nama_kegiatan_detail_proses ?: ""

            // IMAGE: pending -> localImagePath, else server (image_url or imagepath)
            val rawImg = if (item.isPending) item.localImagePath else (item.image_url ?: item.imagepath)

            if (!rawImg.isNullOrEmpty()) {
                var img = rawImg.trim()

                // Replace localhost to emulator host. Keep port if present.
                // Handle variants: http://localhost/uploads/...  or http://localhost:8080/uploads/...
                if (img.contains("localhost")) {
                    // Replace hostname only (preserve :port if present). We'll replace "localhost" -> "10.0.2.2"
                    img = img.replace("localhost", "10.14.11.32")
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
                // re-enable after callback returns control (Activity will refresh list and rebind)
                it.isEnabled = true
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newList: List<DisplayLaporan>) {
        items = newList
        notifyDataSetChanged()
    }
}
