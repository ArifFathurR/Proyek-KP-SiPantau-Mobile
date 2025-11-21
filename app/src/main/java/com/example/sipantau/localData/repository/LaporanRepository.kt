package com.example.sipantau.repository

import android.content.Context
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.LaporanLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanEntity
import com.example.sipantau.model.PelaporanResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import java.io.File

class LaporanRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).laporanDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
            return raw?.let { "Bearer $it" }
        }

    suspend fun getPendingList(): List<PendingLaporanEntity> =
        withContext(Dispatchers.IO) {
            dao.getAllPending()
        }
    suspend fun getServerList(idPcl: Int): List<LaporanLocalEntity> =
        withContext(Dispatchers.IO) {

            if (token != null) {
                try {
                    val call = ApiClient.instance.getLaporan(token!!, idPcl)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val apiList = resp.body()!!.data

                        val entities = apiList.map {
                            LaporanLocalEntity(
                                id_sipantau_transaksi = it.id_sipantau_transaksi,
                                resume = it.resume,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                imagepath = it.imagepath,
                                image_url = it.image_url,
                                nama_kegiatan = it.nama_kegiatan,
                                nama_kegiatan_detail_proses = it.nama_kegiatan_detail_proses,
                                nama_kabupaten = it.nama_kabupaten,
                                nama_kecamatan = it.nama_kecamatan,
                                nama_desa = it.nama_desa,
                                created_at = it.created_at
                            )
                        }

                        // Bersihkan & simpan ulang
                        dao.deleteAllServer()
                        dao.insertServerList(entities)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext dao.getAllServerLaporan()
        }




    suspend fun getMergedList(idPcl: Int?): Pair<List<LaporanLocalEntity>, List<PendingLaporanEntity>> =
        withContext(Dispatchers.IO) {

            if (token != null && idPcl != null) {
                try {
                    val call: Call<PelaporanResponse> = ApiClient.instance.getLaporan(token!!, idPcl)
                    val resp = call.execute()
                    if (resp.isSuccessful && resp.body() != null) {
                        val apiList = resp.body()!!.data
                        val entities = apiList.map {
                            LaporanLocalEntity(
                                id_sipantau_transaksi = it.id_sipantau_transaksi,
                                resume = it.resume,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                imagepath = it.imagepath,
                                image_url = it.image_url,
                                nama_kegiatan = it.nama_kegiatan,
                                nama_kegiatan_detail_proses = it.nama_kegiatan_detail_proses,
                                nama_kabupaten = it.nama_kabupaten,
                                nama_kecamatan = it.nama_kecamatan,
                                nama_desa = it.nama_desa,
                                created_at = it.created_at
                            )
                        }
                        dao.deleteAllServer()
                        dao.insertServerList(entities)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val server = dao.getAllServerLaporan()
            val pending = dao.getAllPending()

            Pair(server, pending)
        }

    suspend fun savePending(p: PendingLaporanEntity): Long =
        withContext(Dispatchers.IO) {
            dao.insertPending(p)
        }

    suspend fun deleteServerById(id: Int) = withContext(Dispatchers.IO) {
        try {
            if (token != null) {
                val resp = ApiClient.instance.hapusLaporan(token!!, id).execute()
                if (resp.isSuccessful) {
                    dao.deleteServerById(id)
                }
            } else {
                dao.deleteServerById(id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePendingByLocalId(localId: Long) =
        withContext(Dispatchers.IO) {
            dao.deletePendingByLocalId(localId)
        }

    suspend fun uploadPendingOnce(p: PendingLaporanEntity): Boolean =
        withContext(Dispatchers.IO) {
            if (token == null) return@withContext false
            try {
                val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_pcl.toString())
                val idKegBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_kegiatan_detail_proses.toString())
                val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.resume)
                val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.latitude ?: "")
                val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.longitude ?: "")
                val kecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_kecamatan?.toString() ?: "")
                val desaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_desa?.toString() ?: "")

                val part = p.local_image_path?.let { File(it) }?.let { f ->
                    val req = RequestBody.create("image/*".toMediaTypeOrNull(), f)
                    MultipartBody.Part.createFormData("image", f.name, req)
                } ?: MultipartBody.Part.createFormData(
                    "image",
                    "empty.jpg",
                    RequestBody.create("text/plain".toMediaTypeOrNull(), "")
                )

                val resp = ApiClient.instance.createPelaporan(
                    token!!,
                    idPclBody, idKegBody, resumeBody, latBody, lonBody, kecBody, desaBody, part
                ).execute()

                if (resp.isSuccessful) {
                    dao.deletePendingByLocalId(p.local_id)
                    true
                } else false

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun syncAllPending(): Int = withContext(Dispatchers.IO) {
        var success = 0
        val list = dao.getAllPending()
        for (p in list) {
            if (uploadPendingOnce(p)) success++
        }
        success
    }

}
