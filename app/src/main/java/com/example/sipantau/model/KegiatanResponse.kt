package com.example.sipantau.model

data class KegiatanResponse(
    val status: String,
    val role_aktif: String,
    val kegiatan: List<Kegiatan>
)

data class Kegiatan(
    val id_pcl: Int?,
    val id_pml: Int?,
    val id_kegiatan_detail_proses: Int?,
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
