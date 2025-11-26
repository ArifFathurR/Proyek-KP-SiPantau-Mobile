package com.example.sipantau.model

data class KegiatanResponse(
    val status: String,
    val roles: List<String>,
    val kegiatan_pml: List<Kegiatan> = emptyList(),
    val kegiatan_pcl: List<Kegiatan> = emptyList()
)

data class Kegiatan(
    val id_pcl: Int? = null,
    val id_pml: Int? = null,
    val id_kegiatan_detail_proses: Int? = null,
    val target: String? = null,
    val status_approval: String? = null,
    val nama_kegiatan: String = "",
    val nama_kegiatan_detail_proses: String = "",
    val tanggal_mulai: String = "",
    val tanggal_selesai: String = "",
    val nama_kabupaten: String = "",
    val nama_pml: String? = null,
    val keterangan_wilayah: String = "",
    val status_kegiatan: String = "tidak aktif"    // aktif / tidak aktif
)

data class TotalKegPClResponse(
    val status: String,
    val total_kegiatan_pcl: Int
)

data class TotalKegPMlResponse(
    val status: String,
    val total_kegiatan_pml: Int
)

