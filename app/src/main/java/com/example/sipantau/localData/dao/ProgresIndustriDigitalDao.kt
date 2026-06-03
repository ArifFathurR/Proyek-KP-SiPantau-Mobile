package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.ProgresIndustriDigitalEntity

@Dao
interface ProgresIndustriDigitalDao {

    @Query("SELECT * FROM progres_industri_digital ORDER BY created_at DESC, local_id DESC")
    suspend fun getAll(): List<ProgresIndustriDigitalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProgresIndustriDigitalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ProgresIndustriDigitalEntity>)

    @Query("DELETE FROM progres_industri_digital")
    suspend fun deleteAll()

    @Query("DELETE FROM progres_industri_digital WHERE server_id = :serverId OR local_id = :localId")
    suspend fun deleteByServerIdOrLocalId(serverId: Int?, localId: Long)

    @Query("SELECT * FROM progres_industri_digital WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProgresIndustriDigitalEntity>

    @Update
    suspend fun update(entity: ProgresIndustriDigitalEntity)
}