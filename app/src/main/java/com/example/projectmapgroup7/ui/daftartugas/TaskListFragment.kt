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
import androidx.fragment.app.viewModels
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.projectmapgroup7.viewmodel.TaskViewModel

// Fragment ini menampilkan daftar tugas (baik yang masih berjalan maupun yang sudah selesai)
class TaskListFragment : Fragment() {

    private lateinit var containerTugas: LinearLayout
    private lateinit var fabDelete: FloatingActionButton

    private val viewModel: TaskViewModel by viewModels()
    private val selectedTasks = mutableListOf<String>()

    private var isComplete = false

    companion object {
        fun newInstance(isComplete: Boolean): TaskListFragment {
            return TaskListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isComplete", isComplete)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isComplete = arguments?.getBoolean("isComplete") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)

        containerTugas = view.findViewById(R.id.containerTugas)
        fabDelete = view.findViewById(R.id.fabDelete)

        fabDelete.setOnClickListener { deleteSelected() }

        observeViewModel()
        loadTasks()

        return view
    }

    private fun loadTasks() {
        val pref = requireActivity()
            .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

        val userId = pref.getString("id_user", null) ?: return
        viewModel.loadTasks(userId, isComplete)
    }

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) {
            displayTasks(it)
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSelected() {
        val pref = requireActivity()
            .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

        val userId = pref.getString("id_user", null) ?: return

        viewModel.deleteTasks(userId, selectedTasks) {
            selectedTasks.clear()
            fabDelete.visibility = View.GONE
            loadTasks()
        }
    }

    private fun displayTasks(tasks: List<Task>) {
        containerTugas.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (tasks.isEmpty()) {
            containerTugas.addView(
                inflater.inflate(R.layout.item_empty_task, containerTugas, false)
            )
            return
        }

        tasks.forEach { task ->
            val view = inflater.inflate(R.layout.item_task_daftar, containerTugas, false)

            val tvTitle = view.findViewById<TextView>(R.id.tvTaskTitle)
            val tvDeadline = view.findViewById<TextView>(R.id.tvTaskDeadline)
            val tvPriority = view.findViewById<TextView>(R.id.tvTaskPriority)
            val cbSelect = view.findViewById<CheckBox>(R.id.cbSelectTask)
            val indicator = view.findViewById<View>(R.id.priorityIndicator)

            tvTitle.text = task.title
            tvDeadline.text = "Deadline: ${task.deadline}"

            cbSelect.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedTasks.add(task.title)
                } else {
                    selectedTasks.remove(task.title)
                }

                fabDelete.visibility =
                    if (selectedTasks.isNotEmpty()) View.VISIBLE else View.GONE
            }

            setPriorityIndicator(task.prioritization, indicator, tvPriority)
            containerTugas.addView(view)
        }
    }

    private fun setPriorityIndicator(priority: String, indicator: View, tv: TextView) {
        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_high, null))
                tv.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_medium, null))
                tv.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_low, null))
                tv.text = "Prioritas: Rendah"
            }
        }
    }
}
