package com.example.sipantau.api
import retrofit2.Call
import retrofit2.http.*
import com.example.sipantau.auth.LoginResponse
import com.example.sipantau.model.DesaResponse
import com.example.sipantau.model.KecamatanResponse
import com.example.sipantau.model.KegiatanResponse
import com.example.sipantau.model.PantauProgresCreateResponse
import com.example.sipantau.model.PantauProgresListResponse
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

    @GET("kecamatan")
    fun getKec(
        @Header("Authorization") token: String
    ): Call<KecamatanResponse>

    @GET("desa")
    fun getDesa(
        @Header("Authorization") token: String,
        @Query("id_kecamatan") idKecamatan: Int? = null
    ): Call<DesaResponse>


    @Multipart
    @POST("pelaporan")
    fun createPelaporan(
        @Header("Authorization") token: String,
        @Part("id_pcl") idPcl: RequestBody,
        @Part("id_kegiatan_detail_proses") idKegiatan: RequestBody,
        @Part("resume") resume: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("id_kecamatan") idKecamatan: RequestBody,
        @Part("id_desa") idDesa: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<Void>

    @DELETE("pelaporan/{id}")
    fun hapusLaporan(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<Void>

    @Multipart
    @POST("progres")
    fun createProgres(
        @Header("Authorization") token: String,
        @Part("id_pcl") idPcl: RequestBody,
        @Part("jumlah_realisasi_absolut") jmlRealisasiAbsolut: RequestBody,
        @Part("catatan_aktivitas") cttAktivitas: RequestBody,
    ) :Call<PantauProgresCreateResponse>

    @GET("progres")
    fun getProgres(
        @Header("Authorization") token: String,
        @Query("id_pcl") idPcl: Int? = null
    ) : Call<PantauProgresListResponse>

    @DELETE("progres/{id}")
    fun hapusProgres(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<Void>

}

