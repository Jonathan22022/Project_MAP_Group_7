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

/**
 * Fragment Home (Beranda)
 * Berfungsi untuk menampilkan daftar tugas aktif (belum selesai)
 * dalam bentuk RecyclerView.
 *
 * Fitur utama:
 * - Menampilkan daftar tugas milik user yang sedang login
 * - Menampilkan pesan jika tidak ada tugas atau user belum login
 * - Navigasi ke halaman detail tugas saat item diklik
 * - Menggunakan arsitektur MVVM (Fragment + ViewModel)
 */
class HomeFragment : Fragment() {

    // RecyclerView untuk menampilkan daftar tugas
    private lateinit var recyclerViewTasks: RecyclerView

    // TextView untuk menampilkan pesan (tidak ada tugas / user belum login)
    private lateinit var tvNoTask: TextView

    // Adapter RecyclerView untuk mengelola data tugas
    private lateinit var taskAdapter: TaskAdapter

    // ViewModel untuk mengambil dan mengelola data tugas
    private val viewModel: HomeViewModel by viewModels()

    /**
     * Lifecycle Fragment:
     * Menginflate layout fragment_home
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    /**
     * Dipanggil setelah view berhasil dibuat
     * Digunakan untuk inisialisasi UI dan logika awal
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi komponen UI
        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks)
        tvNoTask = view.findViewById(R.id.tvNoTask)

        // Setup RecyclerView dan Adapter
        setupRecyclerView()

        // Mengamati perubahan data dari ViewModel
        observeViewModel()

        // Memuat data tugas
        loadData()
    }

    /**
     * Menyiapkan RecyclerView dan Adapter
     */
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList()) { task ->
            // Navigasi ke halaman detail ketika item diklik
            navigateToDetail(task)
        }

        recyclerViewTasks.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerViewTasks.adapter = taskAdapter
    }

    /**
     * Mengamati LiveData dari ViewModel
     * - tasks   : daftar tugas
     * - message : pesan jika data kosong atau error
     */
    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            // Update data pada adapter
            taskAdapter.updateData(tasks)
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            // Menampilkan atau menyembunyikan pesan
            tvNoTask.visibility =
                if (message.isNullOrEmpty()) View.GONE else View.VISIBLE

            tvNoTask.text = message
        }
    }

    /**
     * Memuat data tugas berdasarkan user yang login
     */
    private fun loadData() {
        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                android.content.Context.MODE_PRIVATE
            )

        val userId = sharedPref.getString("id_user", null)

        if (userId.isNullOrEmpty()) {
            // Jika user belum login
            tvNoTask.text = "User belum login!"
            tvNoTask.visibility = View.VISIBLE
        } else {
            // Memuat tugas aktif dari ViewModel
            viewModel.loadActiveTasks(userId)
        }
    }

    /**
     * Navigasi ke halaman Detail Tugas
     * dengan mengirim data tugas melalui Bundle
     */
    private fun navigateToDetail(task: Task) {
        val bundle = Bundle().apply {
            putString("title", task.title)
            putString("description", task.description)
            putString("deadline", task.deadline)
            putString("priority", task.prioritization)
            putString("image_url", task.image_url)
        }

        findNavController()
            .navigate(
                R.id.action_nav_home_to_detailTugasFragment,
                bundle
            )
    }
}
