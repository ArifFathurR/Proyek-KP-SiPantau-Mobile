package com.example.sipantau.api
import retrofit2.Call
import retrofit2.http.*
import com.example.sipantau.auth.LoginResponse
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.PelaporanResponse
import com.example.sipantau.model.UserData
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    // Login
    @FormUrlEncoded
    @POST("auth/login")
    fun login(
        @Field("email") sobatId: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // Get profile (me)
    @GET("me")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<UserData>


    @GET("kegiatan")
    fun getKegiatan(
        @Header("Authorization") token: String
    ): Call<KegiatanResponse>

    @GET("pelaporan")
    fun getLaporan(
        @Header("Authorization") token: String,
        @Query("id_pcl") idPcl: Int? = null
    ): Call<PelaporanResponse>

}

