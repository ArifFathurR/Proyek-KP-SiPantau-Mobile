package com.example.sipantau

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityTambahLaporanBinding
import com.example.sipantau.model.Desa
import com.example.sipantau.model.DesaResponse
import com.example.sipantau.model.Kecamatan
import com.example.sipantau.model.KecamatanResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class TambahLaporan : AppCompatActivity() {

    private lateinit var binding: ActivityTambahLaporanBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var imageCapture: ImageCapture? = null
    private var imageFile: File? = null

    private var idPcl: Int = 0
    private var idKegiatanDetailProses: Int = 0
    private var selectedKecamatanId: Int? = null
    private var selectedDesaId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 🧭 Ambil data intent
        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        // 🔹 Setup CameraX
        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        // 🔹 Load dropdown kecamatan
        loadKecamatan()

        // 🔹 Ambil koordinat otomatis
        binding.btnKoordinat.setOnClickListener {
            getCurrentLocation()
        }

        // 🔹 Simpan laporan
        binding.btnSimpan.setOnClickListener {
            uploadLaporan()
        }
    }

    /** -------------------- CAMERA CONFIG -------------------- */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Gagal mengaktifkan kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage(): File? {
        val file = File(externalCacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture?.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    imageFile = file
                    Toast.makeText(applicationContext, "Foto berhasil diambil", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(applicationContext, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
        return file
    }

    /** -------------------- LOKASI -------------------- */
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                binding.edtLatitude.setText(location.latitude.toString())
                binding.edtLongitude.setText(location.longitude.toString())
            } else {
                Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** -------------------- LOAD KECAMATAN -------------------- */
    private fun loadKecamatan() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        ApiClient.instance.getKec("Bearer $token")
            .enqueue(object : Callback<KecamatanResponse> {
                override fun onResponse(call: Call<KecamatanResponse>, response: Response<KecamatanResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val list = response.body()!!.data
                        val names = list.map { it.nama_kecamatan }
                        val adapter = ArrayAdapter(this@TambahLaporan, android.R.layout.simple_dropdown_item_1line, names)
                        binding.spinnerKecamatan.setAdapter(adapter)

                        binding.spinnerKecamatan.setOnItemClickListener { _, _, position, _ ->
                            selectedKecamatanId = list[position].id_kecamatan
                            loadDesa(selectedKecamatanId!!)
                        }
                    }
                }

                override fun onFailure(call: Call<KecamatanResponse>, t: Throwable) {
                    Toast.makeText(this@TambahLaporan, "Gagal memuat kecamatan", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** -------------------- LOAD DESA -------------------- */
    private fun loadDesa(idKecamatan: Int) {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        ApiClient.instance.getDesa("Bearer $token", idKecamatan)
            .enqueue(object : Callback<DesaResponse> {
                override fun onResponse(call: Call<DesaResponse>, response: Response<DesaResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val list = response.body()!!.data
                        val names = list.map { it.nama_desa }
                        val adapter = ArrayAdapter(this@TambahLaporan, android.R.layout.simple_dropdown_item_1line, names)
                        binding.spinnerDesa.setAdapter(adapter)

                        binding.spinnerDesa.setOnItemClickListener { _, _, position, _ ->
                            selectedDesaId = list[position].id_desa
                        }
                    }
                }

                override fun onFailure(call: Call<DesaResponse>, t: Throwable) {
                    Toast.makeText(this@TambahLaporan, "Gagal memuat desa", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** -------------------- UPLOAD LAPORAN -------------------- */
    private fun uploadLaporan() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return

        val resume = binding.edtResume.text.toString().trim()
        val lat = binding.edtLatitude.text.toString().trim()
        val lon = binding.edtLongitude.text.toString().trim()

        // 🧩 DEBUG LOG ke Logcat
        android.util.Log.d("TambahLaporan", """
        🧩 DEBUG DATA:
        id_pcl = $idPcl
        id_kegiatan_detail_proses = $idKegiatanDetailProses
        selectedKecamatanId = $selectedKecamatanId
        selectedDesaId = $selectedDesaId
        resume = '$resume'
        lat = '$lat'
        lon = '$lon'
        imageFile = ${imageFile?.path ?: "null"}
    """.trimIndent())

        // 🧠 Validasi per field
        when {
            idPcl == 0 -> {
                Toast.makeText(this, "ID PCL tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }
            idKegiatanDetailProses == 0 -> {
                Toast.makeText(this, "ID Kegiatan Detail Proses tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }
            selectedKecamatanId == null -> {
                Toast.makeText(this, "Kecamatan belum dipilih", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDesaId == null -> {
                Toast.makeText(this, "Desa belum dipilih", Toast.LENGTH_SHORT).show()
                return
            }
            resume.isEmpty() -> {
                Toast.makeText(this, "Resume belum diisi", Toast.LENGTH_SHORT).show()
                return
            }
            lat.isEmpty() -> {
                Toast.makeText(this, "Latitude belum diisi", Toast.LENGTH_SHORT).show()
                return
            }
            lon.isEmpty() -> {
                Toast.makeText(this, "Longitude belum diisi", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // ✅ Gunakan default image jika tidak ada hasil kamera (misalnya di emulator)
        if (imageFile == null) {
            val inputStream = resources.openRawResource(R.drawable.default_image)
            val tempFile = File(cacheDir, "default_image.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            imageFile = tempFile
            Toast.makeText(this, "📸 Menggunakan gambar default", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Semua field terisi, lanjut upload
        val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idPcl.toString())
        val idKegiatanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idKegiatanDetailProses.toString())
        val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), resume)
        val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lat)
        val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lon)
        val idKecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedKecamatanId.toString())
        val idDesaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedDesaId.toString())

        val imageBody = MultipartBody.Part.createFormData(
            "image",
            imageFile!!.name,
            RequestBody.create("image/*".toMediaTypeOrNull(), imageFile!!)
        )

        Toast.makeText(this, "Mengirim laporan...", Toast.LENGTH_SHORT).show()

        ApiClient.instance.createPelaporan(
            "Bearer $token",
            idPclBody,
            idKegiatanBody,
            resumeBody,
            latBody,
            lonBody,
            idKecBody,
            idDesaBody,
            imageBody
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@TambahLaporan, "✅ Laporan berhasil dikirim", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@TambahLaporan,
                        "Gagal mengirim laporan (kode: ${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                    android.util.Log.e("TambahLaporan", "Response gagal: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@TambahLaporan, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("TambahLaporan", "Upload gagal", t)
            }
        })
    }



    /** -------------------- UTIL -------------------- */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
