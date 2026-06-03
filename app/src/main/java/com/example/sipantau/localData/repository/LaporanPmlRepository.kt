package com.example.sipantau.repository

import android.content.Context
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.LaporanPmlLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanPmlEntity
import com.example.sipantau.model.PelaporanPmlResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import java.io.File

class LaporanPmlRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).laporanPmlDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
            return raw?.let { "Bearer $it" }
        }

    // ─────────────────────────────────────────────────────────────────
    // READ — Pending list (data offline belum terkirim)
    // ─────────────────────────────────────────────────────────────────

    suspend fun getPendingList(): List<PendingLaporanPmlEntity> =
        withContext(Dispatchers.IO) {
            dao.getAllPending()
        }

    // ─────────────────────────────────────────────────────────────────
    // READ — Server list (cache laporan yang sudah terkirim)
    // ─────────────────────────────────────────────────────────────────

    suspend fun getServerList(idPml: Int): List<LaporanPmlLocalEntity> =
        withContext(Dispatchers.IO) {
            if (token != null) {
                try {
                    val call = ApiClient.instance.getLaporanPml(token!!, idPml)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val apiList = resp.body()!!.data

                        val entities = apiList.map {
                            LaporanPmlLocalEntity(
                                id_sipantau_transaksi_pml = it.id_sipantau_transaksi_pml,
                                id_pml = it.id_pml,
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

                        // Bersihkan cache lama, simpan data terbaru
                        dao.deleteAllServer()
                        dao.insertServerList(entities)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext dao.getAllServerLaporan()
        }

    // ─────────────────────────────────────────────────────────────────
    // READ — Merged list (server + pending, berguna untuk satu tampilan)
    // ─────────────────────────────────────────────────────────────────

    suspend fun getMergedList(idPml: Int?): Pair<List<LaporanPmlLocalEntity>, List<PendingLaporanPmlEntity>> =
        withContext(Dispatchers.IO) {

            if (token != null && idPml != null) {
                try {
                    val call: Call<PelaporanPmlResponse> =
                        ApiClient.instance.getLaporanPml(token!!, idPml)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val entities = resp.body()!!.data.map {
                            LaporanPmlLocalEntity(
                                id_sipantau_transaksi_pml = it.id_sipantau_transaksi_pml,
                                id_pml = it.id_pml,
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

    // ─────────────────────────────────────────────────────────────────
    // WRITE — Simpan pending (offline)
    // ─────────────────────────────────────────────────────────────────

    suspend fun savePending(p: PendingLaporanPmlEntity): Long =
        withContext(Dispatchers.IO) {
            dao.insertPending(p)
        }

    // ─────────────────────────────────────────────────────────────────
    // DELETE — Hapus laporan server (call API + hapus cache lokal)
    // ─────────────────────────────────────────────────────────────────

    suspend fun deleteServerById(id: Int) = withContext(Dispatchers.IO) {
        try {
            if (token != null) {
                val resp = ApiClient.instance.hapusLaporanPml(token!!, id).execute()
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

    // ─────────────────────────────────────────────────────────────────
    // DELETE — Hapus pending lokal
    // ─────────────────────────────────────────────────────────────────

    suspend fun deletePendingByLocalId(localId: Long) =
        withContext(Dispatchers.IO) {
            dao.deletePendingByLocalId(localId)
        }

    // ─────────────────────────────────────────────────────────────────
    // UPLOAD — Upload satu pending ke server
    // ─────────────────────────────────────────────────────────────────

    suspend fun uploadPendingOnce(p: PendingLaporanPmlEntity): Boolean =
        withContext(Dispatchers.IO) {
            if (token == null) return@withContext false
            try {
                val idPmlBody      = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_pml.toString())
                val idKegBody      = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_kegiatan_detail_proses.toString())
                val resumeBody     = RequestBody.create("text/plain".toMediaTypeOrNull(), p.resume)
                val latBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), p.latitude ?: "")
                val lonBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), p.longitude ?: "")
                val kecBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_kecamatan?.toString() ?: "")
                val desaBody       = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_desa?.toString() ?: "")
                val createdAtBody  = RequestBody.create("text/plain".toMediaTypeOrNull(), p.created_at ?: "")

                val part = p.local_image_path?.let { File(it) }?.let { f ->
                    val req = RequestBody.create("image/*".toMediaTypeOrNull(), f)
                    MultipartBody.Part.createFormData("image", f.name, req)
                } ?: MultipartBody.Part.createFormData(
                    "image",
                    "empty.jpg",
                    RequestBody.create("text/plain".toMediaTypeOrNull(), "")
                )

                val resp = ApiClient.instance.createPelaporanPml(
                    token!!,
                    idPmlBody, idKegBody, resumeBody,
                    latBody, lonBody, kecBody, desaBody,
                    createdAtBody, part
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

    // ─────────────────────────────────────────────────────────────────
    // SYNC — Upload semua pending sekaligus (dipanggil dari WorkManager)
    // ─────────────────────────────────────────────────────────────────

    suspend fun syncAllPending(): Int = withContext(Dispatchers.IO) {
        var success = 0
        val list = dao.getAllPending()
        for (p in list) {
            if (uploadPendingOnce(p)) success++
        }
        success
    }
}