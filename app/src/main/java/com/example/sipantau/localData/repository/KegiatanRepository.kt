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

    // aktif role yg disimpan saat login / pemilihan role
    private val activeRole: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(LoginActivity.PREF_ACTIVE_ROLE, null)
        }

    /** Load kegiatan offline-online */
    suspend fun getKegiatan(): List<KegiatanEntity> = withContext(Dispatchers.IO) {
        val localData = dao.getAll() // data lokal

        if (isOnline()) {
            try {
                val response = ApiClient.instance.getKegiatan(token ?: "").awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // pilih list sesuai role aktif
                    val kegiatanList: List<Kegiatan> = when (activeRole) {
                        "PCL" -> body.kegiatan_pcl
                        "PML" -> body.kegiatan_pml
                        else -> {
                            // jika role null: jika API mengembalikan roles, pakai first role
                            val fallback = body.roles.firstOrNull()
                            when (fallback) {
                                "PCL" -> body.kegiatan_pcl
                                "PML" -> body.kegiatan_pml
                                else -> emptyList()
                            }
                        }
                    }

                    val entityList = kegiatanList.mapNotNull { toEntitySafe(it) }

                    // simpan cache
                    dao.deleteAll()
                    dao.insertAll(entityList)

                    return@withContext entityList
                }
            } catch (e: Exception) {
                // fallback ke lokal
            }
        }

        return@withContext localData
    }

    private fun toEntity(k: Kegiatan) = KegiatanEntity(
        id_kegiatan_detail_proses = k.id_kegiatan_detail_proses ?: (k.id_pcl ?: k.id_pml ?: 0),
        id_pcl = k.id_pcl,
        id_pml = k.id_pml,
        total_realisasi_kumulatif= k.total_realisasi_kumulatif,
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

    // safety: skip items yang tidak punya id sama sekali (menghindari primaryKey=0 duplikat)
    private fun toEntitySafe(k: Kegiatan): KegiatanEntity? {
        val pk = k.id_kegiatan_detail_proses ?: (k.id_pcl ?: k.id_pml ?: 0)
        if (pk == 0) return null
        return toEntity(k)
    }

    /** Cek koneksi */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
