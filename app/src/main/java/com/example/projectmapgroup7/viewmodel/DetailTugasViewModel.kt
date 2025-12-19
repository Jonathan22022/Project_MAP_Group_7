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

class DetailTugasViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _doneSuccess = MutableLiveData<Boolean>()
    val doneSuccess: LiveData<Boolean> = _doneSuccess

    fun markTaskDone(userId: String, title: String) {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                repository.markTaskAsDone(userId, title, sdf.format(Date()))
                _doneSuccess.value = true
            } catch (e: Exception) {
                _doneSuccess.value = false
            }
        }
    }

    fun scheduleNotification(
        context: Context,
        title: String,
        deadline: String
    ) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(deadline) ?: return

            val triggerTime = date.time - (60 * 60 * 1000)

            val intent = Intent(context, DeadlineReceiver::class.java).apply {
                putExtra("title", title)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                title.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun cancelNotification(context: Context, title: String) {
        val intent = Intent(context, DeadlineReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }
}
