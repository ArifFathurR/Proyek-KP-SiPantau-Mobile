package com.example.sipantau.auth
import com.example.sipantau.model.UserData
data class LoginResponse(
    val status: String,
    val message: String,
    val token: String,
    val user: UserData
)

