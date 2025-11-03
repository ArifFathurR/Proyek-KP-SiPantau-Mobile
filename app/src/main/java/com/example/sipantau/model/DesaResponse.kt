package com.example.sipantau.model

data class DesaResponse(
    val status: String,
    val message: String,
    val data: List<Desa>
)

data class Desa(
    val id_desa: Int?,
    val id_kecamatan: Int?,
    val nama_desa: String
)
