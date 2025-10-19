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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.model.Task
import com.example.projectmapgroup7.ui.home.TaskAdapter
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private val client = SupabaseClientInstance.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        etSearch = view.findViewById(R.id.etSearch)
        recyclerViewResults = view.findViewById(R.id.recyclerViewSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)

        // 🔹 Setup RecyclerView
        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            val bundle = Bundle().apply {
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("deadline", selectedTask.deadline)
                putString("priority", selectedTask.prioritization)
                putString("image_url", selectedTask.image_url)
            }
            findNavController().navigate(R.id.action_nav_search_to_detailTugasFragment, bundle)
        }

        recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewResults.adapter = taskAdapter

        setupSearchListener()
        return view
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchTasks(query)
                } else {
                    taskList.clear()
                    taskAdapter.notifyDataSetChanged()
                    tvNoResults.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun searchTasks(keyword: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)

                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        tvNoResults.text = "User belum login!"
                        tvNoResults.visibility = View.VISIBLE
                    }
                    return@launch
                }

                // 🔹 Query ke Supabase (gunakan ilike agar case-insensitive)
                val results = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            ilike("title", "%$keyword%")
                        }
                    }
                    .decodeList<Task>()

                withContext(Dispatchers.Main) {
                    taskList.clear()
                    taskList.addAll(results)
                    taskAdapter.notifyDataSetChanged()

                    tvNoResults.visibility =
                        if (taskList.isEmpty()) View.VISIBLE else View.GONE
                    if (taskList.isEmpty()) tvNoResults.text = "Tidak ditemukan tugas dengan judul \"$keyword\""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvNoResults.text = "Gagal mencari: ${e.message}"
                    tvNoResults.visibility = View.VISIBLE
                }
            }
        }
    }
}
