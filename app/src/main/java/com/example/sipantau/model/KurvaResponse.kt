package com.example.sipantau.model

data class KurvaResponse(
    val status: Boolean,
    val message: String,
    val data: List<KurvaData>
)

data class KurvaData(
    val tanggal_target: String,
    val target_kumulatif_absolut: Float,
    val target_harian_absolut: Float,
    val target_persen_kumulatif: Float,
    val realisasi_harian: Float,
    val realisasi_kumulatif: Float,
)
