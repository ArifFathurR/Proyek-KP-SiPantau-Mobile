package com.example.sipantau.repository

import android.content.Context
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.DesaLocalEntity
import com.example.sipantau.localData.entity.KecamatanLocalEntity

class WilayahRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.wilayahDao()

    suspend fun saveKecamatan(list: List<KecamatanLocalEntity>) {
        dao.insertKecamatan(list)
    }

    suspend fun saveDesa(list: List<DesaLocalEntity>) {
        dao.insertDesa(list)
    }

    suspend fun getKecamatan() = dao.getAllKecamatan()

    suspend fun getDesaByKecamatan(id: Int) = dao.getDesaByKecamatan(id)
}
