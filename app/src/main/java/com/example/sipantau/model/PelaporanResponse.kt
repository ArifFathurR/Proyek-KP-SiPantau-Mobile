package com.example.sipantau.model

data class PelaporanResponse(
    val id_sipantau_transaksi: String,
    val sobat_id: String,
    val id_kegiatan_detail: String,
    val id_user_bridge: String?,
    val tanggal_transaksi: String,
    val resume: String?,
    val imagepath: String?,
    val nama_kegiatan_detail: String?
)
