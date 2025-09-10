package com.example.sipantau.api
import retrofit2.Call
import retrofit2.http.*
import com.example.sipantau.auth.LoginResponse
import com.example.sipantau.model.PelaporanResponse
import com.example.sipantau.model.UserData
import com.example.sipantau.model.PelaporanWrapper
//import com.example.sipantau.api.ApiResponse
import com.example.sipantau.model.DeleteResponse
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

    @DELETE("api/pelaporan/{id}")
    fun deletePelaporan(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<DeleteResponse>

//    // Tambah pelaporan
//    @FormUrlEncoded
//    @POST("api/pelaporan/tambah")
//    fun tambahPelaporan(
//        @Header("Authorization") token: String,
//        @Field("judul") judul: String,
//        @Field("deskripsi") deskripsi: String,
//        @Field("image") imageBase64: String
//    ): Call<ApiResponse>
//
//    // Hapus pelaporan
//    @DELETE("api/pelaporan/{id}")
//    fun hapusPelaporan(
//        @Header("Authorization") token: String,
//        @Path("id") id: Int
//    ): Call<ApiResponse>
}

