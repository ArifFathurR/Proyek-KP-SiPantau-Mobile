package com.example.sipantau.model

data class PclResponse(
    val status: Boolean,
    val message: String,
    val data: List<Pcl>
)


data class Pcl(
    val id_pcl: Int? = null,
    val sobat_id: String?,
    val nama_pcl: String?,
    val hp: String?,
    val id_pml: String?,
    val status_approval: String?,
    val target: Int?,
    val total_realisasi_kumulatif: Int?,
    val persentase: String
)