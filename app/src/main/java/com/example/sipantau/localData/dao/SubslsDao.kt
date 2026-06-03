package com.example.sipantau.localData.dao

import androidx.room.*
import com.example.sipantau.localData.entity.SubslsLocalEntity

@Dao
interface SubslsDao {
    @Query("SELECT * FROM subsls_local")
    suspend fun getAll(): List<SubslsLocalEntity>

    @Query("SELECT * FROM subsls_local WHERE (:idDesa IS NULL OR id_desa = :idDesa OR id_desa IS NULL) AND (:idKecamatan IS NULL OR id_kecamatan = :idKecamatan OR id_kecamatan IS NULL) AND (:idPcl IS NULL OR id_pcl = :idPcl OR id_pcl IS NULL)")
    suspend fun getFiltered(idDesa: Int?, idKecamatan: Int?, idPcl: Int?): List<SubslsLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SubslsLocalEntity>)

    @Query("DELETE FROM subsls_local")
    suspend fun deleteAll()
}
