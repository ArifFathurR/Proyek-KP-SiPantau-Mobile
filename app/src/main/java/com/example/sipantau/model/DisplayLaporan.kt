package com.example.sipantau.model

import com.example.sipantau.localData.entity.PendingLaporanEntity

data class DisplayLaporan(
    val isPending: Boolean,
    val serverId: Int?,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val imagepath: String?,
    val image_url: String?,
    val nama_kegiatan: String,
    val nama_kegiatan_detail_proses: String?,
    val nama_kabupaten: String?,
    val nama_kecamatan: String?,
    val nama_desa: String?,
    val created_at: String?,
    val localId: Long?,
    val localImagePath: String?,
    val id_kecamatan: Int?,
    val id_desa: Int?,

    // ✨ TAMBAHAN: Simpan reference ke PendingLaporanEntity asli
    // Ini memastikan semua data pending termasuk created_at terkirim dengan benar
    val pendingEntity: PendingLaporanEntity? = null
)