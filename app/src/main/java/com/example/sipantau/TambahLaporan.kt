package com.example.sipantau

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.sipantau.model.DesaResponse
import com.example.sipantau.model.KecamatanResponse
import com.example.sipantau.repository.WilayahRepository
import com.example.sipantau.util.FileUtil
import com.example.sipantau.utils.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    // contoh path file yang kamu upload (bisa dipakai sbg placeholder saat testing)
    companion object {
        // adjust path if needed; developer provided this file earlier
        const val SAMPLE_IMAGE_PATH = "/mnt/data/45f14d54-461a-43b4-9bc4-868d6531cbea.png"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = LaporanRepository(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        idPcl = intent.getIntExtra("id_pcl", 0)
        idKegiatanDetailProses = intent.getIntExtra("id_kegiatan_detail_proses", 0)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        loadKecamatan()
        wilayahRepo = WilayahRepository(this)

        binding.btnKembali.setOnClickListener {
            finish()
        }


        binding.btnKoordinat.setOnClickListener { getCurrentLocation() }
        binding.btnSimpan.setOnClickListener { onSaveClicked() }
    }

    // ---------- CameraX preview setup ----------
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

    // suspending wrapper untuk mengambil foto dan menyimpan ke filesDir/images
    private suspend fun capturePhotoToFile(): String? = suspendCancellableCoroutine { cont ->
        val ic = imageCapture
        if (ic == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val dir = File(filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        val outputFile = File(dir, "lap_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        ic.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    // ---------- Location ----------
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

    // ---------- Load kecamatan / desa ----------
    private fun loadKecamatan() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val raw = prefs.getString(LoginActivity.PREF_TOKEN, null)
        val token = if (raw != null) "Bearer $raw" else null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isOnline(this@TambahLaporan) && token != null) {
                    // ONLINE → ambil API
                    val call = ApiClient.instance.getKec(token)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val list = resp.body()!!.data

                        // simpan ke ROOM
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
                    // OFFLINE → load dari ROOM
                    val local = wilayahRepo.getKecamatan()
                    showKecamatanDropdown(local)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // fallback offline
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
                    // ONLINE
                    val call = ApiClient.instance.getDesa(token, idKecamatan)
                    val resp = call.execute()

                    if (resp.isSuccessful && resp.body() != null) {
                        val list = resp.body()!!.data

                        // simpan lokal
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
                    // OFFLINE
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



    // ---------- Save / Upload flow ----------
    private fun onSaveClicked() {
        val resume = binding.edtResume.text.toString().trim()
        val lat = binding.edtLatitude.text.toString().trim()
        val lon = binding.edtLongitude.text.toString().trim()
        if (resume.isEmpty()) {
            Toast.makeText(this, "Resume wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // attempt to capture photo (if camera available)
            val localImagePath: String? = try {
                // try to capture; if fails, leave null
                capturePhotoToFile()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            // If online try upload, else save pending
            if (NetworkUtil.isOnline(this@TambahLaporan)) {
                val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
                val raw = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: ""
                val token = "Bearer $raw"

                // prepare bodies
                val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idPcl.toString())
                val idKegBody = RequestBody.create("text/plain".toMediaTypeOrNull(), idKegiatanDetailProses.toString())
                val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), resume)
                val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lat)
                val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lon)
                val kecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedKecamatanId?.toString() ?: "")
                val desaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedDesaId?.toString() ?: "")

                val part = localImagePath?.let { File(it) }?.let { f ->
                    val req = RequestBody.create("image/*".toMediaTypeOrNull(), f)
                    MultipartBody.Part.createFormData("image", f.name, req)
                } ?: MultipartBody.Part.createFormData("image", "empty.jpg", RequestBody.create("text/plain".toMediaTypeOrNull(), ""))

                // do network call on IO thread
                withContext(Dispatchers.IO) {
                    try {
                        val call = ApiClient.instance.createPelaporan(token, idPclBody, idKegBody, resumeBody, latBody, lonBody, kecBody, desaBody, part)
                        val resp = call.execute()
                        if (resp.isSuccessful) {
                            // success -> finish
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TambahLaporan, "Laporan terkirim", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } else {
                            // fallback: save pending
                            val pending = PendingLaporanEntity(
                                id_pcl = idPcl,
                                id_kegiatan_detail_proses = idKegiatanDetailProses,
                                resume = resume,
                                latitude = if (lat.isEmpty()) null else lat,
                                longitude = if (lon.isEmpty()) null else lon,
                                id_kecamatan = selectedKecamatanId,
                                id_desa = selectedDesaId,
                                local_image_path = localImagePath,
                                created_at = now
                            )
                            repo.savePending(pending)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TambahLaporan, "Gagal kirim, disimpan sebagai pending", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // save pending
                        val pending = PendingLaporanEntity(
                            id_pcl = idPcl,
                            id_kegiatan_detail_proses = idKegiatanDetailProses,
                            resume = resume,
                            latitude = if (lat.isEmpty()) null else lat,
                            longitude = if (lon.isEmpty()) null else lon,
                            id_kecamatan = selectedKecamatanId,
                            id_desa = selectedDesaId,
                            local_image_path = localImagePath,
                            created_at = now
                        )
                        repo.savePending(pending)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TambahLaporan, "Offline — laporan disimpan (pending)", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            } else {
                // offline -> save pending
                val pending = PendingLaporanEntity(
                    id_pcl = idPcl,
                    id_kegiatan_detail_proses = idKegiatanDetailProses,
                    resume = resume,
                    latitude = if (lat.isEmpty()) null else lat,
                    longitude = if (lon.isEmpty()) null else lon,
                    id_kecamatan = selectedKecamatanId,
                    id_desa = selectedDesaId,
                    local_image_path = localImagePath,
                    created_at = now
                )
                repo.savePending(pending)
                Toast.makeText(this@TambahLaporan, "Tersimpan lokal (pending). Akan dikirim saat online.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    suspend fun uploadPendingOnce(pending: PendingLaporanEntity): Boolean {
        return try {
            // Ambil token dari SharedPreferences
            val prefs = baseContext.getSharedPreferences(LoginActivity.PREF_NAME, AppCompatActivity.MODE_PRIVATE)
            val raw = prefs.getString(LoginActivity.PREF_TOKEN, null) ?: return false
            val token = "Bearer $raw"

            // Prepare request bodies
            val idPclBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_pcl.toString())
            val idKegBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_kegiatan_detail_proses.toString())
            val resumeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.resume)
            val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.latitude ?: "")
            val lonBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.longitude ?: "")
            val kecBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_kecamatan?.toString() ?: "")
            val desaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), pending.id_desa?.toString() ?: "")

            val part = pending.local_image_path?.let { File(it) }?.let { f ->
                val req = RequestBody.create("image/*".toMediaTypeOrNull(), f)
                MultipartBody.Part.createFormData("image", f.name, req)
            } ?: MultipartBody.Part.createFormData("image", "empty.jpg", RequestBody.create("text/plain".toMediaTypeOrNull(), ""))

            // Execute network call
            val call = ApiClient.instance.createPelaporan(token, idPclBody, idKegBody, resumeBody, latBody, lonBody, kecBody, desaBody, part)
            val resp = call.execute()

            if (resp.isSuccessful) {
                // Jika berhasil kirim ke server, hapus dari pending DB
//                pending.local_id?.let { deletePendingByLocalId(it) }
                true
            } else {
                false
            }
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
