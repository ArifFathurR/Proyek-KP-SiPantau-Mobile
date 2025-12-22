package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kegiatan")
data class KegiatanEntity(
    @PrimaryKey val id_kegiatan_detail_proses: Int,
    val id_pcl: Int?,
    val id_pml: Int?,
    val total_realisasi_kumulatif: Int = 0,
    val target: String?,
    val status_approval: String?,
    val nama_kegiatan: String,
    val nama_kegiatan_detail_proses: String,
    val tanggal_mulai: String,
    val tanggal_selesai: String,
    val nama_kabupaten: String,
    val status_kegiatan: String,   // "aktif" atau "tidak aktif"
    val keterangan_wilayah: String
)
