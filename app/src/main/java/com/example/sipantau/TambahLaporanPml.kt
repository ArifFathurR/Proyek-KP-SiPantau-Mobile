package com.example.sipantau

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
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
import com.example.sipantau.databinding.ActivityTambahLaporanBinding  // Gunakan layout yang sama
import com.example.sipantau.localData.entity.DesaLocalEntity
import com.example.sipantau.localData.entity.KecamatanLocalEntity
import com.example.sipantau.localData.entity.PendingLaporanPmlEntity
import com.example.sipantau.repository.LaporanPmlRepository
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

/**
 * Activity untuk PML membuat laporan mandiri.
 * Menggunakan id_pml (bukan id_pcl) dan endpoint /pelaporan-pml.
 *
 * Cara pakai — kirim intent dari PantauAktivitasPml:
 *   intent.putExtra("id_pml", idPml)
 *   intent.putExtra("id_kegiatan_detail_proses", idKegiatanDetailProses)
 */
class TambahLaporanPml : AppCompatActivity() {

    private lateinit var binding: ActivityTambahLaporanBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var idPml: Int = 0
    private var idKegiatanDetailProses: Int = 0
    private var selectedKecamatanId: Int? = null
    private var selectedDesaId: Int? = null
    private var selectedSlsId: Int? = null // Menambahkan variabel SLS jika ada
    private lateinit var wilayahRepo: WilayahRepository
    private lateinit var repo: LaporanPmlRepository

    private var isSubmitting = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private const val MAX_IMAGE_SIZE = 1024
        private const val JPEG_QUALITY = 75
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = LaporanPmlRepository(this)
        wilayahRepo = WilayahRepository(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        idPml = intent.getIntExtra("id_pml", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        loadKecamatanFromLocal()

        binding.btnKembali.setOnClickListener { finish() }
        binding.btnKoordinat.setOnClickListener { getCurrentLocation() }
        binding.btnSimpan.setOnClickListener { onSaveClicked() }
    }

    // ─────────────────────────────────────────────────────────────────
    // KAMERA
    // ─────────────────────────────────────────────────────────────────

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

    private suspend fun captureAndCompressPhoto(): File? = withContext(Dispatchers.IO) {
        try {
            val tempFile = capturePhotoToFile() ?: return@withContext null
            val compressedFile = compressImage(tempFile)
            tempFile.delete()
            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun capturePhotoToFile(): File? = suspendCancellableCoroutine { cont ->
        val ic = imageCapture
        if (ic == null) { cont.resume(null); return@suspendCancellableCoroutine }

        val dir = File(filesDir, "images_temp")
        if (!dir.exists()) dir.mkdirs()
        val outputFile = File(dir, "temp_pml_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        ic.takePicture(outputOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(r: ImageCapture.OutputFileResults) = cont.resume(outputFile)
                override fun onError(e: ImageCaptureException) = cont.resumeWithException(e)
            }
        )
    }

    private suspend fun compressImage(sourceFile: File): File = withContext(Dispatchers.IO) {
        val dir = File(filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        val outputFile = File(dir, "compressed_pml_${System.currentTimeMillis()}.jpg")

        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            options.inJustDecodeBounds = false

            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
                val ratio = min(
                    MAX_IMAGE_SIZE.toFloat() / bitmap.width,
                    MAX_IMAGE_SIZE.toFloat() / bitmap.height
                )
                val resized = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
                bitmap.recycle()
                bitmap = resized
            }

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            sourceFile.copyTo(outputFile, overwrite = true)
            outputFile
        }
    }

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

    // ─────────────────────────────────────────────────────────────────
    // LOKASI
    // ─────────────────────────────────────────────────────────────────

    private fun getCurrentLocation() {
        val fused = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            return
        }
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                binding.edtLatitude.setText(loc.latitude.toString())
                binding.edtLongitude.setText(loc.longitude.toString())
            } else {
                Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // WILAYAH (Kecamatan & Desa dari Room lokal)
    // ─────────────────────────────────────────────────────────────────

    private fun loadKecamatanFromLocal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = wilayahRepo.getKecamatan()
            showKecamatanDropdownLocal(list)
        }
    }

    private suspend fun showKecamatanDropdownLocal(list: List<KecamatanLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_kecamatan }
            val adapter = ArrayAdapter(
                this@TambahLaporanPml,
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.spinnerKecamatan.setAdapter(adapter)
            binding.spinnerKecamatan.setOnItemClickListener { _, _, pos, _ ->
                selectedKecamatanId = list[pos].id_kecamatan
                loadDesaFromLocal(selectedKecamatanId!!)
            }
        }
    }

    private fun loadDesaFromLocal(idKecamatan: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = wilayahRepo.getDesaByKecamatan(idKecamatan)
            showDesaDropdownLocal(list)
        }
    }

    private suspend fun showDesaDropdownLocal(list: List<DesaLocalEntity>) {
        withContext(Dispatchers.Main) {
            val names = list.map { it.nama_desa }
            val adapter = ArrayAdapter(
                this@TambahLaporanPml,
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.spinnerDesa.setAdapter(adapter)
            binding.spinnerDesa.setOnItemClickListener { _, _, pos, _ ->
                selectedDesaId = list[pos].id_desa
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SAVE
    // ─────────────────────────────────────────────────────────────────

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

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val compressedFile = captureAndCompressPhoto()
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                if (NetworkUtil.isOnline(this@TambahLaporanPml)) {
                    uploadToServer(compressedFile, resume, lat, lon, now)
                } else {
                    savePendingLocally(compressedFile?.absolutePath, resume, lat, lon, now)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Toast.makeText(this@TambahLaporanPml, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
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

            val idPmlBody      = RequestBody.create("text/plain".toMediaTypeOrNull(), idPml.toString())
            val idKegBody      = RequestBody.create("text/plain".toMediaTypeOrNull(), idKegiatanDetailProses.toString())
            val resumeBody     = RequestBody.create("text/plain".toMediaTypeOrNull(), resume)
            val latBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), lat)
            val lonBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), lon)
            val kecBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedKecamatanId?.toString() ?: "")
            val desaBody       = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedDesaId?.toString() ?: "")
            // SLS dikirim sebagai string kosong jika null (opsional)
            val slsBody        = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedSlsId?.toString() ?: "")
            val createdAtBody  = RequestBody.create("text/plain".toMediaTypeOrNull(), timestamp)

            val imagePart = imageFile?.let {
                val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), it)
                MultipartBody.Part.createFormData("image", it.name, reqFile)
            } ?: MultipartBody.Part.createFormData("image", "")

            val call = ApiClient.instance.createPelaporanPml(
                token,
                idPmlBody, idKegBody, resumeBody,
                latBody, lonBody, kecBody, desaBody,
                createdAtBody, imagePart // Jika API mendukung, tambahkan slsBody di sini
            )
            val response = call.execute()

            withContext(Dispatchers.Main) {
                setLoadingState(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@TambahLaporanPml, "✓ Laporan PML berhasil dikirim!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    savePendingLocally(imageFile?.absolutePath, resume, lat, lon, timestamp)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                setLoadingState(false)
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
            val pending = PendingLaporanPmlEntity(
                id_pml = idPml,
                id_kegiatan_detail_proses = idKegiatanDetailProses,
                resume = resume,
                latitude = if (lat.isEmpty()) null else lat,
                longitude = if (lon.isEmpty()) null else lon,
                id_kecamatan = selectedKecamatanId,
                id_desa = selectedDesaId,
                // id_sls = selectedSlsId, // Pastikan entity PendingLaporanPmlEntity mendukung field ini
                local_image_path = imagePath,
                created_at = timestamp
            )
            repo.savePending(pending)

            withContext(Dispatchers.Main) {
                setLoadingState(false)
                Toast.makeText(
                    this@TambahLaporanPml,
                    "📱 Tersimpan lokal. Akan dikirim saat online.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // LOADING STATE
    // ─────────────────────────────────────────────────────────────────

    private fun setLoadingState(isLoading: Boolean) {
        isSubmitting = isLoading
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "" else "Selfie + Simpan Data"
        binding.progressBarSimpan.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.edtResume.isEnabled = !isLoading
        binding.edtLatitude.isEnabled = !isLoading
        binding.edtLongitude.isEnabled = !isLoading
        binding.spinnerKecamatan.isEnabled = !isLoading
        binding.spinnerDesa.isEnabled = !isLoading
        binding.btnKoordinat.isEnabled = !isLoading
        binding.btnKembali.isEnabled = !isLoading
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}