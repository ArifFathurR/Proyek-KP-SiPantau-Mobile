package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.LaporanLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanEntity

@Dao
interface LaporanDao {

    // Laporan dari server (terkirim)
    @Query("SELECT * FROM laporan_local ORDER BY created_at DESC")
    suspend fun getAllServerLaporan(): List<LaporanLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerList(list: List<LaporanLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(l: LaporanLocalEntity)

    @Query("DELETE FROM laporan_local")
    suspend fun deleteAllServer()

    @Query("DELETE FROM laporan_local WHERE id_sipantau_transaksi = :id")
    suspend fun deleteServerById(id: Int)

    // Pending laporan (offline)
    @Query("SELECT * FROM pending_laporan ORDER BY created_at DESC")
    suspend fun getAllPending(): List<PendingLaporanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(p: PendingLaporanEntity): Long

    @Delete
    suspend fun deletePending(p: PendingLaporanEntity)

    @Query("DELETE FROM pending_laporan WHERE local_id = :localId")
    suspend fun deletePendingByLocalId(localId: Long)
}
