package com.example.projectmapgroup7.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task
import com.example.projectmapgroup7.viewmodel.HomeViewModel

class HomeFragment : Fragment() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var tvNoTask: TextView
    private lateinit var taskAdapter: TaskAdapter

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks)
        tvNoTask = view.findViewById(R.id.tvNoTask)

        setupRecyclerView()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList()) { task ->
            navigateToDetail(task)
        }

        recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewTasks.adapter = taskAdapter
    }

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.updateData(tasks)
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            tvNoTask.visibility =
                if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvNoTask.text = message
        }
    }

    private fun loadData() {
        val sharedPref = requireActivity()
            .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

        val userId = sharedPref.getString("id_user", null)

        if (userId.isNullOrEmpty()) {
            tvNoTask.text = "User belum login!"
            tvNoTask.visibility = View.VISIBLE
        } else {
            viewModel.loadActiveTasks(userId)
        }
    }

    private fun navigateToDetail(task: Task) {
        val bundle = Bundle().apply {
            putString("title", task.title)
            putString("description", task.description)
            putString("deadline", task.deadline)
            putString("priority", task.prioritization)
            putString("image_url", task.image_url)
        }

        findNavController()
            .navigate(R.id.action_nav_home_to_detailTugasFragment, bundle)
    }
}
