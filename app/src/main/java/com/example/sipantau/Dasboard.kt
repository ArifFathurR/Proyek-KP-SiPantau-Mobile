package com.example.sipantau

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardBinding
import com.example.sipantau.localData.entity.DesaLocalEntity
import com.example.sipantau.localData.entity.KecamatanLocalEntity
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.entity.SubslsLocalEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.localData.repository.SubslsRepository
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.ReminderResponse
import com.example.sipantau.model.TotalKegPClResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.notifications.NotificationHelper
import com.example.sipantau.notifications.NotificationScheduler
import com.example.sipantau.repository.WilayahRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Dasboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()
    private var se2026IdPcl: Int? = null

    private val NOTIF_PERMISSION = 101
    private val REQ_10_30 = 1030
    private val PREF_WILAYAH_SYNC = "pref_wilayah_sync"

    private val REQ_16_00 = 1600

    companion object {
        private const val TAG = "Dasboard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        try {
            binding = DashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "Dashboard onCreate started")

            // ====== CEK LOGIN ======
            if (!isUserLoggedIn()) {
                navigateToLogin()

                return
            }
            preloadWilayahOnce()
            preloadSubslsOnce()

            // Buat channel notifikasi
            NotificationHelper.createChannel(this)

            // ✓ Cek permission notifikasi dulu
            checkNotifPermission()

            showLoggedInUserName()

            // ====== SETUP ADAPTER ======
            kegiatanAdapter = KegiatanAdapter(
                emptyList(),
                onItemClick = { kegiatan ->
                    // Click pada card - navigasi ke PantauAktivitas
                    val idPcl = kegiatan.id_pcl
                    val idKegiatanDetailProses = kegiatan.id_kegiatan_detail_proses
                    if (idPcl != null) {
                        val intent = Intent(this, PantauAktivitas::class.java)
                        intent.putExtra("id_pcl", idPcl)
                        intent.putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "ID PCL tidak ditemukan pada kegiatan ini", Toast.LENGTH_SHORT).show()
                    }
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
                layoutManager = LinearLayoutManager(this@Dasboard)
                adapter = kegiatanAdapter
            }

            binding.root.post { loadKegiatan() }

            // ====== BUTTON LISTENER ======
            binding.gambarProfil.setOnClickListener {
                Log.d(TAG, "Profile picture clicked")
                binding.bottomNavigationView.selectedItemId = R.id.nav_profile
            }

            binding.btnPantauAktivitas.setOnClickListener {
                startActivity(Intent(this, KegiatanSaya::class.java))
            }

            binding.btnPantauProgress.setOnClickListener {
                startActivity(Intent(this, ProgresKegiatanSaya::class.java))
            }

            setSE2026MenuVisible(false)

            binding.btnKinerjaHarian.setOnClickListener {
                startActivity(Intent(this, KinerjaHarian::class.java))
            }

            binding.btnFeedback.setOnClickListener {
                startActivity(Intent(this, FeedbackUser::class.java))
            }

            binding.btnAchievement.setOnClickListener {
                startActivity(Intent(this, Achievement::class.java))
            }
            binding.btnIndustriDigital.setOnClickListener {
                startActivity(Intent( this, ProgresIndustriDigital::class.java))
            }
            binding.btnLaporKeluarga.setOnClickListener {
                val intent = Intent(this, ProgresKeluarga::class.java).apply {
                    se2026IdPcl?.let { putExtra("id_pcl", it) }
                }
                startActivity(intent)
            }
            binding.btnLaporPertanian.setOnClickListener {
                val intent = Intent(this, ProgresPertanian::class.java).apply {
                    se2026IdPcl?.let { putExtra("id_pcl", it) }
                }
                startActivity(intent)
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

            loadTotalKegiatanPcl()

            // ====== SETUP BOTTOM NAVIGATION ======
            setupBottomNavigation()

            Log.d(TAG, "Dashboard onCreate completed successfully")

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
            loadKegiatan()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dashboard: ${e.message}", e)
        }
    }

    // ====================================================================
    // NOTIFIKASI & INTEGRASI API REMINDER

    private fun checkNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIF_PERMISSION
                )
            } else {
                fetchReminderAndSchedule()
            }
        } else {
            fetchReminderAndSchedule()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIF_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchReminderAndSchedule()
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
                NotificationScheduler.cancelScheduledNotification(this, REQ_10_30)
                NotificationScheduler.cancelScheduledNotification(this, REQ_16_00)
            }
        }
    }

    private fun fetchReminderAndSchedule() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        ApiClient.instance.getReminderStatus("Bearer $token")
            .enqueue(object : Callback<ReminderResponse> {
                override fun onResponse(call: Call<ReminderResponse>, response: Response<ReminderResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!.data

                        var perluTransaksi = false
                        var perluProgress = false
                        var targetHarian = 0

                        for (item in data) {
                            if (!item.sudah_transaksi) perluTransaksi = true
                            if (!item.sudah_progress) perluProgress = true
                            if (item.target_harian > targetHarian) targetHarian = item.target_harian
                        }

                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)

                        if (perluTransaksi) {
                            val msg = "Jangan lupa melakukan pelaporan hari ini.\nTarget harian: $targetHarian"
                            NotificationScheduler.scheduleDailyNotification(
                                this@Dasboard,
                                10,
                                0,
                                msg,
                                REQ_10_30
                            )
                        }

                        if (perluProgress) {
                            val msg = "Jangan lupa melaporkan progress hari ini.\nTarget harian: $targetHarian"
                            NotificationScheduler.scheduleDailyNotification(
                                this@Dasboard,
                                16,
                                0,
                                msg,
                                REQ_16_00
                            )
                        }
                    } else {
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)
                    }
                }

                override fun onFailure(call: Call<ReminderResponse>, t: Throwable) {
                    NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                    NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)
                }
            })
    }

    // ====================================================================

    private fun loadKegiatan() {
        val repo = KegiatanRepository(this)

        lifecycleScope.launch {
            try {
                val data: List<KegiatanEntity> = repo.getKegiatan()

                listAktif = data.filter { it.status_kegiatan == "aktif" }.map { it.toKegiatanModel() }
                listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }.map { it.toKegiatanModel() }

                kegiatanAdapter.updateData(listAktif)
                checkSE2026(data.mapNotNull { it.id_pcl }.distinct())

                if (data.isEmpty() && !isOnline()) {
                    Toast.makeText(
                        this@Dasboard,
                        "⚠️ Kamu sedang offline",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading kegiatan: ${e.message}", e)
            }
        }
    }

    private fun setSE2026MenuVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnLaporKeluarga.visibility = visibility
        binding.btnIndustriDigital.visibility = visibility
        binding.btnLaporPertanian.visibility = visibility
        if (!visible) se2026IdPcl = null
    }

    private fun checkSE2026(idPclList: List<Int>) {
        if (idPclList.isEmpty()) {
            setSE2026MenuVisible(false)
            return
        }

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return
        val bearerToken = "Bearer $token"

        lifecycleScope.launch(Dispatchers.IO) {
            var hasSE2026Access = false
            var foundIdPcl: Int? = null
            var apiCheckSuccessful = false

            for (idPcl in idPclList) {
                try {
                    val resp = ApiClient.instance.checkSe2026(bearerToken, idPcl).execute()
                    if (resp.isSuccessful && resp.body() != null) {
                        apiCheckSuccessful = true
                        if (resp.body()!!.is_se2026) {
                            hasSE2026Access = true
                            foundIdPcl = idPcl
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (apiCheckSuccessful) {
                // Berhasil terhubung ke API: Simpan status kelayakan terbaru ke SharedPreferences
                prefs.edit().apply {
                    putBoolean("has_se2026_access", hasSE2026Access)
                    if (hasSE2026Access && foundIdPcl != null) {
                        putInt("se2026_id_pcl", foundIdPcl)
                    } else {
                        remove("se2026_id_pcl")
                    }
                    apply()
                }
            } else {
                // Gagal menghubungi API (Offline): Ambil data dari cache local SharedPreferences
                hasSE2026Access = prefs.getBoolean("has_se2026_access", false)
                val cachedIdPcl = prefs.getInt("se2026_id_pcl", -1)
                if (cachedIdPcl != -1) {
                    foundIdPcl = cachedIdPcl
                }
            }

            withContext(Dispatchers.Main) {
                se2026IdPcl = foundIdPcl
                setSE2026MenuVisible(hasSE2026Access)
            }
        }
    }

    private fun KegiatanEntity.toKegiatanModel() = Kegiatan(
        id_pcl = this.id_pcl,
        id_pml = this.id_pml,
        total_realisasi_kumulatif = this.total_realisasi_kumulatif,
        id_kegiatan_detail_proses = this.id_kegiatan_detail_proses,
        target = this.target,
        status_approval = this.status_approval,
        nama_kegiatan = this.nama_kegiatan,
        nama_kegiatan_detail_proses = this.nama_kegiatan_detail_proses,
        tanggal_mulai = this.tanggal_mulai,
        tanggal_selesai = this.tanggal_selesai,
        nama_kabupaten = this.nama_kabupaten,
        status_kegiatan = this.status_kegiatan,
        keterangan_wilayah = this.keterangan_wilayah
    )

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
            val user = Gson().fromJson(userJson, UserData::class.java)
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

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadTotalKegiatanPcl() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        if (token.isNullOrEmpty()) return

        ApiClient.instance.getTotalKegPcl("Bearer $token")
            .enqueue(object : retrofit2.Callback<TotalKegPClResponse> {

                override fun onResponse(
                    call: retrofit2.Call<TotalKegPClResponse>,
                    response: retrofit2.Response<TotalKegPClResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val res = response.body()!!

                        // total kegiatan pcl
                        binding.jmlKeg.text = res.total_kegiatan_pcl.toString()

                        // total kegiatan aktif
                        binding.ratingKepatuhan.text = res.total_kegiatan_pcl_aktif.toString()

                        // 🔥 CETAK TOTAL ACHIEVEMENT (contoh: 5/18)
                        binding.rating.text = "${res.total_achievement}/18"

                    } else {
                        binding.jmlKeg.text = "0"
                        binding.ratingKepatuhan.text = "0"
                        binding.rating.text = "0/18"
                    }
                }

                override fun onFailure(call: retrofit2.Call<TotalKegPClResponse>, t: Throwable) {
                    binding.jmlKeg.text = "0"
                    binding.ratingKepatuhan.text = "0"
                    binding.rating.text = "0/18"
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

                val repo = WilayahRepository(this@Dasboard)

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
                val db = com.example.sipantau.localData.AppDatabase.getDatabase(this@Dasboard)
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
                val subslsRepo = SubslsRepository(this@Dasboard)
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