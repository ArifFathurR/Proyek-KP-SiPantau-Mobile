package com.example.sipantau

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityKinerjaHarianDetailBinding
import com.example.sipantau.model.KurvaData
import com.example.sipantau.model.KurvaResponse
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKinerjaHarianDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Ambil id_pcl dari intent
        idPcl = intent.getIntExtra("id_pcl", 0)
        if (idPcl == 0) {
            Toast.makeText(this, "ID PCL tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Ambil token dari SharedPreferences yang sama dengan LoginActivity
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Muat data kurva
        loadKurvaData()
    }

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

            // ✅ Pasang custom MarkerView (tooltip)
            marker = CustomMarkerView(this@KinerjaHarianDetail, dataList, tanggalList)
        }

        val xAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 10f
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.setLabelCount(5, true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in tanggalList.indices) tanggalList[index] else ""
            }
        }

        val leftAxis = binding.lineChart.axisLeft
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        val legend = binding.lineChart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textSize = 11f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(true)

        binding.lineChart.invalidate()
    }
}

// ✅ Custom MarkerView (tooltip card modern)
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

            animate().alpha(1f).setDuration(150).start()
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 20f)
    }
}
