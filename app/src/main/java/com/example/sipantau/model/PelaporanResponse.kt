package com.example.sipantau.model

data class PelaporanResponse(
    val status: String,
    val total_laporan: Int,
    val data: List<LaporanData>
)

data class LaporanData(
    val id_sipantau_transaksi: Int,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val imagepath: String?, // tambahkan ini
    val image_url: String?,
    val nama_kegiatan: String,
    val nama_kegiatan_detail_proses: String,
    val nama_kabupaten: String?,
    val nama_kecamatan: String?,
    val nama_desa: String?,
    val created_at: String
)
