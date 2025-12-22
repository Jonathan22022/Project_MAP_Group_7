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

/**
 * SearchFragment
 *
 * Fragment ini digunakan untuk fitur pencarian tugas.
 * Pengguna dapat mengetik kata kunci pada kolom pencarian,
 * lalu sistem akan menampilkan daftar tugas yang sesuai
 * berdasarkan judul atau deskripsi.
 *
 * Hasil pencarian ditampilkan menggunakan RecyclerView.
 * Jika tidak ada hasil, maka pesan akan ditampilkan ke pengguna.
 *
 * Arsitektur yang digunakan: MVVM
 */
class SearchFragment : Fragment() {

    // Input field untuk kata kunci pencarian
    private lateinit var etSearch: EditText

    // RecyclerView untuk menampilkan hasil pencarian
    private lateinit var recyclerViewResults: RecyclerView

    // TextView untuk menampilkan pesan "tidak ada hasil"
    private lateinit var tvNoResults: TextView

    // Adapter RecyclerView untuk daftar tugas
    private lateinit var taskAdapter: TaskAdapter

    // List data tugas hasil pencarian
    private val taskList = mutableListOf<Task>()

    // ViewModel yang menangani logika pencarian tugas
    private val viewModel: SearchViewModel by viewModels()

    /**
     * Membuat dan menampilkan tampilan SearchFragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_search.xml
        val view = inflater.inflate(
            R.layout.fragment_search,
            container,
            false
        )

        // Inisialisasi komponen UI
        etSearch = view.findViewById(R.id.etSearch)
        recyclerViewResults = view.findViewById(R.id.recyclerViewSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)

        // Setup RecyclerView, observer, dan listener pencarian
        setupRecyclerView()
        setupObserver()
        setupSearchListener()

        return view
    }

    /**
     * Mengatur RecyclerView dan adapter
     */
    private fun setupRecyclerView() {

        // Inisialisasi adapter dengan event klik item
        taskAdapter = TaskAdapter(taskList) { selectedTask ->

            // Mengirim data task ke halaman detail
            val bundle = Bundle().apply {
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("deadline", selectedTask.deadline)
                putString("priority", selectedTask.prioritization)
                putString("image_url", selectedTask.image_url)
            }

            // Navigasi ke DetailTugasFragment
            findNavController().navigate(
                R.id.action_nav_search_to_detailTugasFragment,
                bundle
            )
        }

        // RecyclerView menggunakan layout vertikal
        recyclerViewResults.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerViewResults.adapter = taskAdapter
    }

    /**
     * Mengamati data dari ViewModel
     */
    private fun setupObserver() {

        // Observer hasil pencarian tugas
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskList.clear()
            taskList.addAll(tasks)
            taskAdapter.notifyDataSetChanged()
        }

        // Observer pesan (misalnya: "Tidak ada hasil")
        viewModel.message.observe(viewLifecycleOwner) { message ->
            tvNoResults.visibility =
                if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvNoResults.text = message
        }
    }

    /**
     * Listener untuk mendeteksi perubahan teks pencarian
     */
    private fun setupSearchListener() {

        etSearch.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                // Ambil keyword pencarian
                val keyword = s.toString().trim()

                // Jika input kosong, hapus hasil pencarian
                if (keyword.isEmpty()) {
                    viewModel.clearResults()
                    return
                }

                // Ambil user_id dari SharedPreferences
                val sharedPref = requireActivity()
                    .getSharedPreferences(
                        "user_session",
                        android.content.Context.MODE_PRIVATE
                    )

                val userId = sharedPref.getString("id_user", null)

                // Validasi user login
                if (userId.isNullOrEmpty()) {
                    tvNoResults.text = "User belum login!"
                    tvNoResults.visibility = View.VISIBLE
                } else {
                    // Jalankan pencarian melalui ViewModel
                    viewModel.searchTasks(userId, keyword)
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {}
        })
    }
}
