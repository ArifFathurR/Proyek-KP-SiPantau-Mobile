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

class KegiatanSaya : AppCompatActivity() {

    private lateinit var binding: KegiatanSayaBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter

    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    // 🔥 STATE TAB AKTIF
    private var currentTab: TabType = TabType.AKTIF

    enum class TabType {
        AKTIF,
        TIDAK_AKTIF
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KegiatanSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ================= Adapter =================
        kegiatanAdapter = KegiatanAdapter(
            emptyList(),
            onItemClick = { kegiatan ->
                val idPcl = kegiatan.id_pcl
                val idDetail = kegiatan.id_kegiatan_detail_proses

                if (idPcl != null) {
                    val intent = Intent(this, PantauAktivitas::class.java)
                    intent.putExtra("id_pcl", idPcl)
                    intent.putExtra("id_kegiatan_detail_proses", idDetail)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "ID PCL tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            },
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
                    putExtra("id_pcl", kegiatan.id_pcl ?: 0)
                    putExtra("id_pml", kegiatan.id_pml ?: 0)
                    putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses ?: 0)
                }
                startActivity(intent)
            }
        )

        binding.rvKegiatan.apply {
            layoutManager = LinearLayoutManager(this@KegiatanSaya)
            adapter = kegiatanAdapter
        }

        // ================= TAB AKTIF =================
        binding.tabAktif.setOnClickListener {
            currentTab = TabType.AKTIF
            kegiatanAdapter.updateData(listAktif)

            binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        // ================= TAB TIDAK AKTIF =================
        binding.tabTidakAktif.setOnClickListener {
            currentTab = TabType.TIDAK_AKTIF
            kegiatanAdapter.updateData(listTidakAktif)

            binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        // ================= SWIPE REFRESH =================
        binding.swipeRefresh.setOnRefreshListener {
            loadKegiatan()
        }

        binding.btnKembali.setOnClickListener {
            finish()
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
                val data: List<KegiatanEntity> = repo.getKegiatan()
                val models = data.map { it.toKegiatanModel() }

                listAktif = models.filter { it.status_kegiatan == "aktif" }
                listTidakAktif = models.filter { it.status_kegiatan == "tidak aktif" }

                // 🔥 TAMPILKAN SESUAI TAB YANG AKTIF
                when (currentTab) {
                    TabType.AKTIF -> kegiatanAdapter.updateData(listAktif)
                    TabType.TIDAK_AKTIF -> kegiatanAdapter.updateData(listTidakAktif)
                }

                if (data.isEmpty() && !isOnline()) {
                    Toast.makeText(
                        this@KegiatanSaya,
                        "⚠️ Kamu sedang offline. Data lokal kosong.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@KegiatanSaya,
                    "Gagal memuat kegiatan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ================= ENTITY → MODEL =================
    private fun KegiatanEntity.toKegiatanModel() = Kegiatan(
        id_pcl = id_pcl,
        id_pml = id_pml,
        total_realisasi_kumulatif = total_realisasi_kumulatif,
        id_kegiatan_detail_proses = id_kegiatan_detail_proses,
        target = target,
        status_approval = status_approval,
        nama_kegiatan = nama_kegiatan,
        nama_kegiatan_detail_proses = nama_kegiatan_detail_proses,
        tanggal_mulai = tanggal_mulai,
        tanggal_selesai = tanggal_selesai,
        nama_kabupaten = nama_kabupaten,
        status_kegiatan = status_kegiatan,
        keterangan_wilayah = keterangan_wilayah
    )

    // ================= CEK INTERNET =================
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
