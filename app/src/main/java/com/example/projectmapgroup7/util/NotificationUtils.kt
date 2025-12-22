package com.example.projectmapgroup7.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * NotificationUtils
 *
 * Object helper (singleton) yang bertugas untuk:
 * - Menyediakan CHANNEL_ID notifikasi
 * - Membuat Notification Channel untuk pengingat deadline task
 *
 * Notification Channel wajib dibuat pada Android Oreo (API 26) ke atas
 * agar notifikasi dapat ditampilkan dengan benar.
 */
object NotificationUtils {

    // ID unik untuk notification channel
    // Harus sama dengan CHANNEL_ID yang digunakan saat membuat notifikasi
    const val CHANNEL_ID = "task_deadline_channel"

    /**
     * Membuat Notification Channel jika belum ada.
     *
     * @param context Context aplikasi untuk mengakses NotificationManager
     */
    fun createNotificationChannel(context: Context) {

        // Log untuk debugging proses pembuatan channel
        Log.d("NotifChannel", "Memulai pembuatan notification channel...")

        // Notification Channel hanya diperlukan untuk Android Oreo (API 26) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Nama channel (ditampilkan di pengaturan aplikasi)
            val name = "Task Deadline Alerts"

            // Deskripsi channel (ditampilkan di pengaturan aplikasi)
            val description = "Notifikasi untuk deadline task"

            // Tingkat prioritas notifikasi
            val importance = NotificationManager.IMPORTANCE_HIGH

            // Membuat objek NotificationChannel
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }

            // Mengambil NotificationManager dari sistem
            val manager = context.getSystemService(NotificationManager::class.java)

            // Cek apakah NotificationManager berhasil didapatkan
            if (manager == null) {
                Log.e(
                    "NotifChannel",
                    "NotificationManager = NULL! Channel gagal dibuat"
                )
            } else {
                // Mendaftarkan channel ke sistem Android
                manager.createNotificationChannel(channel)
                Log.d(
                    "NotifChannel",
                    "CHANNEL BERHASIL DIBUAT: $CHANNEL_ID"
                )
            }
        } else {
            // Untuk Android versi lama, channel tidak diperlukan
            Log.d(
                "NotifChannel",
                "Android < Oreo, channel tidak diperlukan."
            )
        }
    }
}
