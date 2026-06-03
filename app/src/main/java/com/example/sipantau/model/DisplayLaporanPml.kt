package com.example.sipantau.model

import com.example.sipantau.localData.entity.PendingLaporanPmlEntity

data class DisplayLaporanPml(
    val isPending: Boolean,

    // Server data
    val serverId: Int? = null,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val imagepath: String?,
    val image_url: String?,
    val nama_kegiatan: String,
    val nama_kegiatan_detail_proses: String,
    val nama_kabupaten: String?,
    val nama_kecamatan: String?,
    val nama_desa: String?,
    val created_at: String?,

    // Pending/local data
    val localId: Long? = null,
    val localImagePath: String? = null,
    val id_kecamatan: Int? = null,
    val id_desa: Int? = null,

    // Referensi entity asli untuk upload ulang
    val pendingEntity: PendingLaporanPmlEntity? = null
)