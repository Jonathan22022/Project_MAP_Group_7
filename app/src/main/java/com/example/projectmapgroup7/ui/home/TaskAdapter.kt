package com.example.projectmapgroup7.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task

// Adapter untuk menampilkan daftar Task ke dalam RecyclerView
class TaskAdapter(
    private var taskList: List<Task>,              // Daftar task yang akan ditampilkan
    private val onTaskClick: (Task) -> Unit        // Callback saat item task diklik
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // ViewHolder: merepresentasikan 1 item tampilan dalam RecyclerView
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)           // Menampilkan judul task
        val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)     // Menampilkan teks prioritas
        val tvDeadline: TextView = itemView.findViewById(R.id.tvTaskDeadline)     // Menampilkan deadline
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelectTask)         // Checkbox (opsional untuk memilih task)
        val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator) // Garis/warnanya indikator prioritas
    }

    // Dipanggil saat RecyclerView butuh ViewHolder baru
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Menghubungkan layout item_task.xml ke ViewHolder
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    // Mengikat (bind) data task ke tampilan tiap item di daftar
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]                      // Ambil task sesuai posisi
        holder.tvTitle.text = task.title                   // Set judul task
        holder.tvDeadline.text = "Deadline: ${task.deadline}" // Tampilkan deadline dengan label

        // 🟩 Atur tampilan prioritas dan warna indikator sesuai data task
        setPriorityIndicator(task.prioritization, holder.priorityIndicator, holder.tvPriority)

        // Saat item diklik, jalankan callback dari parameter onTaskClick
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    // Jumlah item yang akan ditampilkan di RecyclerView
    override fun getItemCount(): Int = taskList.size

    // Fungsi untuk memperbarui data daftar task dan refresh tampilan
    fun updateData(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()  // Memberitahu adapter bahwa data berubah → update UI
    }

    // 🔽 Fungsi untuk menyesuaikan warna indikator dan teks berdasarkan prioritas
    private fun setPriorityIndicator(priority: String, indicator: View, textView: TextView) {
        when (priority.lowercase()) {
            "tinggi" -> {
                // Warna merah (tinggi)
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_high, null))
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                // Warna kuning (sedang)
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_medium, null))
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                // Warna biru (rendah)
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_low, null))
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                // Jika data prioritas tidak dikenali
                indicator.setBackgroundColor(indicator.resources.getColor(android.R.color.darker_gray, null))
                textView.text = "Prioritas: -"
            }
        }
    }
}
