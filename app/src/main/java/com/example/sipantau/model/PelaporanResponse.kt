package com.example.sipantau.model

data class PelaporanResponse(
    val status: String,
    val tampildata: List<Laporan>,
    val row_count: Int
)