package com.example.sipantau

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PelaporanPmlAdapter
import com.example.sipantau.databinding.ActivityMainBinding
import com.example.sipantau.model.DisplayLaporanPml
import com.example.sipantau.repository.LaporanPmlRepository
import com.example.sipantau.utils.NetworkUtil
import com.example.sipantau.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PantauAktivitasLaporanPml : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PelaporanPmlAdapter          // ✅ adapter PML
    private lateinit var repo: LaporanPmlRepository            // ✅ repo PML
    private var idPml: Int = 0                                 // ✅ id PML
    private var idKegDetail: Int = 0

    private var currentTab = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = LaporanPmlRepository(this)
        idPml = intent.getIntExtra("id_pml", 0)
        idKegDetail = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        adapter = PelaporanPmlAdapter(
            emptyList(),
            onDeleteClick = { item -> confirmDelete(item) },
            onSendClick   = { item -> confirmSend(item) }
        )

        binding.recylerView.layoutManager = LinearLayoutManager(this)
        binding.recylerView.adapter = adapter

        binding.btnTambah.setOnClickListener {
            startActivity(
                Intent(this, TambahLaporanPml::class.java).apply {
                    putExtra("id_pml", idPml)
                    putExtra("id_kegiatan_detail_proses", idKegDetail)
                }
            )
        }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch { loadData() }
        }

        binding.btnKembali.setOnClickListener { finish() }

        setupTabs()
        setActiveTab("pending")
        WorkScheduler.scheduleSync(applicationContext)
        lifecycleScope.launch { loadData() }
    }

    // ── Tabs ──────────────────────────────────────────────────────────

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
        val active   = Color.parseColor("#B3D9FF")
        val inactive = Color.TRANSPARENT
        if (tab == "pending") {
            binding.tabPending.setCardBackgroundColor(active)
            binding.tabTerkirim.setCardBackgroundColor(inactive)
        } else {
            binding.tabTerkirim.setCardBackgroundColor(active)
            binding.tabPending.setCardBackgroundColor(inactive)
        }
    }

    // ── Load data dari LaporanPmlRepository ───────────────────────────

    private suspend fun loadData() = withContext(Dispatchers.IO) {
        val items: List<DisplayLaporanPml> = when (currentTab) {

            "pending" -> repo.getPendingList().map { p ->
                DisplayLaporanPml(
                    isPending                   = true,
                    serverId                    = null,
                    resume                      = p.resume,
                    latitude                    = p.latitude,
                    longitude                   = p.longitude,
                    imagepath                   = null,
                    image_url                   = null,
                    nama_kegiatan               = "Pending Upload",
                    nama_kegiatan_detail_proses = "",
                    nama_kabupaten              = null,
                    nama_kecamatan              = null,
                    nama_desa                   = null,
                    created_at                  = p.created_at,
                    localId                     = p.local_id,
                    localImagePath              = p.local_image_path,
                    id_kecamatan                = p.id_kecamatan,
                    id_desa                     = p.id_desa,
                    pendingEntity               = p              // ✅ PendingLaporanPmlEntity
                )
            }

            "terkirim" -> repo.getServerList(idPml).map { s ->  // ✅ pakai idPml
                DisplayLaporanPml(
                    isPending                   = false,
                    serverId                    = s.id_sipantau_transaksi_pml, // ✅ field PML
                    resume                      = s.resume,
                    latitude                    = s.latitude,
                    longitude                   = s.longitude,
                    imagepath                   = s.imagepath,
                    image_url                   = s.image_url,
                    nama_kegiatan               = s.nama_kegiatan,
                    nama_kegiatan_detail_proses = s.nama_kegiatan_detail_proses,
                    nama_kabupaten              = s.nama_kabupaten,
                    nama_kecamatan              = s.nama_kecamatan,
                    nama_desa                   = s.nama_desa,
                    created_at                  = s.created_at,
                    localId                     = null,
                    localImagePath              = null,
                    id_kecamatan                = null,
                    id_desa                     = null,
                    pendingEntity               = null
                )
            }

            else -> emptyList()
        }

        withContext(Dispatchers.Main) {
            adapter.update(items)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    // ── Hapus ─────────────────────────────────────────────────────────

    private fun confirmDelete(item: DisplayLaporanPml) {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Yakin ingin menghapus laporan ini?")
            .setPositiveButton("Ya") { _, _ ->
                lifecycleScope.launch {
                    if (item.isPending) {
                        item.localId?.let { repo.deletePendingByLocalId(it)
                            Toast.makeText(this@PantauAktivitasLaporanPml, "Data pending berhasil di Hapus", Toast.LENGTH_SHORT).show()
                        }


                    } else {
                        item.serverId?.let { repo.deleteServerById(it)
                            Toast.makeText(this@PantauAktivitasLaporanPml, "Data berhasil di Hapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                    loadData()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Kirim ulang pending ───────────────────────────────────────────

    private fun confirmSend(item: DisplayLaporanPml) {
        AlertDialog.Builder(this)
            .setTitle("Kirim Laporan")
            .setMessage("Kirim laporan ini ke server sekarang?")
            .setPositiveButton("Kirim") { _, _ ->
                lifecycleScope.launch {
                    if (!NetworkUtil.isOnline(this@PantauAktivitasLaporanPml)) {
                        Toast.makeText(this@PantauAktivitasLaporanPml,
                            "Masih offline", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val entity = item.pendingEntity
                    if (entity == null) {
                        Toast.makeText(this@PantauAktivitasLaporanPml,
                            "Error: data tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    Toast.makeText(this@PantauAktivitasLaporanPml,
                        "Mengirim...", Toast.LENGTH_SHORT).show()

                    // ✅ langsung pakai PendingLaporanPmlEntity, tidak perlu cast
                    val ok = repo.uploadPendingOnce(entity)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PantauAktivitasLaporanPml,
                            if (ok) "✓ Terkirim" else "✗ Gagal mengirim",
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