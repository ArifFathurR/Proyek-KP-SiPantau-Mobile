package com.example.sipantau.model

data class PelaporanPmlResponse(
    val status: String,
    val total_laporan: Int,
    val data: List<LaporanPmlData>
)

data class LaporanPmlData(
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
