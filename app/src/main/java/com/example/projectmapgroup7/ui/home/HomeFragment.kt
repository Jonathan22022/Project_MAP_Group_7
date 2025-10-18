package com.example.projectmapgroup7.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.model.Task
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var tvNoTask: TextView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks)
        tvNoTask = view.findViewById(R.id.tvNoTask)

        // Adapter: klik item -> pindah ke detail
        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            val bundle = Bundle().apply {
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("deadline", selectedTask.deadline)
                putString("priority", selectedTask.prioritization)
                putString("image_url", selectedTask.image_url)
            }
            findNavController().navigate(R.id.action_nav_home_to_detailTugasFragment, bundle)
        }

        recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewTasks.adapter = taskAdapter

        loadTasks()
    }

    /**
     * Load task sesuai user yang login dari Supabase
     */
    private fun loadTasks() {
        val client = SupabaseClientInstance.client
        val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)

        if (idUser.isNullOrEmpty()) {
            tvNoTask.visibility = View.VISIBLE
            tvNoTask.text = "User belum login!"
            return
        }

        lifecycleScope.launch {
            try {
                tvNoTask.visibility = View.VISIBLE
                tvNoTask.text = "Memuat task..."

                val response = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            eq("is_complete", false)
                        }
                    }
                    .decodeList<Task>()

                taskList.clear()
                taskList.addAll(response)
                taskAdapter.notifyDataSetChanged()

                tvNoTask.visibility = if (taskList.isEmpty()) View.VISIBLE else View.GONE
                if (taskList.isEmpty()) {
                    tvNoTask.text = "Belum ada task, ayo tambahkan!"
                }

            } catch (e: Exception) {
                tvNoTask.visibility = View.VISIBLE
                tvNoTask.text = "Gagal memuat task: ${e.message}"
            }
        }
    }
}
