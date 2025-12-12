package com.example.projectmapgroup7.DetailTugas

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DetailTugasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detail_tugas, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        val tvDeadline = view.findViewById<TextView>(R.id.tvDetailDeadline)
        val tvPriority = view.findViewById<TextView>(R.id.tvDetailPriority)
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val btnEdit = view.findViewById<Button>(R.id.btnEditTask)
        val btnMarkAsDone = view.findViewById<Button>(R.id.btnMarkAsDone)

        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val deadline = arguments?.getString("deadline")
        val priority = arguments?.getString("priority")
        val imageUrl = arguments?.getString("image_url")

        tvTitle.text = title
        tvDescription.text = description
        tvDeadline.text = "Deadline: ${formatDeadline(deadline)}"
        tvPriority.text = "Prioritas: $priority"

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .into(ivImage)
        }

        // 🔹 Navigasi ke Edit
        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putString("title", title)
                putString("description", description)
                putString("deadline", deadline)
                putString("priority", priority)
                putString("image_url", imageUrl)
            }
            findNavController().navigate(R.id.action_detailTugasFragment_to_editTaskFragment, bundle)
        }

        // 🔹 Tandai Selesai
        btnMarkAsDone.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda yakin tugas ini sudah selesai?")
                .setPositiveButton("Ya") { _, _ ->
                    markTaskAsDone(title)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // ==============================
        // 🔔 NOTIFICATION SWITCH
        // ==============================
        val switchNotif = view.findViewById<Switch>(R.id.switchNotification)
        val sharedPref = requireActivity().getSharedPreferences("task_notifications", android.content.Context.MODE_PRIVATE)
        val isNotifOn = sharedPref.getBoolean(title + "_notif", true)
        switchNotif.isChecked = isNotifOn

        if (isNotifOn && deadline != null) {
            scheduleNotification(title!!, deadline)
        }

        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            if (isChecked) {
                if (deadline != null) {
                    scheduleNotification(title!!, deadline)
                    Toast.makeText(requireContext(), "Pengingat diaktifkan", Toast.LENGTH_SHORT).show()
                }
                editor.putBoolean(title + "_notif", true)
            } else {
                cancelNotification(title!!)
                Toast.makeText(requireContext(), "Pengingat dimatikan", Toast.LENGTH_SHORT).show()
                editor.putBoolean(title + "_notif", false)
            }
            editor.apply()
        }

        return view
    }

    // =====================================
    // 🔧 FORMAT DEADLINE (AMAN API 24)
    // =====================================
    private fun formatDeadline(deadline: String?): String {
        if (deadline.isNullOrEmpty()) return "-"

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = input.parse(deadline)
            if (date != null) output.format(date) else deadline
        } catch (e: Exception) {
            deadline
        }
    }

    private fun scheduleNotification(title: String, deadline: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(deadline) ?: return
            val deadlineMillis = date.time
            val oneHourBefore = deadlineMillis - (60 * 60 * 1000)

            val intent = android.content.Intent(requireContext(), com.example.projectmapgroup7.util.DeadlineReceiver::class.java)
            intent.putExtra("title", title)

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                requireContext(),
                title.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, oneHourBefore, pendingIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNotification(title: String) {
        val intent = android.content.Intent(requireContext(), com.example.projectmapgroup7.util.DeadlineReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            title.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun markTaskAsDone(title: String?) {
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Judul tugas tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val client = SupabaseClientInstance.client

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedTime = sdf.format(Date())

                client.postgrest["tasks"].update({
                    set("is_complete", true)
                    set("completed_at", formattedTime)
                }) {
                    filter {
                        eq("title", title)
                        eq("id_user", idUser)
                    }
                }

                Toast.makeText(requireContext(), "Tugas ditandai selesai ✅", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menandai tugas selesai: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
