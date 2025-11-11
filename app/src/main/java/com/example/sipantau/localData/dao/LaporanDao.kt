package com.example.sipantau.localData.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sipantau.localData.entity.LaporanEntity

@Dao
interface LaporanDao {
    @Query("SELECT * FROM laporan WHERE status = :status")
    fun getByStatus(status: String): LiveData<List<LaporanEntity>>

    @Query("SELECT * FROM laporan WHERE status = 'pending'")
    suspend fun getPendingNow(): List<LaporanEntity>

    @Insert
    suspend fun insert(laporan: LaporanEntity)

    @Query("UPDATE laporan SET status = 'terkirim' WHERE localId = :id")
    suspend fun markAsSent(id: Int)
}
