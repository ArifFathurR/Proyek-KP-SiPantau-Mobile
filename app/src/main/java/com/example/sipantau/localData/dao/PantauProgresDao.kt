package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.PantauProgresEntity

@Dao
interface PantauProgresDao {

    @Query("SELECT * FROM pantau_progres WHERE id_pcl = :idPcl ORDER BY created_at DESC, local_id DESC")
    suspend fun getAllByPcl(idPcl: Int): List<PantauProgresEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PantauProgresEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<PantauProgresEntity>)

    @Query("DELETE FROM pantau_progres")
    suspend fun deleteAll()

    @Query("DELETE FROM pantau_progres WHERE server_id = :serverId OR local_id = :localId")
    suspend fun deleteByServerIdOrLocalId(serverId: Int?, localId: Long)

    @Query("SELECT * FROM pantau_progres WHERE is_synced = 0")
    suspend fun getUnsynced(): List<PantauProgresEntity>

    @Update
    suspend fun update(entity: PantauProgresEntity)
}
