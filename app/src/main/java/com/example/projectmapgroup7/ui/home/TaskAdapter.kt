package com.example.projectmapgroup7.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter RecyclerView untuk menampilkan daftar tugas pada halaman Home.
 *
 * Tanggung jawab utama:
 * - Menghubungkan data Task dengan tampilan item_task.xml
 * - Mengatur format deadline agar mudah dibaca
 * - Menampilkan indikator prioritas dengan warna berbeda
 * - Menangani event klik item untuk navigasi ke detail tugas
 */
class TaskAdapter(
    // List data tugas yang akan ditampilkan
    private var taskList: List<Task>,

    // Callback ketika item tugas diklik
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    /**
     * ViewHolder
     * Menyimpan referensi komponen UI dalam satu item RecyclerView
     */
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)
        val tvDeadline: TextView = itemView.findViewById(R.id.tvTaskDeadline)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelectTask)
        val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
    }

    // ======================================================
    // 🔧 Fungsi untuk memformat deadline agar lebih readable
    // Mendukung API < 26 (tanpa java.time)
    // ======================================================
    private fun formatDeadline(deadline: String?): String {
        if (deadline.isNullOrEmpty()) return "-"

        return try {
            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat =
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

            val date = inputFormat.parse(deadline)
            if (date != null) outputFormat.format(date) else deadline
        } catch (e: Exception) {
            // Jika parsing gagal, tampilkan string asli
            deadline
        }
    }

    /**
     * Membuat ViewHolder baru
     * Dipanggil saat RecyclerView membutuhkan item baru
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    /**
     * Mengikat data Task ke ViewHolder
     * Dipanggil setiap item ditampilkan di layar
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // Set judul tugas
        holder.tvTitle.text = task.title

        // Set deadline yang sudah diformat
        holder.tvDeadline.text =
            "Deadline: ${formatDeadline(task.deadline)}"

        // Set prioritas dan warna indikator
        setPriorityIndicator(
            task.prioritization,
            holder.priorityIndicator,
            holder.tvPriority
        )

        // Navigasi ke detail tugas ketika item diklik
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    /**
     * Mengembalikan jumlah item pada RecyclerView
     */
    override fun getItemCount(): Int = taskList.size

    /**
     * Memperbarui data RecyclerView
     * Dipanggil saat data baru diterima dari ViewModel
     */
    fun updateData(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()
    }

    // ======================================================
    // 🎨 Mengatur teks prioritas dan warna indikator
    // ======================================================
    private fun setPriorityIndicator(
        priority: String,
        indicator: View,
        textView: TextView
    ) {
        val context = indicator.context

        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.priority_high)
                )
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.priority_medium)
                )
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.priority_low)
                )
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                indicator.setBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
                textView.text = "Prioritas: -"
            }
        }
    }
}
