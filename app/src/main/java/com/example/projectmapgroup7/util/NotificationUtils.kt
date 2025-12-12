package com.example.projectmapgroup7.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

object NotificationUtils {

    const val CHANNEL_ID = "task_deadline_channel"

    fun createNotificationChannel(context: Context) {
        Log.d("NotifChannel", "Memulai pembuatan notification channel...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Deadline Alerts"
            val description = "Notifikasi untuk deadline task"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }

            val manager = context.getSystemService(NotificationManager::class.java)

            if (manager == null) {
                Log.e("NotifChannel", "NotificationManager = NULL! Channel gagal dibuat")
            } else {
                manager.createNotificationChannel(channel)
                Log.d("NotifChannel", "CHANNEL BERHASIL DIBUAT: $CHANNEL_ID")
            }
        } else {
            Log.d("NotifChannel", "Android < Oreo, channel tidak diperlukan.")
        }
    }

}