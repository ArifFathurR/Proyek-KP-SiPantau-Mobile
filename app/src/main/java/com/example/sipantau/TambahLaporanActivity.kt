package com.example.sipantau

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.sipantau.api.ApiClient
//import com.example.sipantau.api.ApiService
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityTambahLaporanBinding
import com.example.sipantau.model.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Base64



class TambahLaporanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahLaporanBinding
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private val prefs by lazy { getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE) }
    private val token: String by lazy { prefs.getString(LoginActivity.PREF_TOKEN, "") ?: "" }

    private var idKegiatan: Int? = null
    private var idKecamatan: Int? = null
    private var idDesa: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Tombol kembali
        binding.btnKembali.setOnClickListener { finish() }

        // Isi tanggal & waktu otomatis
        val now = Calendar.getInstance()
        val sdfTanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.edtTglKegiatan.setText("${sdfTanggal.format(now.time)} ${sdfWaktu.format(now.time)}")
        binding.edtTglKegiatan.isEnabled = false // readonly

        // Ambil data dari API
        loadKegiatan()
        loadKecamatan()

        // Event: kecamatan dipilih -> load desa
        binding.spinnerKecamatan.setOnItemClickListener { _, _, position, _ ->
            val kecamatan = (binding.spinnerKecamatan.tag as List<Kecamatan>)[position]
            idKecamatan = kecamatan.idkec
            loadDesa(idKecamatan!!)
        }

        // Event: kegiatan dipilih
        binding.spinnerKegiatan.setOnItemClickListener { _, _, position, _ ->
            val kegiatan = (binding.spinnerKegiatan.tag as List<Kegiatan>)[position]
            idKegiatan = kegiatan.id_kegiatan_detail
        }

        // Event: desa dipilih
        binding.spinnerDesa.setOnItemClickListener { _, _, position, _ ->
            val desa = (binding.spinnerDesa.tag as List<Desa>)[position]
            idDesa = desa.iddesa
        }

        // Ambil lokasi
        binding.btnKoordinat.setOnClickListener { getCurrentLocation() }

        // Start kamera
        startCamera()

        // Executor buat background
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Tombol simpan = ambil foto + upload
        binding.btnSimpan.setOnClickListener {
            takePhotoAndUpload()
        }

    }

    // 🔹 Load data Kegiatan
    private fun loadKegiatan() {
        ApiClient.instance.getKegiatan("Bearer $token").enqueue(object : Callback<KegiatanResponse> {
            override fun onResponse(call: Call<KegiatanResponse>, response: Response<KegiatanResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    binding.spinnerKegiatan.setAdapter(
                        ArrayAdapter(this@TambahLaporanActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            data.map { "${it.nama_kegiatan_detail} (${it.bulan})" })
                    )
                    binding.spinnerKegiatan.tag = data
                }
            }
            override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {}
        })
    }

    // 🔹 Load data Kecamatan
    private fun loadKecamatan() {
        ApiClient.instance.getKecamatan("Bearer $token").enqueue(object : Callback<KecamatanResponse> {
            override fun onResponse(call: Call<KecamatanResponse>, response: Response<KecamatanResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    binding.spinnerKecamatan.setAdapter(
                        ArrayAdapter(this@TambahLaporanActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            data.map { it.nmkec })
                    )
                    binding.spinnerKecamatan.tag = data
                }
            }
            override fun onFailure(call: Call<KecamatanResponse>, t: Throwable) {}
        })
    }

    // 🔹 Load data Desa
    private fun loadDesa(idKec: Int) {
        ApiClient.instance.getDesa("Bearer $token", idKec).enqueue(object : Callback<DesaResponse> {
            override fun onResponse(call: Call<DesaResponse>, response: Response<DesaResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    binding.spinnerDesa.setAdapter(
                        ArrayAdapter(this@TambahLaporanActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            data.map { it.nmdesa })
                    )
                    binding.spinnerDesa.tag = data
                }
            }
            override fun onFailure(call: Call<DesaResponse>, t: Throwable) {}
        })
    }

    // 🔹 Ambil lokasi
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getCurrentLocation()
            else Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }

    private fun getCurrentLocation() {
        val client = fusedLocationClient ?: return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                binding.edtLongitude.setText(location.longitude.toString())
                binding.edtLatitude.setText(location.latitude.toString())
            } else {
                Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal ambil lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Kamera gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndUpload() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@TambahLaporanActivity, "Gagal ambil foto: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@TambahLaporanActivity, "Foto tersimpan", Toast.LENGTH_SHORT).show()

                    // 🔹 Convert ke Base64 dan kirim ke API
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val imageBase64 = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

                    uploadData(imageBase64)
                }
            }
        )
    }

    private fun uploadData(imageBase64: String) {
        ApiClient.instance.tambahPelaporan(
            "Bearer $token",
            idKegiatan ?: 0,
            idKecamatan ?: 0,
            idDesa ?: 0,
            binding.edtLongitude.text.toString(),
            binding.edtLatitude.text.toString(),
            imageBase64
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@TambahLaporanActivity, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TambahLaporanActivity, "Gagal simpan data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@TambahLaporanActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
