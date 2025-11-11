// localData/repository/KegiatanRepository.kt
package com.example.sipantau.localData.repository

import android.content.Context
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.model.Kegiatan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class KegiatanRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).kegiatanDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(LoginActivity.PREF_TOKEN, null)?.let { "Bearer $it" }
        }

    /** Load kegiatan offline-online */
    suspend fun getKegiatan(): List<KegiatanEntity> = withContext(Dispatchers.IO) {
        val localData = dao.getAll() // ambil data lokal dulu

        if (isOnline()) {
            try {
                val response = ApiClient.instance.getKegiatan(token ?: "").awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    val kegiatanList = response.body()!!.kegiatan.map { toEntity(it) }
                    dao.deleteAll()
                    dao.insertAll(kegiatanList)
                    return@withContext kegiatanList
                }
            } catch (e: Exception) {
                // fallback ke data lokal
            }
        }
        return@withContext localData
    }

    private fun toEntity(k: Kegiatan) = KegiatanEntity(
        id_kegiatan_detail_proses = k.id_kegiatan_detail_proses ?: 0,
        id_pcl = k.id_pcl,
        id_pml = k.id_pml,
        target = k.target,
        status_approval = k.status_approval,
        nama_kegiatan = k.nama_kegiatan,
        nama_kegiatan_detail_proses = k.nama_kegiatan_detail_proses,
        tanggal_mulai = k.tanggal_mulai,
        tanggal_selesai = k.tanggal_selesai,
        nama_kabupaten = k.nama_kabupaten,
        status_kegiatan = k.status_kegiatan,
        keterangan_wilayah = k.keterangan_wilayah
    )

    /** Cek koneksi */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
