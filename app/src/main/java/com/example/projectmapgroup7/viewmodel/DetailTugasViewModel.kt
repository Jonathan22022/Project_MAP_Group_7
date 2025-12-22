package com.example.projectmapgroup7.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.util.DeadlineReceiver
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * DetailTugasViewModel
 *
 * ViewModel yang menangani:
 * - Penandaan tugas sebagai selesai
 * - Penjadwalan notifikasi pengingat deadline
 * - Pembatalan notifikasi deadline
 *
 * ViewModel ini memisahkan logika bisnis dari UI (Fragment),
 * sehingga UI hanya bertugas menampilkan data dan merespons perubahan state.
 */
class DetailTugasViewModel(
    // Repository sebagai penghubung ke database / Supabase
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    // =======================
    // DONE TASK STATE
    // =======================

    // LiveData untuk memberi tahu UI apakah proses "mark as done" berhasil
    private val _doneSuccess = MutableLiveData<Boolean>()
    val doneSuccess: LiveData<Boolean> = _doneSuccess

    // =======================
    // MARK TASK AS DONE
    // =======================

    /**
     * Menandai tugas sebagai selesai
     *
     * @param userId ID user pemilik tugas
     * @param title  Judul tugas (digunakan sebagai identifier)
     */
    fun markTaskDone(userId: String, title: String) {
        viewModelScope.launch {
            try {
                // Format tanggal saat task diselesaikan
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Update status task di database
                repository.markTaskAsDone(
                    userId,
                    title,
                    sdf.format(Date())
                )

                // Kirim status sukses ke UI
                _doneSuccess.value = true
            } catch (e: Exception) {
                // Jika gagal, kirim status gagal
                _doneSuccess.value = false
            }
        }
    }

    // =======================
    // DEADLINE NOTIFICATION
    // =======================

    /**
     * Menjadwalkan notifikasi 1 jam sebelum deadline tugas
     *
     * @param context  Context aplikasi
     * @param title    Judul tugas
     * @param deadline Waktu deadline (format: yyyy-MM-dd HH:mm:ss)
     */
    fun scheduleNotification(
        context: Context,
        title: String,
        deadline: String
    ) {
        try {
            // Parsing string deadline ke Date
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(deadline) ?: return

            // Waktu trigger notifikasi (1 jam sebelum deadline)
            val triggerTime = date.time - (60 * 60 * 1000)

            // Intent untuk memanggil DeadlineReceiver
            val intent = Intent(context, DeadlineReceiver::class.java).apply {
                putExtra("title", title)
            }

            // PendingIntent sebagai identitas alarm
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                title.hashCode(), // unik berdasarkan judul task
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Ambil AlarmManager dari sistem
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Jadwalkan alarm meskipun device sedang idle
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (_: Exception) {
            // Error diabaikan untuk menghindari crash aplikasi
        }
    }

    /**
     * Membatalkan notifikasi deadline yang sudah dijadwalkan
     *
     * @param context Context aplikasi
     * @param title   Judul tugas (digunakan sebagai identifier alarm)
     */
    fun cancelNotification(context: Context, title: String) {
        // Intent harus sama dengan yang digunakan saat scheduling
        val intent = Intent(context, DeadlineReceiver::class.java)

        // PendingIntent yang sama → alarm dapat dibatalkan
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ambil AlarmManager
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Batalkan alarm/notifikasi
        alarmManager.cancel(pendingIntent)
    }
}
