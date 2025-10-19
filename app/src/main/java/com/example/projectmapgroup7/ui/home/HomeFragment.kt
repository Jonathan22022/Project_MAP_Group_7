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

/**
 * 🏠 HomeFragment - Fragment untuk menampilkan daftar tugas yang belum selesai
 *
 * Fungsi Utama:
 * - Menampilkan daftar tugas aktif (belum selesai) milik user yang login
 * - Navigasi ke detail tugas ketika item diklik
 * - Menangani state loading, empty, dan error
 *
 * @constructor Membuat fragment home/beranda
 */
class HomeFragment : Fragment() {

    // 🔹 DEKLARASI VIEW COMPONENTS
    private lateinit var recyclerViewTasks: RecyclerView  // Untuk menampilkan list tugas
    private lateinit var tvNoTask: TextView               // Text view untuk pesan kosong/error
    private lateinit var taskAdapter: TaskAdapter         // Adapter untuk RecyclerView

    // Data source untuk daftar tugas
    private val taskList = mutableListOf<Task>()

    /**
     * 📐 Membuat tampilan fragment (Lifecycle Method)
     *
     * @param inflater Layout inflater untuk meng-inflate layout
     * @param container Container parent view
     * @param savedInstanceState State yang disimpan sebelumnya
     * @return View yang telah di-inflate
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout fragment_home.xml ke dalam view
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    /**
     * 🎯 Setup setelah tampilan dibuat (Lifecycle Method)
     *
     * @param view View yang telah dibuat
     * @param savedInstanceState State yang disimpan sebelumnya
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 INITIALIZE VIEW COMPONENTS
        // Hubungkan variabel dengan view di layout
        recyclerViewTasks = view.findViewById(R.id.recyclerViewTasks)
        tvNoTask = view.findViewById(R.id.tvNoTask)

        // 🔹 SETUP RECYCLERVIEW ADAPTER
        // Buat adapter dengan click listener untuk navigasi
        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            // Lambda function yang dijalankan ketika item tugas diklik
            navigateToTaskDetail(selectedTask)
        }

        // 🔹 CONFIGURE RECYCLERVIEW
        // Set layout manager (linear vertical) dan adapter
        recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewTasks.adapter = taskAdapter

        // 🔹 LOAD DATA
        // Memuat data tugas dari database
        loadTasks()
    }

    /**
     * 🚀 Navigasi ke halaman detail tugas
     *
     * @param selectedTask Task yang dipilih/diklik oleh user
     */
    private fun navigateToTaskDetail(selectedTask: Task) {
        // Siapkan bundle untuk passing data antar fragment
        val bundle = Bundle().apply {
            putString("title", selectedTask.title)           // Judul tugas
            putString("description", selectedTask.description) // Deskripsi tugas
            putString("deadline", selectedTask.deadline)     // Tanggal deadline
            putString("priority", selectedTask.prioritization) // Prioritas tugas
            putString("image_url", selectedTask.image_url)   // URL gambar (jika ada)
        }

        // Navigasi ke DetailTugasFragment menggunakan Navigation Component
        findNavController().navigate(R.id.action_nav_home_to_detailTugasFragment, bundle)
    }

    /**
     * 📥 Memuat daftar tugas dari Supabase database
     *
     * Proses:
     * 1. Cek session user yang login
     * 2. Tampilkan loading state
     * 3. Ambil data dari Supabase dengan filter:
     *    - id_user = user yang login
     *    - is_complete = false (hanya tugas belum selesai)
     * 4. Update UI berdasarkan hasil:
     *    - Success: tampilkan list tugas
     *    - Empty: tampilkan pesan "belum ada task"
     *    - Error: tampilkan pesan error
     */
    private fun loadTasks() {
        // Dapatkan instance Supabase client
        val client = SupabaseClientInstance.client

        // Ambil ID user dari SharedPreferences (session management)
        val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)

        // 🔹 VALIDASI USER LOGIN
        // Jika user belum login, tampilkan pesan error
        if (idUser.isNullOrEmpty()) {
            tvNoTask.visibility = View.VISIBLE
            tvNoTask.text = "User belum login!"
            return  // Stop execution jika user tidak login
        }

        // 🔹 LAUNCH COROUTINE UNTUK NETWORK OPERATION
        // Gunakan lifecycleScope untuk auto-cancel ketika fragment destroyed
        lifecycleScope.launch {
            try {
                // 🔹 SHOW LOADING STATE
                tvNoTask.visibility = View.VISIBLE
                tvNoTask.text = "Memuat task..."

                // 🔹 FETCH DATA FROM SUPABASE
                // Query ke tabel 'tasks' dengan filter
                val response = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)        // Filter by user ID
                            eq("is_complete", false)    // Hanya tugas belum selesai
                        }
                    }
                    .decodeList<Task>()  // Decode JSON response ke List<Task>

                // 🔹 UPDATE DATA SOURCE
                // Clear list lama dan tambahkan data baru
                taskList.clear()
                taskList.addAll(response)

                // 🔹 NOTIFY ADAPTER ABOUT DATA CHANGE
                taskAdapter.notifyDataSetChanged()

                // 🔹 UPDATE UI STATE BASED ON DATA
                if (taskList.isEmpty()) {
                    // Jika tidak ada tugas, tampilkan empty state
                    tvNoTask.visibility = View.VISIBLE
                    tvNoTask.text = "Belum ada task, ayo tambahkan!"
                } else {
                    // Jika ada tugas, sembunyikan pesan no task
                    tvNoTask.visibility = View.GONE
                }

            } catch (e: Exception) {
                // 🔹 ERROR HANDLING
                // Tangani error network/database
                tvNoTask.visibility = View.VISIBLE
                tvNoTask.text = "Gagal memuat task: ${e.message}"
            }
        }
    }
}