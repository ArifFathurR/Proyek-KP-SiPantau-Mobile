package com.example.sipantau

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.PelaporanAdapter
import com.example.sipantau.adapter.ProgresAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityKinerjaHarianDetailBinding
import com.example.sipantau.model.*
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.components.MarkerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KinerjaHarianDetail : AppCompatActivity() {

    private lateinit var binding: ActivityKinerjaHarianDetailBinding
    private var idPcl: Int = 0
    private var token: String = ""

    // TAB
    private var isLaporanActive = true

    // Adapter
    private lateinit var laporanAdapter: PelaporanAdapter
    private lateinit var progressAdapter: ProgresAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKinerjaHarianDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil ID PCL
        idPcl = intent.getIntExtra("id_pcl", 0)
        if (idPcl == 0) {
            Toast.makeText(this, "ID PCL tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil Token
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: ""
        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Muat Kurva
        loadKurvaData()

        // RecyclerView (perhatikan nama id: recylerView sesuai layoutmu)
        binding.recylerView.layoutManager = LinearLayoutManager(this)

        // Tabs
        setupTabs()

        // Swipe Refresh
        binding.swipeRefresh.setOnRefreshListener {
            if (isLaporanActive) loadLaporan() else loadProgress()
        }
    }

    // =========================== TAB HANDLER ==============================

    private fun setupTabs() {
        binding.tabLaporan.setOnClickListener {
            if (!isLaporanActive) {
                setActiveTab(true)
                loadLaporan()
            }
        }

        binding.tabProgress.setOnClickListener {
            if (isLaporanActive) {
                setActiveTab(false)
                loadProgress()
            }
        }

        // Default buka laporan
        setActiveTab(true)
        loadLaporan()
    }

    private fun setActiveTab(isLaporan: Boolean) {
        isLaporanActive = isLaporan

        if (isLaporan) {
            binding.tabLaporan.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
            binding.tabProgress.setCardBackgroundColor(Color.TRANSPARENT)
        } else {
            binding.tabLaporan.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tabProgress.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
        }
    }

    // ====================== LOAD LAPORAN ===============================

    private fun loadLaporan() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.instance.getLaporan("Bearer $token", idPcl)
            .enqueue(object : Callback<PelaporanResponse> {
                override fun onResponse(
                    call: Call<PelaporanResponse>,
                    response: Response<PelaporanResponse>
                ) {
                    binding.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && (response.body()?.status == "success" || response.body()?.status == "ok")) {

                        val serverList = response.body()!!.data

                        // ==== KONVERSI KE DisplayLaporan ====
                        val displayList = serverList.map { s ->
                            DisplayLaporan(
                                isPending = false,
                                serverId = s.id_sipantau_transaksi,
                                resume = s.resume ?: "",
                                latitude = s.latitude,
                                longitude = s.longitude,
                                imagepath = s.imagepath,
                                image_url = s.image_url,
                                nama_kegiatan = s.nama_kegiatan,
                                nama_kegiatan_detail_proses = s.nama_kegiatan_detail_proses,
                                nama_kabupaten = s.nama_kabupaten,
                                nama_kecamatan = s.nama_kecamatan,
                                nama_desa = s.nama_desa,
                                created_at = s.created_at ?: "",
                                localId = null,
                                localImagePath = null,
                                id_kecamatan = null,
                                id_desa = null
                            )
                        }

                        laporanAdapter = PelaporanAdapter(
                            displayList,
                            onDeleteClick = { laporan ->
                                hapusLaporan(laporan.serverId!!)
                            },
                            onSendClick = { laporan ->
                                // kosong, karena server item tidak dikirim lagi
                            }
                        )

                        binding.recylerView.adapter = laporanAdapter
                    } else {
                        Toast.makeText(this@KinerjaHarianDetail, "Gagal memuat laporan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PelaporanResponse>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@KinerjaHarianDetail, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun hapusLaporan(id: Int) {
        ApiClient.instance.hapusLaporan("Bearer $token", id)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@KinerjaHarianDetail, "Laporan dihapus", Toast.LENGTH_SHORT).show()
                        loadLaporan()
                    } else {
                        Toast.makeText(this@KinerjaHarianDetail, "Gagal menghapus laporan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@KinerjaHarianDetail, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ====================== LOAD PROGRESS ===============================

    private fun loadProgress() {
        binding.swipeRefresh.isRefreshing = true

        ApiClient.instance.getProgres("Bearer $token", idPcl)
            .enqueue(object : Callback<PantauProgresListResponse> {
                override fun onResponse(
                    call: Call<PantauProgresListResponse>,
                    response: Response<PantauProgresListResponse>
                ) {
                    binding.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && (response.body()?.status == "success" || response.body()?.status == "ok")) {
                        val data = response.body()!!.data

                        // Berikan lambda onItemClick yang memanggil API hapus dan refresh
                        progressAdapter = ProgresAdapter(data) { progres ->
                            val id = progres.id_pantau_progess ?: -1
                            if (id != -1) hapusProgres(id)
                        }
                        binding.recylerView.adapter = progressAdapter
                    } else {
                        Toast.makeText(this@KinerjaHarianDetail, "Gagal memuat progress", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PantauProgresListResponse>, t: Throwable) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@KinerjaHarianDetail, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun hapusProgres(id: Int) {
        ApiClient.instance.hapusProgres("Bearer $token", id)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@KinerjaHarianDetail, "Progress dihapus", Toast.LENGTH_SHORT).show()
                        loadProgress()
                    } else {
                        Toast.makeText(this@KinerjaHarianDetail, "Gagal menghapus progress", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@KinerjaHarianDetail, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ========================= LOAD KURVA ===============================

    private fun loadKurvaData() {
        val call = ApiClient.instance.getKurvaPetugas("Bearer $token", idPcl)
        call.enqueue(object : Callback<KurvaResponse> {
            override fun onResponse(call: Call<KurvaResponse>, response: Response<KurvaResponse>) {
                if (response.isSuccessful && response.body()?.status == true) {
                    val dataList = response.body()!!.data
                    setupSCurveChart(dataList)
                } else {
                    Toast.makeText(
                        this@KinerjaHarianDetail,
                        "Gagal memuat data kurva (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<KurvaResponse>, t: Throwable) {
                Toast.makeText(this@KinerjaHarianDetail, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ======================= CHART SETUP ===============================

    private fun setupSCurveChart(dataList: List<KurvaData>) {
        if (dataList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data kurva ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        val targetEntries = ArrayList<Entry>()
        val realisasiEntries = ArrayList<Entry>()
        val tanggalList = ArrayList<String>()

        for ((index, item) in dataList.withIndex()) {
            targetEntries.add(Entry(index.toFloat(), item.target_kumulatif_absolut))
            realisasiEntries.add(Entry(index.toFloat(), item.realisasi_kumulatif))
            tanggalList.add(item.tanggal_target)
        }

        val targetDataSet = LineDataSet(targetEntries, "Target Kumulatif").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 0f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val realisasiDataSet = LineDataSet(realisasiEntries, "Realisasi Kumulatif").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 0f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            enableDashedLine(10f, 5f, 0f)
        }

        val lineData = LineData(targetDataSet, realisasiDataSet)
        binding.lineChart.data = lineData

        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            setExtraBottomOffset(15f)
            animateX(1000)
            marker = CustomMarkerView(this@KinerjaHarianDetail, dataList, tanggalList)
        }

        val xAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 10f
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in tanggalList.indices) tanggalList[index] else ""
            }
        }

        binding.lineChart.invalidate()
    }
}

// ===================== CUSTOM MARKER VIEW ============================

class CustomMarkerView(
    context: android.content.Context,
    private val dataList: List<KurvaData>,
    private val tanggalList: List<String>
) : MarkerView(context, R.layout.marker_view) {

    private val textView: android.widget.TextView = findViewById(R.id.tvMarker)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return
        val index = e.x.toInt()
        if (index in dataList.indices) {
            val item = dataList[index]
            textView.text = """
                📅 ${item.tanggal_target}
                🎯 Target Harian: ${item.target_harian_absolut}
                📈 Target Kumulatif: ${item.target_kumulatif_absolut}
                🧮 Realisasi Harian: ${item.realisasi_harian}
                ✅ Realisasi Kumulatif: ${item.realisasi_kumulatif}
            """.trimIndent()
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 20f)
    }
}
