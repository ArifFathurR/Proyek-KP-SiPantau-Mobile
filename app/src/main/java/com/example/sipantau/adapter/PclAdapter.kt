package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.databinding.ItemListPclBinding
import com.example.sipantau.model.Pcl

class PclAdapter(
    private var data: List<Pcl>,
    private val onDetailClick: (Pcl) -> Unit,
    private val onApproveClick: (Pcl) -> Unit
) : RecyclerView.Adapter<PclAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemListPclBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListPclBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val b = holder.binding

        b.namaPcl.text = item.nama_pcl
        b.totalTarget.text = "Total Target : ${item.target}"
        b.totalTargetAktual.text = "Realisasi : ${item.total_realisasi_kumulatif}"

        val persentaseValue = item.persentase.replace("%", "").toFloatOrNull() ?: 0f
        b.progressBar.progress = persentaseValue.toInt()
        b.tvPersentase.text = "${persentaseValue.toInt()}%"

        when {
            item.status_approval == "1" || item.status_approval.equals("approved", ignoreCase = true) -> {
                b.btnApprove.visibility = View.VISIBLE
                b.btnApprove.text = "Approved ✓"
                b.btnApprove.isEnabled = false
                b.btnApprove.alpha = 0.6f
                b.btnApprove.setBackgroundColor(0xFF4CAF50.toInt())
            }
            persentaseValue >= 100f -> {
                b.btnApprove.visibility = View.VISIBLE
                b.btnApprove.text = "Approve"
                b.btnApprove.isEnabled = true
                b.btnApprove.alpha = 1f
                b.btnApprove.setBackgroundColor(0xFF4CAF50.toInt())
            }
            else -> {
                b.btnApprove.visibility = View.GONE
            }
        }

        b.btnDetail.setOnClickListener { onDetailClick(item) }
        b.btnApprove.setOnClickListener { if (b.btnApprove.isEnabled) onApproveClick(item) }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<Pcl>) {
        data = newData
        notifyDataSetChanged()
    }
}
