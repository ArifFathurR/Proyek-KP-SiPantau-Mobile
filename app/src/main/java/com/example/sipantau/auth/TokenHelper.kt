package com.example.sipantau.auth


import android.content.Context
import com.example.sipantau.auth.LoginActivity

object TokenHelper {
    fun getToken(context: Context): String {
        val prefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(LoginActivity.PREF_TOKEN, "").orEmpty()
    }

    fun getBearerToken(context: Context): String {
        val token = getToken(context)
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }
}