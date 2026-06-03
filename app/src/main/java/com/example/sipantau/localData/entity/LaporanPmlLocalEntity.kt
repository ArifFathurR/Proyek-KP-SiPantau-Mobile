package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk laporan PML yang sudah terkirim ke server.
 * Disimpan lokal sebagai cache agar bisa dibaca saat offline.
 */
@Entity(tableName = "laporan_pml_local")
data class LaporanPmlLocalEntity(
    @PrimaryKey(autoGenerate = false)
    val id_sipantau_transaksi_pml: Int,
    val id_pml: Int,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val imagepath: String?,
    val image_url: String?,
    val nama_kegiatan: String,
    val nama_kegiatan_detail_proses: String,
    val nama_kabupaten: String?,
    val nama_kecamatan: String?,
    val nama_desa: String?,
    val created_at: String
)