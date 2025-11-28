package com.example.sipantau.model

data class ReminderResponse(
    val status: Boolean,
    val message: String,
    val data: List<ReminderItem>
)

data class ReminderItem(
    val id_pcl: String,
    val tanggal: String,
    val target_harian: Int,
    val sudah_transaksi: Boolean,
    val sudah_progress: Boolean
)
