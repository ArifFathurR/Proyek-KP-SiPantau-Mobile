package com.example.sipantau

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.ActivityTambahLaporanBinding
import com.example.sipantau.localData.entity.DesaLocalEntity
import com.example.sipantau.localData.entity.KecamatanLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanEntity
import com.example.sipantau.repository.LaporanRepository
import com.example.sipantau.repository.WilayahRepository
import com.example.sipantau.utils.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

class TambahLaporan : AppCompatActivity() {

    private lateinit var binding: ActivityTambahLaporanBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var idPcl: Int = 0
    private var idKegiatanDetailProses: Int = 0
    private var selectedKecamatanId: Int? = null
    private var selectedDesaId: Int? = null
    private lateinit var wilayahRepo: WilayahRepository
    private lateinit var repo: LaporanRepository

    // Flag untuk mencegah double submit
    private var isSubmitting = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Konfigurasi kompresi
        private const val MAX_IMAGE_SIZE = 1024 // Max width/height dalam pixel
        private const val JPEG_QUALITY = 75 // Quality 75% untuk balance size vs quality
        private const val TARGET_FILE_SIZE = 150 * 1024 // Target 150KB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = LaporanRepository(this)
        wilayahRepo = WilayahRepository(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        loadKecamatan()

        binding.btnKembali.setOnClickListener { finish() }
        binding.btnKoordinat.setOnClickListener { getCurrentLocation() }
        binding.btnSimpan.setOnClickListener { onSaveClicked() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(windowManager.defaultDisplay.rotation)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memulai kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Fungsi untuk capture dan langsung kompres gambar
     */
    private suspend fun captureAndCompressPhoto(): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Capture foto dulu
            val tempFile = capturePhotoToFile() ?: return@withContext null

            // 2. Kompres gambar
            val compressedFile = compressImage(tempFile)

            // 3. Hapus file temporary
            tempFile.delete()

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun capturePhotoToFile(): File? = suspendCancellableCoroutine { cont ->
        val ic = imageCapture
        if (ic == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val dir = File(filesDir, "images_temp")
        if (!dir.exists()) dir.mkdirs()
        val outputFile = File(dir, "temp_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        ic.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    /**
     * Fungsi kompresi gambar yang efisien
     * - Resize ke max 1024px
     * - Kompres JPEG ke quality 75%
     * - Target size ~150KB
     */
    private suspend fun compressImage(sourceFile: File): File = withContext(Dispatchers.IO) {
        val dir = File(filesDir, "images")
        if (!dir.exists()) dir.mkdirs()

        val outputFile = File(dir, "compressed_${System.currentTimeMillis()}.jpg")

        try {
            // Decode dengan inSampleSize untuk hemat memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            // Hitung sample size
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            options.inJustDecodeBounds = false

            // Decode dengan sample size
            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            // Resize jika masih terlalu besar
            if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
                val ratio = min(
                    MAX_IMAGE_SIZE.toFloat() / bitmap.width,
                    MAX_IMAGE_SIZE.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()

                val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                bitmap.recycle()
                bitmap = resized
            }

            // Compress dan save
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            bitmap.recycle()

            // Log hasil kompresi
            val originalSize = sourceFile.length()
            val compressedSize = outputFile.length()
            android.util.Log.d("ImageCompress",
                "Original: ${originalSize / 1024}KB → Compressed: ${compressedSize / 1024}KB")

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            // Jika kompresi gagal, return file original
            sourceFile.copyTo(outputFile, overwrite = true)
            outputFile
        }
    }

    /**
     * Calculate sample size untuk decode bitmap secara efisien
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getCurrentLocation() {
        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            return
        }
        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                binding.edtLatitude.setText(loc.latitude.toString())
                binding.edtLongitude.setText(loc.longitude.toString())
            } else {
                Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadKecamatan() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
        val token = if (raw != null) "Bearer $raw" else null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isOnline(this@TambahLaporan) && token != null) {
                    val call = ApiClient.instance.getKec(token)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val list = resp.body()!!.data
                        val toLocal = list.map {
                            KecamatanLocalEntity(
                                id_kecamatan = it.id_kecamatan ?: 0,
                                id_kabupaten = it.id_kabupaten,
                                nama_kecamatan = it.nama_kecamatan
                            )
                        }
                        wilayahRepo.saveKecamatan(toLocal)
                        showKecamatanDropdown(toLocal)
                    }
                } else {
                    val local = wilayahRepo.getKecamatan()
                    showKecamatanDropdown(local)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val local = wilayahRepo.getKecamatan()
                showKecamatanDropdown(local)
            }
        }
    }

    private suspend fun showKecamatanDropdown(list: List<KecamatanLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_kecamatan }
            val adapter = ArrayAdapter(this@TambahLaporan, android.R.layout.simple_dropdown_item_1line, names)
            binding.spinnerKecamatan.setAdapter(adapter)

            binding.spinnerKecamatan.setOnItemClickListener { _, _, pos, _ ->
                selectedKecamatanId = list[pos].id_kecamatan
                loadDesa(selectedKecamatanId!!)
            }
        }
    }

    private fun loadDesa(idKecamatan: Int) {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
        val token = if (raw != null) "Bearer $raw" else null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isOnline(this@TambahLaporan) && token != null) {
                    val call = ApiClient.instance.getDesa(token, idKecamatan)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val list = resp.body()!!.data
                        val toLocal = list.map {
                            DesaLocalEntity(
                                id_desa = it.id_desa ?: 0,
                                id_kecamatan = it.id_kecamatan,
                                nama_desa = it.nama_desa
                            )
                        }
                        wilayahRepo.saveDesa(toLocal)
                        showDesaDropdown(toLocal)
                    }
                } else {
                    val local = wilayahRepo.getDesaByKecamatan(idKecamatan)
                    showDesaDropdown(local)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val local = wilayahRepo.getDesaByKecamatan(idKecamatan)
                showDesaDropdown(local)
            }
        }
    }

    private suspend fun showDesaDropdown(list: List<DesaLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_desa }
            val adapter = ArrayAdapter(this@TambahLaporan, android.R.layout.simple_dropdown_item_1line, names)
            binding.spinnerDesa.setAdapter(adapter)

            binding.spinnerDesa.setOnItemClickListener { _, _, pos, _ ->
                selectedDesaId = list[pos].id_desa
            }
        }
    }

    private fun onSaveClicked() {
        if (isSubmitting) {
            Toast.makeText(this, "Sedang memproses...", Toast.LENGTH_SHORT).show()
            return
        }

        val resume = binding.edtResume.text.toString().trim()
        val lat = binding.edtLatitude.text.toString().trim()
        val lon = binding.edtLongitude.text.toString().trim()

        if (resume.isEmpty()) {
            Toast.makeText(this, "Resume wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true

        lifecycleScope.launch {
            try {
                // Toast progress
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahLaporan, "Mengambil foto...", Toast.LENGTH_SHORT).show()
                }

                // Capture dan kompres foto
                val compressedFile = captureAndCompressPhoto()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahLaporan, "Mengirim laporan...", Toast.LENGTH_SHORT).show()
                }

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                if (NetworkUtil.isOnline(this@TambahLaporan)) {
                    uploadToServer(compressedFile, resume, lat, lon, now)
                } else {
                    savePendingLocally(compressedFile?.absolutePath, resume, lat, lon, now)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    Toast.makeText(this@TambahLaporan, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun uploadToServer(
        imageFile: File?,
        resume: String,
        lat: String,
        lon: String,
        timestamp: String
    ) = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            val token = "Bearer ${prefs.getString(LoginActivity.PREF_TOKEN, "")}"

            val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idPcl.toString())
            val idKegBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idKegiatanDetailProses.toString())
            val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), resume)
            val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lat)
            val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lon)
            val kecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedKecamatanId?.toString() ?: "")
            val desaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedDesaId?.toString() ?: "")

            val imagePart = imageFile?.let {
                val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), it)
                MultipartBody.Part.createFormData("image", it.name, reqFile)
            } ?: MultipartBody.Part.createFormData("image", "")

            val call = ApiClient.instance.createPelaporan(
                token, idPclBody, idKegBody, resumeBody, latBody, lonBody, kecBody, desaBody, imagePart
            )
            val response = call.execute()

            withContext(Dispatchers.Main) {
                isSubmitting = false
                if (response.isSuccessful) {
                    Toast.makeText(this@TambahLaporan, "✓ Laporan berhasil dikirim!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    savePendingLocally(imageFile?.absolutePath, resume, lat, lon, timestamp)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                isSubmitting = false
                savePendingLocally(imageFile?.absolutePath, resume, lat, lon, timestamp)
            }
        }
    }

    private fun savePendingLocally(
        imagePath: String?,
        resume: String,
        lat: String,
        lon: String,
        timestamp: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pending = PendingLaporanEntity(
                id_pcl = idPcl,
                id_kegiatan_detail_proses = idKegiatanDetailProses,
                resume = resume,
                latitude = if (lat.isEmpty()) null else lat,
                longitude = if (lon.isEmpty()) null else lon,
                id_kecamatan = selectedKecamatanId,
                id_desa = selectedDesaId,
                local_image_path = imagePath,
                created_at = timestamp
            )
            repo.savePending(pending)

            withContext(Dispatchers.Main) {
                isSubmitting = false
                Toast.makeText(
                    this@TambahLaporan,
                    "📱 Tersimpan lokal. Akan dikirim saat online.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    suspend fun uploadPendingOnce(pending: PendingLaporanEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
            val token = "Bearer ${prefs.getString(LoginActivity.PREF_TOKEN, "") ?: return@withContext false}"

            // Kompres gambar pending jika ada
            val imageFile = pending.local_image_path?.let { path ->
                val originalFile = File(path)
                if (originalFile.exists()) {
                    compressImage(originalFile)
                } else null
            }

            val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_pcl.toString())
            val idKegBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_kegiatan_detail_proses.toString())
            val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.resume)
            val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.latitude ?: "")
            val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.longitude ?: "")
            val kecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_kecamatan?.toString() ?: "")
            val desaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_desa?.toString() ?: "")

            val imagePart = imageFile?.let {
                val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), it)
                MultipartBody.Part.createFormData("image", it.name, reqFile)
            } ?: MultipartBody.Part.createFormData("image", "")

            val call = ApiClient.instance.createPelaporan(
                token, idPclBody, idKegBody, resumeBody, latBody, lonBody, kecBody, desaBody, imagePart
            )
            val response = call.execute()

            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}