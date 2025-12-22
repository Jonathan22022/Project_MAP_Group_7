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

/**
 * DeadlineReceiver
 *
 * BroadcastReceiver ini bertugas untuk menerima alarm/pengingat
 * terkait deadline tugas dan menampilkan notifikasi ke pengguna.
 *
 * Receiver ini biasanya dipanggil oleh AlarmManager
 * ketika waktu deadline (misalnya 1 jam sebelum) tercapai.
 */
class DeadlineReceiver : BroadcastReceiver() {

    /**
     * Method ini akan otomatis dipanggil oleh sistem
     * ketika BroadcastReceiver menerima Intent
     */
    override fun onReceive(context: Context, intent: Intent) {

        // Log untuk memastikan receiver benar-benar dipanggil
        Log.d("DeadlineReceiver", "=== onReceive DIPANGGIL ===")

        // Log semua data extra yang dikirim melalui Intent
        Log.d("DeadlineReceiver", "Intent extras: ${intent.extras}")

        // =================================================
        // CEK PERMISSION NOTIFIKASI (Android 13+)
        // =================================================
        // Sejak Android 13 (TIRAMISU), aplikasi wajib
        // memiliki izin POST_NOTIFICATIONS untuk menampilkan notifikasi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permission = Manifest.permission.POST_NOTIFICATIONS

            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(
                "DeadlineReceiver",
                "Permission POST_NOTIFICATIONS = $hasPermission"
            )

            // Jika izin belum diberikan, notifikasi dibatalkan
            if (!hasPermission) {
                Log.w(
                    "DeadlineReceiver",
                    "NOTIFIKASI BATAL: Belum punya izin POST_NOTIFICATIONS"
                )
                return
            }
        }

        // =================================================
        // AMBIL DATA TASK DARI INTENT
        // =================================================
        // Ambil judul task yang dikirim saat scheduling alarm
        val title = intent.getStringExtra("title") ?: "Task"

        // Pesan notifikasi yang akan ditampilkan
        val message = "Deadline task \"$title\" tinggal 1 jam lagi!"

        Log.d(
            "DeadlineReceiver",
            "Membuat notifikasi untuk task: $title"
        )

        // =================================================
        // MEMBANGUN NOTIFIKASI
        // =================================================
        val notification = NotificationCompat.Builder(
            context,
            NotificationUtils.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ikon notifikasi
            .setContentTitle("Pengingat Deadline")            // Judul notifikasi
            .setContentText(message)                          // Isi notifikasi
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // Prioritas tinggi
            .setAutoCancel(true)                              // Hilang saat diklik
            .build()

        // ID unik agar setiap notifikasi tidak saling menimpa
        val id = System.currentTimeMillis().toInt()

        Log.d(
            "DeadlineReceiver",
            "Mengirim notifikasi dengan ID = $id"
        )

        // =================================================
        // KIRIM NOTIFIKASI KE SISTEM
        // =================================================
        val nm = NotificationManagerCompat.from(context)

        // Cek apakah notifikasi diaktifkan di setting aplikasi
        if (!nm.areNotificationsEnabled()) {
            Log.e(
                "DeadlineReceiver",
                "NOTIFIKASI DIMATIKAN di SETTINGS APLIKASI!"
            )
        }

        // Menampilkan notifikasi
        nm.notify(id, notification)

        Log.d(
            "DeadlineReceiver",
            "=== NOTIFIKASI TERKIRIM (notify() dipanggil) ==="
        )
    }
}
