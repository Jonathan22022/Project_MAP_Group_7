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

class TaskAdapter(
    private var taskList: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)
        val tvDeadline: TextView = itemView.findViewById(R.id.tvTaskDeadline)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelectTask)
        val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
    }

    // ============================
    // 🔧 Format Deadline (safe API < 26)
    // ============================
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.tvTitle.text = task.title
        holder.tvDeadline.text = "Deadline: ${formatDeadline(task.deadline)}"

        // 🔹 Set prioritas & warna indikator
        setPriorityIndicator(task.prioritization, holder.priorityIndicator, holder.tvPriority)

        // 🔹 Klik ke detail
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()
    }

    // ============================
    // 🎨 Prioritas & warna indikator
    // ============================
    private fun setPriorityIndicator(priority: String, indicator: View, textView: TextView) {
        val context = indicator.context

        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_high))
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_medium))
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_low))
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                indicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                textView.text = "Prioritas: -"
            }
        }
    }
}