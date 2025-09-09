package com.example.sipantau.model

data class SipantauTransaksi (
    val id_sipantau_transaksi: Int,
    val sobat_id: String,
    val id_kegiatan_detail: Int,
    val id_user_bridge: Int,
    val tanggal_transaksi: String,
    val resume: String,
    val latitude: String,
    val longitude: String,
    val idkec: String,
    val iddesa: String,
    val imagepath: String,
    val imagelocated: String
)