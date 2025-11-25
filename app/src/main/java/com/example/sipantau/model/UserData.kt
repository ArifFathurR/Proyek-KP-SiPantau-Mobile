package com.example.sipantau.model

data class UserData(
    val sobat_id: String,
    val nama_user: String,
    val email: String,
    val hp: String,
    val id_kabupaten: String,
    val roles: List<Int>,
    val id_pml: List<Int>,
    val id_pcl: List<Int>

)
