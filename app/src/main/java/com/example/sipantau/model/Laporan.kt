package com.example.sipantau.model

data class Laporan(
    val id_transaksi: Int,
    val id_kegiatan_detail: String,
    val resume: String,
    val tanggal_transaksi: String,
    val latitude: String?,
    val longitude: String?
)