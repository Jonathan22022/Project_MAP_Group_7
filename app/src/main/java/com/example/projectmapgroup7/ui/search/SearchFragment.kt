package com.example.projectmapgroup7.ui.search

// Import komponen Android & library yang digunakan
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

/**
 * SearchFragment digunakan untuk mencari tugas (Task) berdasarkan judul.
 * Pencarian dilakukan secara real-time dengan mengambil data dari Supabase.
 */
class SearchFragment : Fragment() {

    // Deklarasi view dan variabel
    private lateinit var etSearch: EditText
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>() // daftar hasil pencarian
    private val client = SupabaseClientInstance.client // koneksi Supabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout untuk fragment ini
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Inisialisasi view
        etSearch = view.findViewById(R.id.etSearch)
        recyclerViewResults = view.findViewById(R.id.recyclerViewSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)

        // 🔹 Setup RecyclerView untuk menampilkan hasil pencarian
        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            // Aksi ketika salah satu item tugas diklik → pindah ke halaman detail tugas
            val bundle = Bundle().apply {
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("deadline", selectedTask.deadline)
                putString("priority", selectedTask.prioritization)
                putString("image_url", selectedTask.image_url)
            }
            findNavController().navigate(R.id.action_nav_search_to_detailTugasFragment, bundle)
        }

        // Mengatur tampilan daftar hasil (RecyclerView)
        recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewResults.adapter = taskAdapter

        // Aktifkan listener untuk mendeteksi perubahan teks pencarian
        setupSearchListener()

        return view
    }

    /**
     * Fungsi untuk mendeteksi setiap perubahan teks pada EditText pencarian.
     * Pencarian akan dilakukan secara otomatis ketika pengguna mengetik.
     */
    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            // Dipanggil setelah teks berubah
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Jika pengguna mengetik sesuatu → jalankan pencarian
                if (query.isNotEmpty()) {
                    searchTasks(query)
                } else {
                    // Jika input kosong, bersihkan hasil dan sembunyikan teks "tidak ada hasil"
                    taskList.clear()
                    taskAdapter.notifyDataSetChanged()
                    tvNoResults.visibility = View.GONE
                }
            }

            // Tidak digunakan tapi wajib diimplementasi (interface TextWatcher)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /**
     * Fungsi utama untuk mencari data tugas dari Supabase berdasarkan kata kunci.
     * Menggunakan coroutine agar proses berjalan di background thread (tidak freeze UI).
     */
    private fun searchTasks(keyword: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ambil ID pengguna dari SharedPreferences (agar hanya mencari tugas milik user ini)
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)

                // Jika user belum login, tampilkan pesan error
                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        tvNoResults.text = "User belum login!"
                        tvNoResults.visibility = View.VISIBLE
                    }
                    return@launch
                }

                // 🔹 Query ke Supabase (tabel: tasks)
                // Menggunakan operator ilike() agar pencarian tidak case-sensitive
                val results = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)              // hanya ambil task milik user ini
                            ilike("title", "%$keyword%")       // cari task berdasarkan judul
                        }
                    }
                    .decodeList<Task>() // ubah hasil JSON ke list objek Task

                // 🔹 Update UI di main thread
                withContext(Dispatchers.Main) {
                    taskList.clear()
                    taskList.addAll(results)
                    taskAdapter.notifyDataSetChanged()

                    // Jika tidak ada hasil, tampilkan pesan "Tidak ditemukan"
                    tvNoResults.visibility =
                        if (taskList.isEmpty()) View.VISIBLE else View.GONE
                    if (taskList.isEmpty()) tvNoResults.text =
                        "Tidak ditemukan tugas dengan judul \"$keyword\""
                }

            } catch (e: Exception) {
                // Tangani error (misalnya koneksi gagal, atau format data salah)
                withContext(Dispatchers.Main) {
                    tvNoResults.text = "Gagal mencari: ${e.message}"
                    tvNoResults.visibility = View.VISIBLE
                }
            }
        }
    }
}
