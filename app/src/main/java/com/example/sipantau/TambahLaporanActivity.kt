//package com.example.sipantau
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.widget.ArrayAdapter
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.RequiresPermission
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import com.example.sipantau.api.ApiClient
//import com.example.sipantau.auth.LoginActivity
//import com.example.sipantau.databinding.ActivityTambahLaporanBinding
//import com.example.sipantau.model.*
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.*
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class TambahLaporanActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityTambahLaporanBinding
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var cameraExecutor: ExecutorService
//    private var imageCapture: ImageCapture? = null
//
//    private val prefs by lazy { getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE) }
//    private val token: String by lazy { prefs.getString(LoginActivity.PREF_TOKEN, "") ?: "" }
//
//    private var idKegiatan: Int? = null
//    private var idKecamatan: Int? = null
//    private var idDesa: Int? = null
//
//    // Request permission kamera + storage
//    private val cameraPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val granted = permissions[Manifest.permission.CAMERA] == true
//            if (granted) startCamera()
//            else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
//        }
//
//    // Request permission lokasi
//    private val locationPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) getCurrentLocation()
//            else Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        cameraExecutor = Executors.newSingleThreadExecutor()
//
//        binding.btnKembali.setOnClickListener { finish() }
//
//        // Set tanggal & waktu otomatis
//        val now = Calendar.getInstance()
//        val sdfTanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//        val sdfWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//        binding.edtTglKegiatan.setText("${sdfTanggal.format(now.time)} ${sdfWaktu.format(now.time)}")
//        binding.edtTglKegiatan.isEnabled = false
//
//        // Load API
//        loadKegiatan()
//        loadKecamatan()
//
//        // Spinner listener
//        binding.spinnerKecamatan.setOnItemClickListener { _, _, pos, _ ->
//            (binding.spinnerKecamatan.tag as? List<Kecamatan>)?.get(pos)?.let {
//                idKecamatan = it.idkec
//                loadDesa(it.idkec)
//            }
//        }
//        binding.spinnerKegiatan.setOnItemClickListener { _, _, pos, _ ->
//            (binding.spinnerKegiatan.tag as? List<Kegiatan>)?.get(pos)?.let { idKegiatan = it.id_kegiatan_detail }
//        }
//        binding.spinnerDesa.setOnItemClickListener { _, _, pos, _ ->
//            (binding.spinnerDesa.tag as? List<Desa>)?.get(pos)?.let { idDesa = it.iddesa }
//        }
//
//        // Ambil lokasi
//        binding.btnKoordinat.setOnClickListener { checkLocationPermission() }
//
//        // Cek kamera permission & start camera
//        checkCameraPermission()
//
//        // Tombol simpan
//        binding.btnSimpan.setOnClickListener { takePhotoAndUpload() }
//    }
//
//    private fun checkCameraPermission() {
//        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//        val storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
//
//        if (cameraGranted && storageGranted) startCamera()
//        else cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE))
//    }
//
//    private fun checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            getCurrentLocation()
//        } else {
//            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        }
//    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    private fun getCurrentLocation() {
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                binding.edtLongitude.setText(location.longitude.toString())
//                binding.edtLatitude.setText(location.latitude.toString())
//            } else {
//                Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
//            }
//        }.addOnFailureListener {
//            Toast.makeText(this, "Gagal ambil lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//                .setTargetRotation(binding.previewView.display.rotation)
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            imageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .setTargetRotation(binding.previewView.display.rotation)
//                .build()
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
//            } catch (e: Exception) {
//                Toast.makeText(this, "Kamera gagal: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun takePhotoAndUpload() {
//        val capture = imageCapture ?: return
//        val photoFile = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Toast.makeText(this@TambahLaporanActivity, "Gagal ambil foto: ${exc.message}", Toast.LENGTH_SHORT).show()
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    Toast.makeText(this@TambahLaporanActivity, "Foto tersimpan", Toast.LENGTH_SHORT).show()
//                    uploadData(photoFile)
//                }
//            })
//    }
//
//    private fun uploadData(photoFile: File) {
//        val idKegiatanPart = RequestBody.create("text/plain".toMediaTypeOrNull(), (idKegiatan ?: 0).toString())
//        val idKecamatanPart = RequestBody.create("text/plain".toMediaTypeOrNull(), (idKecamatan ?: 0).toString())
//        val idDesaPart = RequestBody.create("text/plain".toMediaTypeOrNull(), (idDesa ?: 0).toString())
//        val resumePart = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.edtResume.text.toString())
//        val longitudePart = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.edtLongitude.text.toString())
//        val latitudePart = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.edtLatitude.text.toString())
//
//        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//        val tanggalPart = RequestBody.create("text/plain".toMediaTypeOrNull(), sdf.format(Date()))
//
//        val filePart = MultipartBody.Part.createFormData(
//            "image",
//            photoFile.name,
//            RequestBody.create("image/jpeg".toMediaTypeOrNull(), photoFile)
//        )
//
//        ApiClient.instance.tambahPelaporan(
//            "Bearer $token",
//            idKegiatanPart,
//            idKecamatanPart,
//            idDesaPart,
//            resumePart,
//            longitudePart,
//            latitudePart,
//            tanggalPart,
//            filePart
//        ).enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                if (response.isSuccessful) {
//                    Toast.makeText(this@TambahLaporanActivity, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
//                    finish()
//                } else {
//                    Toast.makeText(this@TambahLaporanActivity, "Gagal simpan data: ${response.code()}", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                Toast.makeText(this@TambahLaporanActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
//
//    // --- API Loader Methods ---
//    private fun loadKegiatan() {
//        ApiClient.instance.getKegiatan("Bearer $token").enqueue(object : Callback<KegiatanResponse> {
//            override fun onResponse(call: Call<KegiatanResponse>, response: Response<KegiatanResponse>) {
//                val data = response.body()?.data ?: emptyList()
//                binding.spinnerKegiatan.setAdapter(ArrayAdapter(this@TambahLaporanActivity, android.R.layout.simple_dropdown_item_1line, data.map { "${it.nama_kegiatan_detail} (${it.bulan})" }))
//                binding.spinnerKegiatan.tag = data
//            }
//            override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {}
//        })
//    }
//
//    private fun loadKecamatan() {
//        ApiClient.instance.getKecamatan("Bearer $token").enqueue(object : Callback<KecamatanResponse> {
//            override fun onResponse(call: Call<KecamatanResponse>, response: Response<KecamatanResponse>) {
//                val data = response.body()?.data ?: emptyList()
//                binding.spinnerKecamatan.setAdapter(ArrayAdapter(this@TambahLaporanActivity, android.R.layout.simple_dropdown_item_1line, data.map { it.nmkec }))
//                binding.spinnerKecamatan.tag = data
//            }
//            override fun onFailure(call: Call<KecamatanResponse>, t: Throwable) {}
//        })
//    }
//
//    private fun loadDesa(idKec: Int) {
//        ApiClient.instance.getDesa("Bearer $token", idKec).enqueue(object : Callback<DesaResponse> {
//            override fun onResponse(call: Call<DesaResponse>, response: Response<DesaResponse>) {
//                val data = response.body()?.data ?: emptyList()
//                binding.spinnerDesa.setAdapter(ArrayAdapter(this@TambahLaporanActivity, android.R.layout.simple_dropdown_item_1line, data.map { it.nmdesa }))
//                binding.spinnerDesa.tag = data
//            }
//            override fun onFailure(call: Call<DesaResponse>, t: Throwable) {}
//        })
//    }
//}
