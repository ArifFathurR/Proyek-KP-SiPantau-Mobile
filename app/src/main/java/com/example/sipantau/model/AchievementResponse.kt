package com.example.sipantau.model

data class AchievementResponse(
    val status: String,
    val sobat_id: String,
    val kepatuhan: Kepatuhan?,
    val performa: Performa?,
    val speed: Speed?,
    val aktivitas: Aktivitas?,
    val achievement: List<UserAchievement>
)

data class Kepatuhan(
    val hari: List<String>,
    val streak: Int
)

data class Performa(val streak: Int)
data class Speed(val streak: Int)
data class Aktivitas(val streak: Int)

data class UserAchievement(
    val id_achievement: Int,
    val nama_achievement: String,
    val deskripsi: String,
    val kategori: String,
    val achieved: Boolean
)