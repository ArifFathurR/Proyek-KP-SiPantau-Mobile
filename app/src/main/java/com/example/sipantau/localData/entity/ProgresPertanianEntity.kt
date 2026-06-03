package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progres_pertanian")
data class ProgresPertanianEntity(

    @PrimaryKey(autoGenerate = true)
    val local_id: Long = 0L,

    val server_id: Int? = null,

    val jumlah_realisasi_absolut: Int,
    val jumlah_realisasi_kumulatif: Int? = null,
    val catatan_aktivitas: String?,
    val id_subsls: String? = null,

    val created_at: String? = null,
    val updated_at: String? = null,

    val is_synced: Boolean = false
)
