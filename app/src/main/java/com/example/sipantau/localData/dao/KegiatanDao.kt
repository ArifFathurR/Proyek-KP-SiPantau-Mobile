package com.example.sipantau.localData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sipantau.localData.entity.KegiatanEntity

@Dao
interface KegiatanDao {
    @Query("SELECT * FROM kegiatan")
    suspend fun getAll(): List<KegiatanEntity>

    @Query("SELECT * FROM kegiatan WHERE status_kegiatan = :status")
    suspend fun getByStatus(status: String): List<KegiatanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(kegiatan: List<KegiatanEntity>)

    @Query("DELETE FROM kegiatan")
    suspend fun deleteAll()
}
