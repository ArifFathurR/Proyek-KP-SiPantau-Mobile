package com.example.sipantau

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sipantau.adapter.KegiatanAdapter
import com.example.sipantau.api.ApiClient
import com.example.sipantau.auth.LoginActivity
import com.example.sipantau.databinding.DashboardPmlBinding
import com.example.sipantau.model.Feedback
import com.example.sipantau.model.Kegiatan
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.TotalKegPClResponse
import com.example.sipantau.model.TotalKegPMlResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardPML : AppCompatActivity() {

    private lateinit var binding: DashboardPmlBinding
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listAktif = listOf<Kegiatan>()
    private var listTidakAktif = listOf<Kegiatan>()

    companion object {
        private const val TAG = "DashboardPML"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = DashboardPmlBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "DashboardPML onCreate started")

            // ====== CEK LOGIN ======
            if (!isUserLoggedIn()) {
                navigateToLogin()
                return
            }

            showLoggedInUserName()

            // ====== SETUP ADAPTER ======
            kegiatanAdapter = KegiatanAdapter(emptyList()) { kegiatan ->
                val intent = Intent(this, PantauAktivitasPML::class.java)
                intent.putExtra("id_kegiatan_detail_proses", kegiatan.id_kegiatan_detail_proses)
                intent.putExtra("id_pml", kegiatan.id_pml)
                startActivity(intent)
            }

            binding.rvKegiatan.apply {
                layoutManager = LinearLayoutManager(this@DashboardPML)
                adapter = kegiatanAdapter
            }

            loadKegiatanFromAPI()

            // ====== BUTTON LISTENER ======
            binding.gambarProfil.setOnClickListener {
                Log.d(TAG, "Profile picture clicked")
                binding.bottomNavigationView.selectedItemId = R.id.nav_profile
            }

            binding.tabAktif.setOnClickListener {
                kegiatanAdapter.updateData(listAktif)
                binding.tabAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
                binding.tabTidakAktif.setCardBackgroundColor(Color.TRANSPARENT)

                if (listAktif.isEmpty()) {
                    Toast.makeText(this, "Sedang tidak ada data kegiatan aktif", Toast.LENGTH_SHORT).show()
                }
            }

            binding.tabTidakAktif.setOnClickListener {
                kegiatanAdapter.updateData(listTidakAktif)
                binding.tabTidakAktif.setCardBackgroundColor(Color.parseColor("#B3D9FF"))
                binding.tabAktif.setCardBackgroundColor(Color.TRANSPARENT)

                if (listTidakAktif.isEmpty()) {
                    Toast.makeText(this, "Sedang tidak ada data kegiatan tidak aktif", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnPantauAktivitas.setOnClickListener {
                startActivity(Intent(this, PantauAktivitasPML::class.java))
            }

            binding.btnFeedback.setOnClickListener {
                startActivity(Intent(this, FeedbackUser::class.java))
            }

            binding.btnAchievement.setOnClickListener {
                startActivity(Intent(this, PantauAktivitasPML::class.java))
            }

            loadTotalKegiatanPml()

            // ====== SETUP BOTTOM NAVIGATION ======
            setupBottomNavigation()

            Log.d(TAG, "DashboardPML onCreate completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ====================================================================
    // BOTTOM NAVIGATION HANDLER

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigationView.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.nav_home -> {
                            Log.d(TAG, "Home selected")
                            showDashboardContent()
                            true
                        }
                        R.id.nav_profile -> {
                            Log.d(TAG, "Profile selected")
                            loadFragment(Profile())
                            hideDashboardContent()
                            true
                        }
                        R.id.nav_info -> {
                            Log.d(TAG, "Info selected")
                            loadFragment(Info())
                            hideDashboardContent()
                            true
                        }
                        else -> {
                            Log.w(TAG, "Unknown menu item selected")
                            false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in navigation: ${e.message}", e)
                    Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            // Set home as default selected
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            Log.d(TAG, "Bottom navigation setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
            Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            Log.d(TAG, "Loading fragment: ${fragment.javaClass.simpleName}")

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()

            Log.d(TAG, "Fragment loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment: ${e.message}", e)
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideDashboardContent() {
        try {
            Log.d(TAG, "Hiding dashboard content")
            binding.scrollView.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding dashboard: ${e.message}", e)
        }
    }

    private fun showDashboardContent() {
        try {
            Log.d(TAG, "Showing dashboard content")

            // Remove any fragments
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment != null) {
                Log.d(TAG, "Removing current fragment")
                supportFragmentManager.beginTransaction()
                    .remove(currentFragment)
                    .commitAllowingStateLoss()
            }

            binding.scrollView.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            loadKegiatanFromAPI()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dashboard: ${e.message}", e)
        }
    }

    // ====================================================================
    // LOAD KEGIATAN FROM API

    private fun loadKegiatanFromAPI() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, "") ?: ""
        val auth = "Bearer $token"

        ApiClient.instance.getKegiatan(auth)
            .enqueue(object : Callback<KegiatanResponse> {
                override fun onResponse(call: Call<KegiatanResponse>, response: Response<KegiatanResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.kegiatan_pml ?: emptyList()

                        listAktif = data.filter { it.status_kegiatan == "aktif" }
                        listTidakAktif = data.filter { it.status_kegiatan == "tidak aktif" }

                        kegiatanAdapter.updateData(listAktif)
                    } else {
                        Toast.makeText(this@DashboardPML, "Gagal memuat kegiatan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<KegiatanResponse>, t: Throwable) {
                    Toast.makeText(this@DashboardPML, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ====================================================================
    // LOGIN HANDLER

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)
        return !token.isNullOrEmpty()
    }

    private fun showLoggedInUserName() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(LoginActivity.PREF_USER, null)
        binding.nama.text = if (userJson != null) {
            val user = Gson().fromJson(userJson, com.example.sipantau.model.UserData::class.java)
            "Halo, ${user.nama_user}"
        } else "Halo, Pengguna"
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ====================================================================
    // LOAD TOTAL KEGIATAN

    private fun loadTotalKegiatanPml() {
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(LoginActivity.PREF_TOKEN, null)

        if (token.isNullOrEmpty()) return

        ApiClient.instance.getTotalKegPml("Bearer $token")
            .enqueue(object : retrofit2.Callback<TotalKegPMlResponse> {
                override fun onResponse(
                    call: retrofit2.Call<TotalKegPMlResponse>,
                    response: retrofit2.Response<TotalKegPMlResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val total = response.body()!!.total_kegiatan_pml
                        val total_pcl = response.body()!!.total_pcl
                        val total_keg_aktif = response.body()!!.total_kegiatan_pml_aktif
                        binding.jmlKeg.text = total.toString()
                        binding.ratingKepatuhan.text = total_pcl.toString()
                        binding.rating.text = total_keg_aktif.toString()
                    } else {
                        binding.jmlKeg.text = "0"
                    }
                }

                override fun onFailure(call: retrofit2.Call<TotalKegPMlResponse>, t: Throwable) {
                    binding.jmlKeg.text = "0"
                }
            })
    }
}