package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardPmlBinding
import com.example.sipantau.localData.entity.DesaLocalEntity
import com.example.sipantau.localData.entity.KecamatanLocalEntity
import com.example.sipantau.localData.repository.SubslsRepository
import com.example.sipantau.model.Feedback
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.TotalKegPClResponse
import com.example.sipantau.model.TotalKegPMlResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.repository.WilayahRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardPML : AppCompatActivity() {

    private lateinit var binding: DashboardPmlBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()
    private val PREF_WILAYAH_SYNC = "pref_wilayah_sync"

    companion object {
        private const val TAG = "DashboardPML"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = DashboardPmlBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "DashboardPML onCreate started")

            // ====== CEK LOGIN ======
            if (!isUserLoggedIn()) {
                navigateToLogin()
                return
            }

            showLoggedInUserName()

            // ====== SETUP ADAPTER ======
            kegiatanAdapter = KegiatanAdapter(
                emptyList(),
                onItemClick = { kegiatan ->
                    // Click pada card - navigasi ke PantauAktivitasPML
                    val intent = Intent(this, PantauAktivitasPML::class.java)
                    intent.putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses)
                    intent.putExtra("id_pml", kegiatan.id_pml)
                    startActivity(intent)
                },
                onDetailClick = { kegiatan ->
                    // Click pada button detail - navigasi ke DetailKegiatan
                    val intent = Intent(this, DetailKegiatan::class.java).apply {
                        putExtra("nama_kegiatan", kegiatan.nama_kegiatan)
                        putExtra("nama_kegiatan_detail_proses", kegiatan.nama_kegiatan_detail_proses)
                        putExtra("tanggal_mulai", kegiatan.tanggal_mulai)
                        putExtra("tanggal_selesai", kegiatan.tanggal_selesai)
                        putExtra("keterangan_wilayah", kegiatan.keterangan_wilayah)
                        putExtra("nama_kabupaten", kegiatan.nama_kabupaten)
                        putExtra("status_kegiatan", kegiatan.status_kegiatan)
                        putExtra("status_approval", kegiatan.status_approval)
                        putExtra("target", kegiatan.target ?: 0)
                        putExtra("id_pcl", kegiatan.id_pcl ?: 0)
                        putExtra("id_pml", kegiatan.id_pml ?: 0)
                        putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses ?: 0)
                    }
                    startActivity(intent)
                }
            )

            binding.rvKegiatan.apply {
                layoutManager = LinearLayoutManager(this@DashboardPML)
                adapter = kegiatanAdapter
            }

            loadKegiatanFromAPI()

            // ====== BUTTON LISTENER ======
            binding.gambarProfil.setOnClickListener {
                Log.d(TAG, "Profile picture clicked")
                binding.bottomNavigationView.selectedItemId = R.id.nav_profile
            }

            binding.tabAktif.setOnClickListener {
                kegiatanAdapter.updateData(listAktif)
                binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
                binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)

                if (listAktif.isEmpty()) {
                    Toast.makeText(this, "Sedang tidak ada data kegiatan aktif", Toast.LENGTH_SHORT).show()
                }
            }

            binding.tabTidakAktif.setOnClickListener {
                kegiatanAdapter.updateData(listTidakAktif)
                binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
                binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)

                if (listTidakAktif.isEmpty()) {
                    Toast.makeText(this, "Sedang tidak ada data kegiatan tidak aktif", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnPantauAktivitas.setOnClickListener {
                startActivity(Intent(this, PantauAktivitasPML::class.java))
            }

            binding.btnFeedback.setOnClickListener {
                startActivity(Intent(this, FeedbackUser::class.java))
            }

            binding.btnAchievement.setOnClickListener {
                startActivity(Intent(this, PantauAktivitasPML::class.java))
            }
            binding.btnLaporAktivitas.setOnClickListener {
                startActivity(Intent(this, KegiatanSayaPml::class.java))
            }

            loadTotalKegiatanPml()
            preloadWilayahOnce()
            preloadSubslsOnce()

            // ====== SETUP BOTTOM NAVIGATION ======
            setupBottomNavigation()

            Log.d(TAG, "DashboardPML onCreate completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ====================================================================
    // BOTTOM NAVIGATION HANDLER

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigationView.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.nav_home -> {
                            Log.d(TAG, "Home selected")
                            showDashboardContent()
                            true
                        }
                        R.id.nav_profile -> {
                            Log.d(TAG, "Profile selected")
                            loadFragment(Profile())
                            hideDashboardContent()
                            true
                        }
                        R.id.nav_info -> {
                            Log.d(TAG, "Info selected")
                            loadFragment(Info())
                            hideDashboardContent()
                            true
                        }
                        else -> {
                            Log.w(TAG, "Unknown menu item selected")
                            false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in navigation: ${e.message}", e)
                    Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            // Set home as default selected
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            Log.d(TAG, "Bottom navigation setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
            Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            Log.d(TAG, "Loading fragment: ${fragment.javaClass.simpleName}")

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()

            Log.d(TAG, "Fragment loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment: ${e.message}", e)
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideDashboardContent() {
        try {
            Log.d(TAG, "Hiding dashboard content")
            binding.scrollView.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding dashboard: ${e.message}", e)
        }
    }

    private fun showDashboardContent() {
        try {
            Log.d(TAG, "Showing dashboard content")

            // Remove any fragments
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment != null) {
                Log.d(TAG, "Removing current fragment")
                supportFragmentManager.beginTransaction()
                    .remove(currentFragment)
                    .commitAllowingStateLoss()
            }

            binding.scrollView.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            loadKegiatanFromAPI()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dashboard: ${e.message}", e)
        }
    }

    // ====================================================================
    // LOAD KEGIATAN FROM API

    private fun loadKegiatanFromAPI() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val auth = "Bearer $token"

        ApiClient.instance.getKegiatan(auth)
            .enqueue(object : Callback<KegiatanResponse> {
                override fun onResponse(call: Call<KegiatanResponse>, response: Response<KegiatanResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.kegiatan_pml ?: emptyList()

                        listAktif = data.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }

                        kegiatanAdapter.updateData(listAktif)
                    } else {
                        Toast.makeText(this@DashboardPML, "Gagal memuat kegiatan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(this@DashboardPML, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ====================================================================
    // LOGIN HANDLER

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    private fun showLoggedInUserName() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        binding.nama.text = if (userJson != null) {
            val user = Gson().fromJson(userJson, com.example.sipantau.model.UserData::class.java)
            "Halo, ${user.nama_user}"
        } else "Halo, Pengguna"
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ====================================================================
    // LOAD TOTAL KEGIATAN

    private fun loadTotalKegiatanPml() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        if (token.isNullOrEmpty()) return

        ApiClient.instance.getTotalKegPml("Bearer $token")
            .enqueue(object : retrofit2.Callback<TotalKegPMlResponse> {
                override fun onResponse(
                    call: retrofit2.Call<TotalKegPMlResponse>,
                    response: retrofit2.Response<TotalKegPMlResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val total = response.body()!!.total_kegiatan_pml
                        val total_pcl = response.body()!!.total_pcl
                        val total_keg_aktif = response.body()!!.total_kegiatan_pml_aktif
                        binding.jmlKeg.text = total.toString()
                        binding.ratingKepatuhan.text = total_pcl.toString()
                        binding.rating.text = total_keg_aktif.toString()
                    } else {
                        binding.jmlKeg.text = "0"
                    }
                }

                override fun onFailure(call: retrofit2.Call<TotalKegPMlResponse>, t: Throwable) {
                    binding.jmlKeg.text = "0"
                }
            })
    }
    private fun preloadWilayahOnce() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val alreadySynced = prefs.getBoolean(PREF_WILAYAH_SYNC, false)

        if (alreadySynced) {
            Log.d("WILAYAH_SYNC", "⛔ Wilayah sudah pernah disinkron")
            return
        }

        val token = "Bearer ${prefs.getString(LoginActivity.PREF_TOKEN, "")}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("WILAYAH_SYNC", "🚀 Mulai load wilayah dari API")

                val resp = ApiClient.instance.loadAllWilayah(token).execute()

                if (!resp.isSuccessful || resp.body() == null) {
                    Log.e("WILAYAH_SYNC", "❌ API gagal: ${resp.code()}")
                    return@launch
                }

                val data = resp.body()!!

                // 🔍 DEBUG API
                Log.d("WILAYAH_SYNC", "API kecamatan: ${data.kecamatan.data.size}")
                Log.d("WILAYAH_SYNC", "API desa: ${data.kecamatan.data.size}")

                val kecEntities = data.kecamatan.data.map  {
                    KecamatanLocalEntity(
                        id_kecamatan = it.id_kecamatan,
                        id_kabupaten = it.id_kabupaten,
                        nama_kecamatan = it.nama_kecamatan
                    )
                }

                val desaEntities = data.desa.data.map {
                    DesaLocalEntity(
                        id_desa = it.id_desa,
                        id_kecamatan = it.id_kecamatan,
                        nama_desa = it.nama_desa
                    )
                }

                val repo = WilayahRepository(this@DashboardPML)

                // 💾 INSERT KE ROOM
                repo.saveKecamatan(kecEntities)
                repo.saveDesa(desaEntities)

                // 🔎 VALIDASI ROOM
                val kecCount = repo.getKecamatan().size
                val desaCount = repo.getDesaByKecamatan(
                    kecEntities.firstOrNull()?.id_kecamatan ?: -1
                ).size

                Log.d("WILAYAH_SYNC", "ROOM kecamatan tersimpan: $kecCount")
                Log.d("WILAYAH_SYNC", "ROOM contoh desa: $desaCount")

                prefs.edit().putBoolean(PREF_WILAYAH_SYNC, true).apply()

                Log.d("WILAYAH_SYNC", "✅ Sync wilayah SELESAI")

            } catch (e: Exception) {
                Log.e("WILAYAH_SYNC", "🔥 ERROR preload wilayah", e)
            }
        }
    }
    private fun preloadSubslsOnce() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = com.example.sipantau.localData.AppDatabase.getDatabase(this@DashboardPML)
                val subslsDao = db.subslsDao()
                val localCount = subslsDao.getAll().size
                val alreadySynced = prefs.getBoolean("pref_subsls_sync", false)

                if (alreadySynced && localCount > 0) {
                    Log.d("SUBSLS_SYNC", "⛔ Sub-SLS sudah pernah disinkron dan database tidak kosong ($localCount)")
                    return@launch
                }

                val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
                val userJson = prefs.getString(LoginActivity.PREF_USER, null)
                if (token.isNullOrEmpty() || userJson.isNullOrEmpty()) return@launch

                val user = Gson().fromJson(userJson, UserData::class.java)
                val idPclList = user.id_pcl

                if (idPclList.isNullOrEmpty()) {
                    Log.d("SUBSLS_SYNC", "No id_pcl to sync subsls")
                    return@launch
                }

                Log.d("SUBSLS_SYNC", "🚀 Mulai load sub-sls dari API untuk PCL: $idPclList")
                val subslsRepo = SubslsRepository(this@DashboardPML)
                var hasSuccess = false
                for (idPcl in idPclList) {
                    val list = subslsRepo.getSubsls(idPcl = idPcl)
                    if (list.isNotEmpty()) {
                        hasSuccess = true
                    }
                }
                if (hasSuccess) {
                    prefs.edit().putBoolean("pref_subsls_sync", true).apply()
                    Log.d("SUBSLS_SYNC", "✅ Sync sub-sls SELESAI")
                }
            } catch (e: Exception) {
                Log.e("SUBSLS_SYNC", "🔥 ERROR preload sub-sls", e)
            }
        }
    }
}