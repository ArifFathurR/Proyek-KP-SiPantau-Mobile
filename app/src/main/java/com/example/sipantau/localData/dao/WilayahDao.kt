package com.example.sipantau.localData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sipantau.localData.entity.KecamatanLocalEntity
import com.example.sipantau.localData.entity.DesaLocalEntity

@Dao
interface WilayahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKecamatan(list: List<KecamatanLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesa(list: List<DesaLocalEntity>)

    @Query("SELECT * FROM kecamatan")
    suspend fun getAllKecamatan(): List<KecamatanLocalEntity>

    @Query("SELECT * FROM desa WHERE id_kecamatan = :kecId")
    suspend fun getDesaByKecamatan(kecId: Int): List<DesaLocalEntity>
}
