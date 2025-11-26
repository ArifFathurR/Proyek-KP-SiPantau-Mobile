package com.example.sipantau.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantau.databinding.ItemListPclBinding
import com.example.sipantau.model.Pcl

class PclAdapter(
    private var data: List<Pcl>,
    private val onDetailClick: (Pcl) -> Unit
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

        b.btnDetail.setOnClickListener {
            onDetailClick(item)
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<Pcl>) {
        data = newData
        notifyDataSetChanged()
    }
}
