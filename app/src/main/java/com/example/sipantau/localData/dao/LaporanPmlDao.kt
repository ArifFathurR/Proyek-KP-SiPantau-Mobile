package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.LaporanPmlLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanPmlEntity

@Dao
interface LaporanPmlDao {

    // ===================== LAPORAN SERVER (TERKIRIM) =====================

    @Query("SELECT * FROM laporan_pml_local ORDER BY created_at DESC")
    suspend fun getAllServerLaporan(): List<LaporanPmlLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerList(list: List<LaporanPmlLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(l: LaporanPmlLocalEntity)

    @Query("DELETE FROM laporan_pml_local")
    suspend fun deleteAllServer()

    @Query("DELETE FROM laporan_pml_local WHERE id_sipantau_transaksi_pml = :id")
    suspend fun deleteServerById(id: Int)

    // ===================== PENDING LAPORAN (OFFLINE) =====================

    @Query("SELECT * FROM pending_laporan_pml ORDER BY created_at DESC")
    suspend fun getAllPending(): List<PendingLaporanPmlEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(p: PendingLaporanPmlEntity): Long

    @Delete
    suspend fun deletePending(p: PendingLaporanPmlEntity)

    @Query("DELETE FROM pending_laporan_pml WHERE local_id = :localId")
    suspend fun deletePendingByLocalId(localId: Long)
}