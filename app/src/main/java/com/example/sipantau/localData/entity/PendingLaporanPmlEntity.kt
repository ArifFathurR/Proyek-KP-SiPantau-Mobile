package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk laporan PML yang disimpan sementara secara lokal
 * ketika perangkat sedang offline. Akan di-upload ke server
 * saat koneksi tersedia kembali.
 */
@Entity(tableName = "pending_laporan_pml")
data class PendingLaporanPmlEntity(
    @PrimaryKey(autoGenerate = true)
    val local_id: Long = 0,

    val id_pml: Int,
    val id_kegiatan_detail_proses: Int,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val id_kecamatan: Int?,
    val id_desa: Int?,
    val local_image_path: String?,

    // Timestamp waktu laporan dibuat di perangkat.
    // Dikirim ke server saat upload agar created_at akurat.
    val created_at: String?
)