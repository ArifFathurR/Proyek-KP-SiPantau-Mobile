package com.example.sipantau.localData.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.localData.AppDatabase
import com.example.sipantau.localData.entity.SubslsLocalEntity
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubslsRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).subslsDao()

    private val token: String?
        get() {
            val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
            return raw?.let { "Bearer $it" }
        }

    /**
     * Ambil Sub-SLS dengan filter opsional.
     * - Online  → fetch dari API, cache semua ke Room, lalu filter dari Room
     * - Offline → langsung baca dari Room dengan filter yang sama
     */
    suspend fun getSubsls(
        idDesa: Int? = null,
        idKecamatan: Int? = null,
        idPcl: Int? = null
    ): List<SubslsLocalEntity> = withContext(Dispatchers.IO) {

        val online = isOnline()
        Log.d("SubslsRepository", "getSubsls called. isOnline: $online, token: ${token != null}, idDesa: $idDesa, idKecamatan: $idKecamatan, idPcl: $idPcl")

        if (online && token != null) {
            try {
                val resp = ApiClient.instance
                    .getSubsls(token!!, idDesa = idDesa, idKecamatan = idKecamatan, idPcl = idPcl)
                    .execute()

                if (resp.isSuccessful && resp.body() != null) {
                    val apiList = resp.body()!!.data.map { s ->
                        SubslsLocalEntity(
                            id_subsls     = s.id_subsls,
                            nama_subsls   = s.nama_subsls,
                            id_desa       = s.id_desa ?: idDesa,
                            nama_desa     = s.nama_desa,
                            id_kecamatan  = s.id_kecamatan ?: idKecamatan,
                            id_pcl        = s.id_pcl ?: idPcl,
                            nama_kecamatan = s.nama_kecamatan
                        )
                    }

                    // Sisipkan ke Room (upsert per id_subsls karena PRIMARY KEY)
                    dao.insertAll(apiList)
                    Log.d("SubslsRepository", "Fetched from API and saved to Room. Size: ${apiList.size}")

                    return@withContext apiList
                } else {
                    Log.e("SubslsRepository", "API Error: ${resp.code()} - ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SubslsRepository", "API Exception", e)
            }
        }

        // Fallback: ambil dari Room dengan filter
        val allCount = dao.getAll().size
        val filteredList = dao.getFiltered(idDesa, idKecamatan, idPcl)
        Log.d("SubslsRepository", "Room fallback. Total in DB: $allCount, Filtered size: ${filteredList.size}")
        filteredList
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
