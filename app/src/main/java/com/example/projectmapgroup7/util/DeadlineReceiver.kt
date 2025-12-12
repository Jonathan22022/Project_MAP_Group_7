package com.example.projectmapgroup7.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.projectmapgroup7.R

class DeadlineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("DeadlineReceiver", "=== onReceive DIPANGGIL ===")

        Log.d("DeadlineReceiver", "Intent extras: ${intent.extras}")

        // Cek permission POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("DeadlineReceiver", "Permission POST_NOTIFICATIONS = $hasPermission")

            if (!hasPermission) {
                Log.w("DeadlineReceiver", "NOTIFIKASI BATAL: Belum punya izin POST_NOTIFICATIONS")
                return
            }
        }

        val title = intent.getStringExtra("title") ?: "Task"
        val message = "Deadline task \"$title\" tinggal 1 jam lagi!"

        Log.d("DeadlineReceiver", "Membuat notifikasi untuk task: $title")

        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pengingat Deadline")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val id = System.currentTimeMillis().toInt()
        Log.d("DeadlineReceiver", "Mengirim notifikasi dengan ID = $id")

        val nm = NotificationManagerCompat.from(context)

        if (!nm.areNotificationsEnabled()) {
            Log.e("DeadlineReceiver", "NOTIFIKASI DIMATIKAN di SETTINGS APLIKASI!")
        }

        nm.notify(id, notification)

        Log.d("DeadlineReceiver", "=== NOTIFIKASI TERKIRIM (notify() dipanggil) ===")
    }
}