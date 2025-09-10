package com.example.sipantau.api
import retrofit2.Call
import retrofit2.http.*
import com.example.sipantau.auth.LoginResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.model.PelaporanWrapper
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.KecamatanResponse
import com.example.sipantau.model.DesaResponse

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


    @FormUrlEncoded
    @POST("api/pelaporan/tambah")
    fun tambahPelaporan(
        @Header("Authorization") token: String,
        @Field("id_kegiatan") idKegiatan: Int,
        @Field("id_kecamatan") idKecamatan: Int,
        @Field("id_desa") idDesa: Int,
        @Field("longitude") longitude: String,
        @Field("latitude") latitude: String,
        @Field("image") imageBase64: String
    ): Call<ApiResponse>
//
//    // Hapus pelaporan
//    @DELETE("api/pelaporan/{id}")
//    fun hapusPelaporan(
//        @Header("Authorization") token: String,
//        @Path("id") id: Int
//    ): Call<ApiResponse>
}

