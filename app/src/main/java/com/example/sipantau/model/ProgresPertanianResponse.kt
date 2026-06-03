package com.example.sipantau.model

data class ProgresPertanianResponse (
    val status: String,
    val message: String,
    val data: ProgresPertanian
)

data class ProgresPertanianListResponse(
    val status: String,
    val sobat_id: Int,
    val total_kumulatif: Int,
    val total_entry: Int,
    val data: List<ProgresPertanian>
)

data class ProgresPertanian(
    val id_pantau_progres_pertanian : Int?,
    val sobat_id: Int?,
    val jumlah_realisasi_absolut: Int,
    val jumlah_realisasi_kumulatif: Int,
    val catatan_aktivitas: String?,
    val created_at: String?,
    val updated_at: String?,
    val id_subsls: String? = null
)
