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
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.KegiatanSayaBinding
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.repository.KegiatanRepository
import com.example.sipantau.model.Kegiatan
import com.google.gson.Gson
import kotlinx.coroutines.launch

class KegiatanSaya : AppCompatActivity() {

    private lateinit var binding: KegiatanSayaBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KegiatanSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView dengan 2 callback
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
            layoutManager = LinearLayoutManager(this@KegiatanSaya)
            adapter = kegiatanAdapter
        }

        binding.btnKembali.setOnClickListener {
            finish()
        }

        binding.tabAktif.setOnClickListener {
            kegiatanAdapter.updateData(listAktif)
            binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        binding.tabTidakAktif.setOnClickListener {
            kegiatanAdapter.updateData(listTidakAktif)
            binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)
        }

        // Load data
        binding.root.post { loadKegiatan() }
    }

    private fun loadKegiatan() {
        val repo = KegiatanRepository(this)

        lifecycleScope.launch {
            val data: List<KegiatanEntity> = repo.getKegiatan()

            // convert to model
            val models = data.map { it.toKegiatanModel() }

            listAktif = models.filter { it.status_kegiatan == "aktif" }
            listTidakAktif = models.filter { it.status_kegiatan == "tidak aktif" }

            kegiatanAdapter.updateData(listAktif)

            if (data.isEmpty() && !isOnline()) {
                Toast.makeText(
                    this@KegiatanSaya,
                    "⚠️ Kamu sedang offline. Data lokal kosong.",
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

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}