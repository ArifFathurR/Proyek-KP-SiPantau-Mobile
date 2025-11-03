package com.example.sipantau.model

data class KecamatanResponse(
    val status: String,
    val message: String,
    val data: List<Kecamatan>
)

data class Kecamatan(
    val id_kecamatan: Int?,
    val id_kabupaten: Int?,
    val nama_kecamatan: String
)