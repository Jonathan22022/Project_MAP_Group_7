package com.example.projectmapgroup7.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.tvTitle.text = task.title
        holder.tvDeadline.text = "Deadline: ${task.deadline}"

        // 🟩 Gunakan fungsi untuk atur warna & teks prioritas
        setPriorityIndicator(task.prioritization, holder.priorityIndicator, holder.tvPriority)

        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()
    }

    // 🔽 Tambahkan fungsi ini di sini
    private fun setPriorityIndicator(priority: String, indicator: View, textView: TextView) {
        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_high, null))
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_medium, null))
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(indicator.resources.getColor(R.color.priority_low, null))
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                indicator.setBackgroundColor(indicator.resources.getColor(android.R.color.darker_gray, null))
                textView.text = "Prioritas: -"
            }
        }
    }
}
