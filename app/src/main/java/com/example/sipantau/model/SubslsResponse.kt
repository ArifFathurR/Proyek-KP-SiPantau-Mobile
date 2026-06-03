package com.example.sipantau.model

data class SubslsResponse(
    val status: String,
    val message: String,
    val total: Int,
    val data: List<SubslsData>
)

data class SubslsData(
    val id_subsls: String,
    val nama_subsls: String,
    val id_desa: Int?,
    val nama_desa: String?,
    val id_kecamatan: Int?,
    val id_pcl: Int?,
    val nama_kecamatan: String?
)