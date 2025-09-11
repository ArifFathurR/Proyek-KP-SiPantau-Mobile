package com.example.sipantau.api
import retrofit2.Call
import retrofit2.http.*
import com.example.sipantau.auth.LoginResponse
import com.example.sipantau.model.ApiResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.model.PelaporanWrapper
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.KecamatanResponse
import com.example.sipantau.model.DesaResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    // Login
    @FormUrlEncoded
    @POST("api/login")
    fun login(
        @Field("sobat_id") sobatId: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // Get profile (me)
    @GET("api/me")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<UserData>

    @GET("api/pelaporan")
    fun getPelaporan(
        @Header("Authorization") token: String
    ): Call<PelaporanWrapper>


    // Get Kegiatan
    @GET("api/pelaporan/kegiatan")
    fun getKegiatan(
        @Header("Authorization") token: String
    ): Call<KegiatanResponse>

    // Get Kecamatan
    @GET("api/lokasi/kecamatan")
    fun getKecamatan(
        @Header("Authorization") token: String
    ): Call<KecamatanResponse>

    // Get Desa (per-kecamatan)
    @GET("api/lokasi/desa/{idkec}")
    fun getDesa(
        @Header("Authorization") token: String,
        @Path("idkec") idkec: Int
    ): Call<DesaResponse>


    @Multipart
    @POST("api/pelaporan/tambah")
    fun tambahPelaporan(
        @Header("Authorization") token: String,
        @Part("id_kegiatan") idKegiatan: RequestBody,
        @Part("id_kecamatan") idKecamatan: RequestBody,
        @Part("id_desa") idDesa: RequestBody,
        @Part("resume") resume: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("tanggal") tanggal: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>


//
//    // Hapus pelaporan
//    @DELETE("api/pelaporan/{id}")
//    fun hapusPelaporan(
//        @Header("Authorization") token: String,
//        @Path("id") id: Int
//    ): Call<ApiResponse>
}

