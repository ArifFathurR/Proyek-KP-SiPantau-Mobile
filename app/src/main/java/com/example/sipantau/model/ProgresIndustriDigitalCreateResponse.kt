package com.example.sipantau.model

data class ProgresIndustriDigitalCreateResponse(
    val status: String,
    val message: String,
    val data: ProgresIndustriDigital
)

data class ProgresIndustriDigitalListResponse(
    val status: String,
    val sobat_id: Int,
    val total_kumulatif: Int,
    val total_entry: Int,
    val data: List<ProgresIndustriDigital>
)

data class ProgresIndustriDigital(
    val id_lapor_progres_industri_digital: Int?,
    val sobat_id: Int?,
    val jumlah_realisasi_absolut: Int,
    val jumlah_realisasi_kumulatif: Int,
    val catatan_aktivitas: String?,
    val created_at: String?,
    val updated_at: String?
)