package com.example.projectmapgroup7.ui.daftartugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.model.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private lateinit var containerTugas: LinearLayout
    private lateinit var fabDelete: FloatingActionButton
    private val selectedTasks = mutableListOf<String>()

    private var tabType: String = "progress" // default

    companion object {
        fun newInstance(tabType: String): TaskListFragment {
            val fragment = TaskListFragment()
            val args = Bundle()
            args.putString("tabType", tabType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabType = arguments?.getString("tabType") ?: "progress"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)
        containerTugas = view.findViewById(R.id.containerTugas)
        fabDelete = view.findViewById(R.id.fabDelete)
        fabDelete.setOnClickListener { deleteSelectedTasks() }
        loadTasks()
        return view
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null) ?: return@launch

                val isComplete = (tabType == "selesai")

                val tasks = SupabaseClientInstance.client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            eq("is_complete", isComplete)
                        }
                    }
                    .decodeList<Task>()

                displayTasks(tasks)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat tugas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayTasks(tasks: List<Task>) {
        containerTugas.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (tasks.isEmpty()) {
            val emptyView = inflater.inflate(R.layout.item_empty_task, containerTugas, false)
            containerTugas.addView(emptyView)
            return
        }

        for (task in tasks) {
            val view = inflater.inflate(R.layout.item_task, containerTugas, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvTaskTitle)
            val tvDeadline = view.findViewById<TextView>(R.id.tvTaskDeadline)
            val tvPriority = view.findViewById<TextView>(R.id.tvTaskPriority)
            val cbSelect = view.findViewById<CheckBox>(R.id.cbSelectTask)
            val priorityIndicator = view.findViewById<View>(R.id.priorityIndicator)

            tvTitle.text = task.title
            tvDeadline.text = "Deadline: ${task.deadline}"
            setPriorityIndicator(task.prioritization, priorityIndicator, tvPriority)

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTasks.add(task.title)
                else selectedTasks.remove(task.title)
                fabDelete.visibility = if (selectedTasks.isNotEmpty()) View.VISIBLE else View.GONE
            }

            containerTugas.addView(view)
        }
    }

    private fun deleteSelectedTasks() {
        lifecycleScope.launch {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null) ?: return@launch

                selectedTasks.forEach { title ->
                    SupabaseClientInstance.client.postgrest["tasks"].delete {
                        filter {
                            eq("title", title)
                            eq("id_user", idUser)
                        }
                    }
                }

                Toast.makeText(requireContext(), "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                selectedTasks.clear()
                fabDelete.visibility = View.GONE
                loadTasks()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPriorityIndicator(priority: String, indicator: View, textView: TextView) {
        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_high, null))
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_medium, null))
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_low, null))
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                indicator.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                textView.text = "Prioritas: -"
            }
        }
    }
}
