package com.example.sipantau.localData.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.PantauProgresEntity
import com.example.sipantau.model.PantauProgres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.awaitResponse

class PantauProgresRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).pantauProgresDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
            return raw?.let { "Bearer $it" }
        }

    /** Ambil progres: jika online ambil dari API dan update lokal, kalau tidak fallback ke lokal */
    suspend fun getProgres(idPcl: Int): List<PantauProgresEntity> = withContext(Dispatchers.IO) {
        val local = dao.getAllByPcl(idPcl)
        if (isOnline() && token != null) {
            try {
                val resp = ApiClient.instance.getProgres(token!!, idPcl).awaitResponse()
                if (resp.isSuccessful && resp.body() != null) {
                    val apiList = resp.body()!!.data
                    // Convert API models -> Entities (server_id filled)
                    val entities = apiList.map { apiToEntity(it) }
                    // replace local with server data
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

    /** Insert progres:
     *  - jika online: kirim ke API, bila sukses simpan hasil server ke DB (is_synced=true)
     *  - jika offline atau gagal: simpan lokal sebagai is_synced=false
     *  Mengembalikan Pair(success: Boolean, localId: Long)
     */
    suspend fun insertProgres(entity: PantauProgresEntity): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        try {
            if (isOnline() && token != null) {
                // prepare RequestBody
                val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), entity.id_pcl.toString())
                val jumlahBody = RequestBody.create("text/plain".toMediaTypeOrNull(), entity.jumlah_realisasi_absolut.toString())
                val catatanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), entity.catatan_aktivitas)

                val resp = ApiClient.instance.createProgres(token!!, idPclBody, jumlahBody, catatanBody).awaitResponse()
                if (resp.isSuccessful && resp.body() != null) {
                    // server returns created data in resp.body()?.data
                    val created = resp.body()!!.data
                    // convert created -> entity with server_id and is_synced true
                    val saved = PantauProgresEntity(
                        local_id = 0L,
                        server_id = created.id_pantau_progess,
                        id_pcl = created.id_pcl ?: entity.id_pcl,
                        jumlah_realisasi_absolut = created.jumlah_realisasi_absolut ?: entity.jumlah_realisasi_absolut,
                        jumlah_realisasi_kumulatif = created.jumlah_realisasi_kumulatif,
                        catatan_aktivitas = created.catatan_aktivitas,
                        created_at = created.created_at,
                        is_synced = true
                    )
                    val localId = dao.insert(saved)
                    return@withContext Pair(true, localId)
                } else {
                    // gagal server: simpan lokal sebagai unsynced
                    val localSaved = entity.copy(is_synced = false)
                    val localId = dao.insert(localSaved)
                    return@withContext Pair(false, localId)
                }
            } else {
                // offline -> simpan lokal unsynced
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

    /** Hapus progres.
     *  - jika serverId ada dan online -> panggil API delete, lalu hapus lokal
     *  - jika offline atau serverId null -> hapus lokal menggunakan localId (or serverId)
     *  Mengembalikan Boolean success (server) atau true jika lokal dihapus
     */
    suspend fun deleteProgresByServerOrLocal(serverId: Int?, localId: Long?): Boolean = withContext(Dispatchers.IO) {
        try {
            if (serverId != null && isOnline() && token != null) {
                val resp = ApiClient.instance.hapusProgres(token!!, serverId).awaitResponse()
                if (resp.isSuccessful) {
                    dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
                    return@withContext true
                } else {
                    // tetap hapus lokal
                    dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
                    return@withContext false
                }
            } else {
                // offline: hapus lokal by localId or serverId
                dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dao.deleteByServerIdOrLocalId(serverId, localId ?: -1L)
            return@withContext false
        }
    }

    /** Sinkronisasi: kirim semua entri yang is_synced = false */
    suspend fun syncPending(): Int = withContext(Dispatchers.IO) {
        if (!isOnline() || token == null) return@withContext 0
        val pending = dao.getUnsynced()
        var successCount = 0
        for (p in pending) {
            try {
                val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.id_pcl.toString())
                val jumlahBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.jumlah_realisasi_absolut.toString())
                val catatanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), p.catatan_aktivitas)

                val resp = ApiClient.instance.createProgres(token!!, idPclBody, jumlahBody, catatanBody).awaitResponse()
                if (resp.isSuccessful && resp.body() != null) {
                    val created = resp.body()!!.data
                    // update local row with server_id and mark synced
                    val updated = p.copy(server_id = created.id_pantau_progess, is_synced = true, created_at = created.created_at)
                    dao.update(updated)
                    successCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext successCount
    }

    private fun apiToEntity(p: PantauProgres) = PantauProgresEntity(
        local_id = 0L,
        server_id = p.id_pantau_progess,
        id_pcl = p.id_pcl ?: 0,
        jumlah_realisasi_absolut = p.jumlah_realisasi_absolut ?: 0,
        jumlah_realisasi_kumulatif = p.jumlah_realisasi_kumulatif,
        catatan_aktivitas = p.catatan_aktivitas,
        created_at = p.created_at,
        is_synced = true
    )

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
