package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kecamatan")
data class KecamatanLocalEntity(
    @PrimaryKey(autoGenerate = false)
    val id_kecamatan: Int,
    val id_kabupaten: Int?,
    val nama_kecamatan: String
)
