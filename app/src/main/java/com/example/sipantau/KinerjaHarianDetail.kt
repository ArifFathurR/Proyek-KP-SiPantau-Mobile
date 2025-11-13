package com.example.sipantau

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sipantau.databinding.ActivityKinerjaHarianDetailBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class KinerjaHarianDetail : AppCompatActivity() {
    private lateinit var binding: ActivityKinerjaHarianDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKinerjaHarianDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup chart dengan data dummy
        setupSCurveChart()
    }

    private fun setupSCurveChart() {
        // Data dummy untuk Kurva S (Target - Biru)
        val targetEntries = ArrayList<Entry>()
        val targetData = arrayOf(
            0f, 10f, 25f, 45f, 70f, 100f, 140f, 190f, 250f, 320f,
            400f, 490f, 580f, 670f, 750f, 820f, 875f, 920f, 955f, 980f,
            995f, 1000f
        )

        for (i in targetData.indices) {
            targetEntries.add(Entry(i.toFloat(), targetData[i]))
        }

        // Data dummy untuk Aktual (Merah)
        val aktualEntries = ArrayList<Entry>()
        val aktualData = arrayOf(
            0f, 8f, 22f, 42f, 68f, 98f, 135f, 180f, 235f, 300f,
            375f, 460f, 550f, 640f, 730f, 810f, 870f, 915f, 950f, 975f,
            992f, 998f
        )

        for (i in aktualData.indices) {
            aktualEntries.add(Entry(i.toFloat(), aktualData[i]))
        }

        // Dataset untuk Target (Biru)
        val targetDataSet = LineDataSet(targetEntries, "Target (Biru/Logistik)")
        targetDataSet.color = Color.BLUE
        targetDataSet.setCircleColor(Color.BLUE)
        targetDataSet.lineWidth = 2.5f
        targetDataSet.circleRadius = 4f
        targetDataSet.setDrawCircleHole(false)
        targetDataSet.valueTextSize = 0f // Sembunyikan nilai
        targetDataSet.setDrawFilled(false)
        targetDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curve
        targetDataSet.cubicIntensity = 0.2f

        // Dataset untuk Aktual (Merah)
        val aktualDataSet = LineDataSet(aktualEntries, "Aktual (Merah)")
        aktualDataSet.color = Color.RED
        aktualDataSet.setCircleColor(Color.RED)
        aktualDataSet.lineWidth = 2.5f
        aktualDataSet.circleRadius = 4f
        aktualDataSet.setDrawCircleHole(false)
        aktualDataSet.valueTextSize = 0f
        aktualDataSet.setDrawFilled(false)
        aktualDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        aktualDataSet.cubicIntensity = 0.2f
        aktualDataSet.enableDashedLine(10f, 5f, 0f) // Garis putus-putus untuk aktual

        // Gabungkan dataset
        val lineData = LineData(targetDataSet, aktualDataSet)
        binding.lineChart.data = lineData

        // Konfigurasi chart
        binding.lineChart.description.isEnabled = false
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.isDragEnabled = true
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.setPinchZoom(true)
        binding.lineChart.setDrawGridBackground(false)

        // Konfigurasi X-Axis
        val xAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.granularity = 1f
        xAxis.textSize = 10f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "Hari ${value.toInt()}" // Menampilkan Hari 0, Hari 1, dll
            }
        }

        // Konfigurasi Y-Axis (Left)
        val leftAxis = binding.lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 1050f

        // Nonaktifkan Y-Axis (Right)
        binding.lineChart.axisRight.isEnabled = false

        // Konfigurasi Legend
        val legend = binding.lineChart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textSize = 11f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(true)

        // Refresh chart
        binding.lineChart.invalidate()
    }
}