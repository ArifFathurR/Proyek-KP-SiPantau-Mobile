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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardBinding
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.ReminderItem
import com.example.sipantau.model.ReminderResponse
import com.example.sipantau.model.TotalKegPClResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.notifications.NotificationHelper
import com.example.sipantau.notifications.NotificationScheduler
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Dasboard : AppCompatActivity() {

    private lateinit var binding: DashboardBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    private val NOTIF_PERMISSION = 101   // permission request code
    // request codes for alarms (unique)
    private val REQ_10_30 = 1030
    private val REQ_16_00 = 1600

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ====== CEK LOGIN ======
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        // Buat channel notifikasi
        NotificationHelper.createChannel(this)

        // ✓ Cek permission notifikasi dulu (jika sudah diberi izin -> schedule)
        checkNotifPermission()

        showLoggedInUserName()

        // ====== SETUP ADAPTER ======
        kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
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
        }

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@Dasboard)
            adapter = kegiatanAdapter
        }

        binding.root.post { loadKegiatan() }

        // ====== BUTTON LISTENER ======
        binding.gambarProfil.setOnClickListener { logoutUser() }

        binding.btnPantauAktivitas.setOnClickListener {
            startActivity(Intent(this, KegiatanSaya::class.java))
        }

        binding.btnPantauProgress.setOnClickListener {
            startActivity(Intent(this, ProgresKegiatanSaya::class.java))
        }

        binding.btnKinerjaHarian.setOnClickListener {
            startActivity(Intent(this, KinerjaHarian::class.java))
        }

        binding.btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackUser::class.java))
        }
        binding.btnAchievement.setOnClickListener {
            startActivity(Intent(this, Achievement::class.java))
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
                // Kalau sudah diberi izin, panggil API dan schedule sesuai hasilnya
                fetchReminderAndSchedule()
            }
        } else {
            // Android < 13: langsung panggil API & schedule
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
                // jika ditolak, batalkan scheduling jika ada
                NotificationScheduler.cancelScheduledNotification(this, REQ_10_30)
                NotificationScheduler.cancelScheduledNotification(this, REQ_16_00)
            }
        }
    }

    /**
     * Panggil API /cek/ lalu schedule alarm harian 10:30 & 16:00 bila perlu.
     * Pesan notifikasi akan memuat target_harian.
     */
    private fun fetchReminderAndSchedule() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        ApiClient.instance.getReminderStatus("Bearer $token")
            .enqueue(object : Callback<ReminderResponse> {
                override fun onResponse(call: Call<ReminderResponse>, response: Response<ReminderResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!.data

                        // Tentukan apakah ada PCL yang belum transaksi / belum progress
                        var perluTransaksi = false
                        var perluProgress = false
                        var targetHarian = 0

                        for (item in data) {
                            if (!item.sudah_transaksi) perluTransaksi = true
                            if (!item.sudah_progress) perluProgress = true
                            if (item.target_harian > targetHarian) targetHarian = item.target_harian
                        }

                        // Cancel schedule dulu agar tidak duplikasi pesan (user bisa menutup & buka lagi)
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)

                        if (perluTransaksi) {
                            val msg = "Jangan lupa melakukan pelaporan hari ini.\nTarget harian: $targetHarian"
                            NotificationScheduler.scheduleDailyNotification(
                                this@Dasboard,
                                19,
                                3,
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
                        // jika API gagal, batalkan schedule yang ada supaya tidak kirim pesan salah
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                        NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)
                    }
                }

                override fun onFailure(call: Call<ReminderResponse>, t: Throwable) {
                    // gagal network -> jangan schedule agar tidak terjadi notifikasi keliru
                    NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_10_30)
                    NotificationScheduler.cancelScheduledNotification(this@Dasboard, REQ_16_00)
                }
            })
    }

    // ====================================================================

    private fun loadKegiatan() {
        val repo = KegiatanRepository(this)

        lifecycleScope.launch {
            val data: List<KegiatanEntity> = repo.getKegiatan()

            listAktif = data.filter { it.status_kegiatan == "aktif" }.map { it.toKegiatanModel() }
            listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }.map { it.toKegiatanModel() }

            kegiatanAdapter.updateData(listAktif)

            if (data.isEmpty() && !isOnline()) {
                Toast.makeText(
                    this@Dasboard,
                    "⚠️ Kamu sedang offline",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun KegiatanEntity.toKegiatanModel() = Kegiatan(
        id_pcl = this.id_pcl,
        id_pml = this.id_pml,
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

    private fun logoutUser() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(LoginActivity.PREF_TOKEN)
            .remove(LoginActivity.PREF_USER)
            .apply()
        navigateToLogin()
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

        ApiClient.instance.getTotalKegPcl("Bearer $token").enqueue(object :
            retrofit2.Callback<TotalKegPClResponse> {
            override fun onResponse(
                call: retrofit2.Call<TotalKegPClResponse>,
                response: retrofit2.Response<TotalKegPClResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val total = response.body()!!.total_kegiatan_pcl
                    binding.jmlKeg.text = total.toString()
                } else {
                    binding.jmlKeg.text = "0"
                }
            }

            override fun onFailure(call: retrofit2.Call<TotalKegPClResponse>, t: Throwable) {
                binding.jmlKeg.text = "0"
            }
        })
    }
}
