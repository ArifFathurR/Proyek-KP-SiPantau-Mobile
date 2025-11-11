package com.example.sipantau.localData.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantau_progres")
data class PantauProgresEntity(
    @PrimaryKey(autoGenerate = true)
    val local_id: Long = 0L,           // primary key lokal
    val server_id: Int? = null,        // id dari server (id_pantau_progess)
    val id_pcl: Int,
    val jumlah_realisasi_absolut: Int,
    val jumlah_realisasi_kumulatif: Int? = null,
    val catatan_aktivitas: String,
    val created_at: String? = null,
    val is_synced: Boolean = false     // true jika sudah dikirim dan sesuai server
)
