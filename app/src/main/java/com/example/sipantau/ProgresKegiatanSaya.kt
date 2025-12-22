package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.databinding.KegiatanSayaBinding
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.model.Kegiatan
import kotlinx.coroutines.launch

class ProgresKegiatanSaya : AppCompatActivity() {

    private lateinit var binding: KegiatanSayaBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter

    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    // 🔥 STATE TAB
    private var currentTab: TabType = TabType.AKTIF

    enum class TabType {
        AKTIF,
        TIDAK_AKTIF
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KegiatanSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.judul.text = "Pantau Progress"

        // ================= Adapter =================
        kegiatanAdapter = KegiatanAdapter(
            emptyList(),

            // ================= CARD CLICK =================
            onItemClick = { kegiatan ->
                val intent = Intent(this, PantauProgres::class.java).apply {
                    putExtra("id_pcl", kegiatan.id_pcl ?: 0)
                    putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses ?: 0)
                    putExtra("target", kegiatan.target ?: 0)
                    putExtra(
                        "total_realisasi_kumulatif",
                        kegiatan.total_realisasi_kumulatif ?: 0
                    )
                }
                startActivity(intent)
            },

            // ================= DETAIL CLICK =================
            onDetailClick = { kegiatan ->
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
                    putExtra(
                        "total_realisasi_kumulatif",
                        kegiatan.total_realisasi_kumulatif ?: 0
                    )
                    putExtra("id_pcl", kegiatan.id_pcl ?: 0)
                    putExtra("id_pml", kegiatan.id_pml ?: 0)
                    putExtra(
                        "id_kegiatan_detail_proses",
                        kegiatan.id_kegiatan_detail_proses ?: 0
                    )
                }
                startActivity(intent)
            }
        )

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@ProgresKegiatanSaya)
            adapter = kegiatanAdapter
        }

        // ================= TAB AKTIF =================
        binding.tabAktif.setOnClickListener {
            currentTab = TabType.AKTIF
            kegiatanAdapter.updateData(listAktif)
            setTabActive(isAktif = true)
        }

        // ================= TAB TIDAK AKTIF =================
        binding.tabTidakAktif.setOnClickListener {
            currentTab = TabType.TIDAK_AKTIF
            kegiatanAdapter.updateData(listTidakAktif)
            setTabActive(isAktif = false)
        }

        binding.btnKembali.setOnClickListener { finish() }

        // ================= SWIPE REFRESH =================
        binding.swipeRefresh.setOnRefreshListener {
            loadKegiatan()
        }

        // ================= LOAD AWAL =================
        loadKegiatan()
    }

    // ================= LOAD DATA =================
    private fun loadKegiatan() {
        val repo = KegiatanRepository(this)
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val data = repo.getKegiatan()

                listAktif = data
                    .filter { it.status_kegiatan == "aktif" }
                    .map { it.toKegiatanModel() }

                listTidakAktif = data
                    .filter { it.status_kegiatan == "tidak aktif" }
                    .map { it.toKegiatanModel() }

                // 🔥 TAMPILKAN SESUAI TAB AKTIF
                when (currentTab) {
                    TabType.AKTIF -> {
                        kegiatanAdapter.updateData(listAktif)
                        setTabActive(isAktif = true)
                    }
                    TabType.TIDAK_AKTIF -> {
                        kegiatanAdapter.updateData(listTidakAktif)
                        setTabActive(isAktif = false)
                    }
                }

                if (data.isEmpty() && !isOnline()) {
                    Toast.makeText(
                        this@ProgresKegiatanSaya,
                        "⚠️ Kamu sedang offline dan data lokal kosong",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ProgresKegiatanSaya,
                    "Gagal memuat kegiatan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ================= TAB UI =================
    private fun setTabActive(isAktif: Boolean) {
        binding.tabAktif.setCardBackgroundColor(
            if (isAktif) Color.parseColor("#B3D9FF") else Color.TRANSPARENT
        )
        binding.tabTidakAktif.setCardBackgroundColor(
            if (!isAktif) Color.parseColor("#B3D9FF") else Color.TRANSPARENT
        )
    }

    // ================= MAPPER =================
    private fun KegiatanEntity.toKegiatanModel() = Kegiatan(
        id_pcl = id_pcl,
        id_pml = id_pml,
        id_kegiatan_detail_proses = id_kegiatan_detail_proses,
        nama_kegiatan = nama_kegiatan,
        nama_kegiatan_detail_proses = nama_kegiatan_detail_proses,
        tanggal_mulai = tanggal_mulai,
        tanggal_selesai = tanggal_selesai,
        nama_kabupaten = nama_kabupaten,
        keterangan_wilayah = keterangan_wilayah,
        status_kegiatan = status_kegiatan,
        status_approval = status_approval,
        target = target,
        total_realisasi_kumulatif = total_realisasi_kumulatif
    )

    // ================= NETWORK =================
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
