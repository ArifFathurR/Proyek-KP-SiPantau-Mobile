package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_laporan")
data class PendingLaporanEntity(
    @PrimaryKey(autoGenerate = true)
    val local_id: Long = 0L,
    val id_pcl: Int,
    val id_kegiatan_detail_proses: Int,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val id_kecamatan: Int?,
    val id_desa: Int?,
    val local_image_path: String?, // path di filesDir
    val created_at: String,
    val is_sending: Boolean = false
)
