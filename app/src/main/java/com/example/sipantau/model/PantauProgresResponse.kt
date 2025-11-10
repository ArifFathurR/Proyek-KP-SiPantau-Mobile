package com.example.sipantau.model

data class PantauProgresCreateResponse(
    val status: String,
    val message: String,
    val data: PantauProgres
)

data class PantauProgresListResponse(
    val status: String,
    val id_pcl: Int?,
    val total_kumulatif: Int?,
    val total_entry: Int?,
    val data: List<PantauProgres>
)


data class PantauProgres(
    val id_pantau_progess : Int?,
    val id_pcl : Int?,
    val jumlah_realisasi_absolut : Int?,
    val jumlah_realisasi_kumulatif : Int?,
    val catatan_aktivitas : String,
    val created_at : String
)