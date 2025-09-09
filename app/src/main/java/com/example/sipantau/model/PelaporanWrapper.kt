package com.example.sipantau.model

data class PelaporanWrapper(
    val status: String,
    val tampildata: List<PelaporanResponse>,
    val row_count: Int
)