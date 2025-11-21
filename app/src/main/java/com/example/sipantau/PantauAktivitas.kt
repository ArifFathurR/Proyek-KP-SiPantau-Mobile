package com.example.sipantau

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PelaporanAdapter
import com.example.sipantau.databinding.ActivityMainBinding
import com.example.sipantau.localData.entity.PendingLaporanEntity
import com.example.sipantau.model.DisplayLaporan
import com.example.sipantau.repository.LaporanRepository
import com.example.sipantau.utils.NetworkUtil
import com.example.sipantau.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PantauAktivitas : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PelaporanAdapter
    private lateinit var repo: LaporanRepository
    private var idPcl: Int = 0
    private var idKegDetail: Int = 0

    private var currentTab = "pending" // "pending" or "terkirim"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = LaporanRepository(this)
        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegDetail = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        adapter = PelaporanAdapter(
            emptyList(),
            onDeleteClick = { item -> deleteItem(item) },
            onSendClick = { item -> sendPending(item) }
        )

        binding.recylerView.layoutManager = LinearLayoutManager(this)
        binding.recylerView.adapter = adapter

        binding.btnTambah.setOnClickListener {
            val i = Intent(this, TambahLaporan::class.java)
            i.putExtra("id_pcl", idPcl)
            i.putExtra("id_kegiatan_detail_proses", idKegDetail)
            startActivity(i)
        }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch { loadData() }
        }

        setupTabs()
        setActiveTab("pending")

        // schedule background sync
        WorkScheduler.scheduleSync(applicationContext)

        lifecycleScope.launch { loadData() }
    }

    private fun setupTabs() {
        binding.tabPending.setOnClickListener {
            setActiveTab("pending")
            lifecycleScope.launch { loadData() }
        }

        binding.tabTerkirim.setOnClickListener {
            setActiveTab("terkirim")
            lifecycleScope.launch { loadData() }
        }
    }

    private fun setActiveTab(tab: String) {
        currentTab = tab
        val activeColor = Color.parseColor("#B3D9FF")
        val inactiveColor = Color.TRANSPARENT

        if (tab == "pending") {
            binding.tabPending.setCardBackgroundColor(activeColor)
            binding.tabTerkirim.setCardBackgroundColor(inactiveColor)
        } else {
            binding.tabTerkirim.setCardBackgroundColor(activeColor)
            binding.tabPending.setCardBackgroundColor(inactiveColor)
        }
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val items = when (currentTab) {
            "pending" -> {
                repo.getPendingList().map { p ->
                    DisplayLaporan(
                        isPending = true,
                        serverId = null,
                        resume = p.resume,
                        latitude = p.latitude,
                        longitude = p.longitude,
                        imagepath = null,
                        image_url = null,
                        nama_kegiatan = "Pending Upload",
                        nama_kegiatan_detail_proses = "",
                        nama_kabupaten = null,
                        nama_kecamatan = null, // opsional display
                        nama_desa = null,      // opsional display
                        created_at = p.created_at,
                        localId = p.local_id,
                        localImagePath = p.local_image_path,
                        id_kecamatan = p.id_kecamatan, // penting
                        id_desa = p.id_desa            // penting
                    )
                }
            }
            "terkirim" -> {
                repo.getServerList(idPcl).map { s ->
                    DisplayLaporan(
                        isPending = false,
                        serverId = s.id_sipantau_transaksi,
                        resume = s.resume,
                        latitude = s.latitude,
                        longitude = s.longitude,
                        imagepath = s.imagepath,
                        image_url = s.image_url,
                        nama_kegiatan = s.nama_kegiatan,
                        nama_kegiatan_detail_proses = s.nama_kegiatan_detail_proses,
                        nama_kabupaten = s.nama_kabupaten,
                        nama_kecamatan = s.nama_kecamatan,
                        nama_desa = s.nama_desa,
                        created_at = s.created_at,
                        localId = null,
                        localImagePath = null,
                        id_kecamatan = null,
                        id_desa = null
                    )
                }
            }
            else -> emptyList()
        }

        withContext(Dispatchers.Main) {
            adapter.update(items)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun deleteItem(item: DisplayLaporan) {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Yakin ingin menghapus laporan ini?")
            .setPositiveButton("Ya") { _, _ ->
                lifecycleScope.launch {
                    if (item.isPending) {
                        item.localId?.let { repo.deletePendingByLocalId(it) }
                    } else {
                        item.serverId?.let { repo.deleteServerById(it) }
                    }
                    loadData()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun sendPending(item: DisplayLaporan) {
        AlertDialog.Builder(this)
            .setTitle("Kirim Laporan")
            .setMessage("Kirim laporan ini ke server sekarang?")
            .setPositiveButton("Kirim") { _, _ ->
                lifecycleScope.launch {
                    if (!NetworkUtil.isOnline(this@PantauAktivitas)) {
                        Toast.makeText(this@PantauAktivitas, "Masih offline", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    Toast.makeText(this@PantauAktivitas, "Mengirim...", Toast.LENGTH_SHORT).show()

                    val pending = PendingLaporanEntity(
                        local_id = item.localId ?: 0L,
                        id_pcl = idPcl,
                        id_kegiatan_detail_proses = idKegDetail,
                        resume = item.resume,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        id_kecamatan = item.id_kecamatan, // dari DisplayLaporan
                        id_desa = item.id_desa,           // dari DisplayLaporan
                        local_image_path = item.localImagePath,
                        created_at = item.created_at
                    )

                    val ok = repo.uploadPendingOnce(pending)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PantauAktivitas,
                            if (ok) "Terkirim" else "Gagal mengirim",
                            Toast.LENGTH_SHORT
                        ).show()
                        lifecycleScope.launch { loadData() }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { loadData() }
    }
}
