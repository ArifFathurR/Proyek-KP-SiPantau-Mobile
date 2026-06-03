package com.example.sipantau.localData.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.ProgresKeluargaEntity
import com.example.sipantau.model.ProgresKeluarga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class ProgresKeluargaRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).progresKeluargaDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
            return raw?.let { "Bearer $it" }
        }

    /**
     * 🔹 GET DATA
     * Online → ambil dari API lalu replace lokal
     * Offline → ambil dari Room
     */
    suspend fun getProgres(): List<ProgresKeluargaEntity> = withContext(Dispatchers.IO) {

        val local = dao.getAll()

        if (isOnline() && token != null) {
            try {
                val resp = ApiClient.instance.getProgresKeluarga(token!!).awaitResponse()

                if (resp.isSuccessful && resp.body() != null) {

                    val apiList = resp.body()!!.data

                    val entities = apiList.map { apiToEntity(it) }

                    dao.deleteAll()
                    dao.insertAll(entities)

                    return@withContext entities
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext local
    }

    /**
     * 🔹 INSERT DATA
     * Online → kirim ke API
     * Offline → simpan lokal (unsynced)
     */
    suspend fun insertProgres(
        entity: ProgresKeluargaEntity
    ): Pair<Boolean, Long> = withContext(Dispatchers.IO) {

        try {
            if (isOnline() && token != null) {

                val resp = ApiClient.instance.createProgresKeluarga(
                    token!!,
                    entity.jumlah_realisasi_absolut,
                    entity.catatan_aktivitas,
                    entity.id_subsls
                ).awaitResponse()

                if (resp.isSuccessful && resp.body() != null) {

                    val created = resp.body()!!.data

                    val saved = ProgresKeluargaEntity(
                        local_id = 0L,
                        server_id = created.id_pantau_progres_keluarga,
                        jumlah_realisasi_absolut = created.jumlah_realisasi_absolut,
                        jumlah_realisasi_kumulatif = created.jumlah_realisasi_kumulatif,
                        catatan_aktivitas = created.catatan_aktivitas,
                        id_subsls = created.id_subsls ?: entity.id_subsls,
                        created_at = created.created_at,
                        updated_at = created.updated_at,
                        is_synced = true
                    )

                    val localId = dao.insert(saved)
                    return@withContext Pair(true, localId)

                } else {
                    val localSaved = entity.copy(is_synced = false)
                    val localId = dao.insert(localSaved)
                    return@withContext Pair(false, localId)
                }

            } else {
                val localSaved = entity.copy(is_synced = false)
                val localId = dao.insert(localSaved)
                return@withContext Pair(false, localId)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val localSaved = entity.copy(is_synced = false)
            val localId = dao.insert(localSaved)
            return@withContext Pair(false, localId)
        }
    }

    /**
     * 🔹 DELETE DATA
     * - Jika ada server_id & online → delete API
     * - Jika offline → hapus lokal saja
     */
    suspend fun deleteProgres(
        serverId: Int?,
        localId: Long?
    ): Boolean = withContext(Dispatchers.IO) {

        try {
            if (serverId != null && isOnline() && token != null) {

                val resp = ApiClient.instance
                    .deleteProgresKeluarga(token!!, serverId)
                    .awaitResponse()

                dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)

                return@withContext resp.isSuccessful

            } else {
                dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
                return@withContext true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
            return@withContext false
        }
    }

    /**
     * 🔹 SYNC DATA (PENTING)
     * Kirim semua data is_synced = false ke server
     */
    suspend fun syncPending(): Int = withContext(Dispatchers.IO) {

        if (!isOnline() || token == null) return@withContext 0

        val pending = dao.getUnsynced()
        var successCount = 0

        for (p in pending) {
            try {

                val resp = ApiClient.instance.createProgresKeluarga(
                    token!!,
                    p.jumlah_realisasi_absolut,
                    p.catatan_aktivitas,
                    p.id_subsls
                ).awaitResponse()

                if (resp.isSuccessful && resp.body() != null) {

                    val created = resp.body()!!.data

                    val updated = p.copy(
                        server_id = created.id_pantau_progres_keluarga,
                        jumlah_realisasi_kumulatif = created.jumlah_realisasi_kumulatif,
                        id_subsls = created.id_subsls ?: p.id_subsls,
                        created_at = created.created_at,
                        updated_at = created.updated_at,
                        is_synced = true
                    )

                    dao.update(updated)
                    successCount++
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext successCount
    }

    /**
     * 🔹 MAPPING API → ENTITY
     */
    private fun apiToEntity(p: ProgresKeluarga) =
        ProgresKeluargaEntity(
            local_id = 0L,
            server_id = p.id_pantau_progres_keluarga,
            jumlah_realisasi_absolut = p.jumlah_realisasi_absolut,
            jumlah_realisasi_kumulatif = p.jumlah_realisasi_kumulatif,
            catatan_aktivitas = p.catatan_aktivitas,
            id_subsls = p.id_subsls,
            created_at = p.created_at,
            updated_at = p.updated_at,
            is_synced = true
        )

    /**
     * 🔹 CEK KONEKSI INTERNET
     */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}