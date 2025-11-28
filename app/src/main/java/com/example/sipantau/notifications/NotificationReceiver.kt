package com.example.sipantau.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Reminder"

        val builder = NotificationCompat.Builder(context, "daily_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pengingat")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 🔥 CEK PERMISSION SEBELUM NOTIFY
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                NotificationManagerCompat.from(context)
                    .notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
