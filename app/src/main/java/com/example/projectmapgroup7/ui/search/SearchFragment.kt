package com.example.projectmapgroup7.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task
import androidx.fragment.app.viewModels
import com.example.projectmapgroup7.ui.home.TaskAdapter
import com.example.projectmapgroup7.viewmodel.SearchViewModel

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var taskAdapter: TaskAdapter

    private val taskList = mutableListOf<Task>()

    // 🔹 ViewModel
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        etSearch = view.findViewById(R.id.etSearch)
        recyclerViewResults = view.findViewById(R.id.recyclerViewSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)

        setupRecyclerView()
        setupObserver()
        setupSearchListener()

        return view
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            val bundle = Bundle().apply {
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("deadline", selectedTask.deadline)
                putString("priority", selectedTask.prioritization)
                putString("image_url", selectedTask.image_url)
            }
            findNavController()
                .navigate(R.id.action_nav_search_to_detailTugasFragment, bundle)
        }

        recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewResults.adapter = taskAdapter
    }

    private fun setupObserver() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskList.clear()
            taskList.addAll(tasks)
            taskAdapter.notifyDataSetChanged()
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            tvNoResults.visibility =
                if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvNoResults.text = message
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()

                if (keyword.isEmpty()) {
                    viewModel.clearResults()
                    return
                }

                val sharedPref = requireActivity()
                    .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val userId = sharedPref.getString("id_user", null)

                if (userId.isNullOrEmpty()) {
                    tvNoResults.text = "User belum login!"
                    tvNoResults.visibility = View.VISIBLE
                } else {
                    viewModel.searchTasks(userId, keyword)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
