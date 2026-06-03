package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.ProgresPertanianEntity

@Dao
interface ProgresPertanianDao {

    @Query("SELECT * FROM progres_pertanian ORDER BY created_at DESC, local_id DESC")
    suspend fun getAll(): List<ProgresPertanianEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProgresPertanianEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ProgresPertanianEntity>)

    @Query("DELETE FROM progres_pertanian")
    suspend fun deleteAll()

    @Query("DELETE FROM progres_pertanian WHERE server_id = :serverId OR local_id = :localId")
    suspend fun deleteByServerIdOrLocalId(serverId: Int?, localId: Long)

    @Query("SELECT * FROM progres_pertanian WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProgresPertanianEntity>

    @Update
    suspend fun update(entity: ProgresPertanianEntity)
}
