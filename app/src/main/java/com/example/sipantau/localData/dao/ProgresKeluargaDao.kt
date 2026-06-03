package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.ProgresKeluargaEntity

@Dao
interface ProgresKeluargaDao {

    @Query("SELECT * FROM progres_keluarga ORDER BY created_at DESC, local_id DESC")
    suspend fun getAll(): List<ProgresKeluargaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProgresKeluargaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ProgresKeluargaEntity>)

    @Query("DELETE FROM progres_keluarga")
    suspend fun deleteAll()

    @Query("DELETE FROM progres_keluarga WHERE server_id = :serverId OR local_id = :localId")
    suspend fun deleteByServerIdOrLocalId(serverId: Int?, localId: Long)

    @Query("SELECT * FROM progres_keluarga WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProgresKeluargaEntity>

    @Update
    suspend fun update(entity: ProgresKeluargaEntity)
}