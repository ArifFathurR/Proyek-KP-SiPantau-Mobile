package com.example.sipantau.model

// For Adapter rendering — unify server-laporan and pending-laporan
data class DisplayLaporan(
    val isPending: Boolean,
    // if from server:
    val serverId: Int? = null,
    val resume: String,
    val latitude: String?,
    val longitude: String?,
    val imagepath: String?,
    val image_url: String?,
    val nama_kegiatan: String?,
    val nama_kegiatan_detail_proses: String?,
    val nama_kabupaten: String?,
    val nama_kecamatan: String?,
    val nama_desa: String?,
    val created_at: String,
    val id_kecamatan: Int?,
    val id_desa:Int?,
    // if pending:
    val localId: Long? = null,
    val localImagePath: String? = null
)
