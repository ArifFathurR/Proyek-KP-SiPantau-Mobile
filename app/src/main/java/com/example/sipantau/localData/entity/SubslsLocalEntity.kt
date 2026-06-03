package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subsls_local")
data class SubslsLocalEntity(
    @PrimaryKey val id_subsls: String,
    val nama_subsls: String,
    val id_desa: Int?,
    val nama_desa: String?,
    val id_kecamatan: Int?,
    val id_pcl: Int?,
    val nama_kecamatan: String?
)
