package com.example.sipantau.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Pengingat"
        val notifId = intent.getIntExtra("notif_id", System.currentTimeMillis().toInt())

        // Periksa permission POST_NOTIFICATIONS (Android 13+)
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            // Tidak ada izin -> jangan panggil notify
            return
        }

        try {
            NotificationHelper.showNotification(
                context,
                notifId,
                "Pengingat",
                message
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
