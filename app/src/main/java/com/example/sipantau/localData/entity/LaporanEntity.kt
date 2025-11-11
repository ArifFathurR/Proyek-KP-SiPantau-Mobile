package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "laporan")
data class LaporanEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id_pcl: Int,
    val id_kegiatan: Int,
    val jumlah_realisasi_absolut: Int,
    val catatan_aktivitas: String,
    val status: String, // "pending" atau "terkirim"
    val created_at: Long = System.currentTimeMillis()
)
