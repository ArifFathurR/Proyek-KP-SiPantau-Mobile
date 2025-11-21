package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desa")
data class DesaLocalEntity(
    @PrimaryKey(autoGenerate = false)
    val id_desa: Int,
    val id_kecamatan: Int?,
    val nama_desa: String
)
