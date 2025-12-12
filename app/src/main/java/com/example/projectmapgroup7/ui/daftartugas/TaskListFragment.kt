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
import java.text.SimpleDateFormat
import java.util.Locale

// Fragment ini menampilkan daftar tugas (baik yang masih berjalan maupun yang sudah selesai)
class TaskListFragment : Fragment() {

    // 🔹 Deklarasi komponen UI
    private lateinit var containerTugas: LinearLayout           // Container untuk menampung item tugas
    private lateinit var fabDelete: FloatingActionButton         // Tombol FAB untuk menghapus tugas
    private val selectedTasks = mutableListOf<String>()          // Menyimpan judul tugas yang dipilih user

    private var tabType: String = "progress" // Default tab → menampilkan tugas yang masih dalam progres

    companion object {
        // 🔹 Fungsi pembuat instance baru fragment berdasarkan jenis tab (progress/selesai)
        fun newInstance(tabType: String): TaskListFragment {
            val fragment = TaskListFragment()
            val args = Bundle()
            args.putString("tabType", tabType)
            fragment.arguments = args
            return fragment
        }
    }

    private fun formatDeadline(deadline: String?): String {
        if (deadline.isNullOrEmpty()) return "-"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(deadline)
            if (date != null) outputFormat.format(date) else deadline
        } catch (e: Exception) {
            deadline
        }
    }

    // 🔹 Ambil nilai tabType dari argumen yang dikirim
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabType = arguments?.getString("tabType") ?: "progress"
    }

    // 🔹 Buat dan atur tampilan utama fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)

        // Inisialisasi elemen tampilan dari layout XML
        containerTugas = view.findViewById(R.id.containerTugas)
        fabDelete = view.findViewById(R.id.fabDelete)

        // Saat tombol FAB ditekan → hapus tugas yang dipilih
        fabDelete.setOnClickListener { deleteSelectedTasks() }

        // Muat data tugas dari Supabase
        loadTasks()

        return view
    }

    // 🔹 Fungsi untuk memuat daftar tugas dari database Supabase
    private fun loadTasks() {
        lifecycleScope.launch {
            try {
                // Ambil ID user dari SharedPreferences (penanda pengguna yang login)
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null) ?: return@launch

                // Tentukan apakah tab sedang menampilkan tugas selesai atau belum
                val isComplete = (tabType == "selesai")

                // 🔹 Query ke tabel “tasks” di Supabase
                // Filter berdasarkan id_user dan status is_complete
                val tasks = SupabaseClientInstance.client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            eq("is_complete", isComplete)
                        }
                    }
                    .decodeList<Task>() // Konversi hasil query menjadi list of Task (model data)

                // Tampilkan hasil ke tampilan
                displayTasks(tasks)

            } catch (e: Exception) {
                // Tangani error jika koneksi atau query gagal
                Toast.makeText(requireContext(), "Gagal memuat tugas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 🔹 Fungsi untuk menampilkan daftar tugas ke dalam container (LinearLayout)
    private fun displayTasks(tasks: List<Task>) {
        containerTugas.removeAllViews() // Kosongkan container sebelum menambahkan data baru
        val inflater = LayoutInflater.from(requireContext())

        // Jika tidak ada tugas → tampilkan tampilan kosong
        if (tasks.isEmpty()) {
            val emptyView = inflater.inflate(R.layout.item_empty_task, containerTugas, false)
            containerTugas.addView(emptyView)
            return
        }

        // Loop setiap data tugas dan tampilkan satu per satu
        for (task in tasks) {
            // Gunakan layout item_task.xml untuk setiap item
            val view = inflater.inflate(R.layout.item_task, containerTugas, false)

            // 🔹 Ambil komponen tampilan dari item_task.xml
            val tvTitle = view.findViewById<TextView>(R.id.tvTaskTitle)          // Judul tugas
            val tvDeadline = view.findViewById<TextView>(R.id.tvTaskDeadline)    // Tanggal deadline
            val tvPriority = view.findViewById<TextView>(R.id.tvTaskPriority)    // Prioritas tugas
            val cbSelect = view.findViewById<CheckBox>(R.id.cbSelectTask)        // Checkbox untuk memilih tugas
            val priorityIndicator = view.findViewById<View>(R.id.priorityIndicator) // Warna indikator prioritas

            // 🔹 Tampilkan data dari objek task
            tvTitle.text = task.title
            tvDeadline.text = "Deadline: ${formatDeadline(task.deadline)}"
            setPriorityIndicator(task.prioritization, priorityIndicator, tvPriority) // Atur tampilan prioritas

            // 🔹 Event ketika checkbox diaktifkan atau dimatikan
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTasks.add(task.title)  // Tambahkan judul ke daftar pilihan
                else selectedTasks.remove(task.title)         // Hapus dari daftar jika tidak dicentang

                // Jika ada tugas yang dipilih → tampilkan tombol hapus
                fabDelete.visibility = if (selectedTasks.isNotEmpty()) View.VISIBLE else View.GONE
            }

            // Tambahkan tampilan item ke dalam container utama
            containerTugas.addView(view)
        }
    }

    // 🔹 Fungsi untuk menghapus semua tugas yang dipilih user
    private fun deleteSelectedTasks() {
        lifecycleScope.launch {
            try {
                // Ambil ID user dari SharedPreferences
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null) ?: return@launch

                // 🔹 Hapus setiap tugas berdasarkan judul & id_user
                selectedTasks.forEach { title ->
                    SupabaseClientInstance.client.postgrest["tasks"].delete {
                        filter {
                            eq("title", title)
                            eq("id_user", idUser)
                        }
                    }
                }

                // 🔹 Setelah penghapusan selesai, perbarui UI
                Toast.makeText(requireContext(), "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                selectedTasks.clear()             // Kosongkan daftar tugas terpilih
                fabDelete.visibility = View.GONE  // Sembunyikan tombol hapus
                loadTasks()                       // Muat ulang daftar tugas

            } catch (e: Exception) {
                // Tangani error saat proses hapus gagal
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 Fungsi untuk menampilkan indikator warna dan teks prioritas
    private fun setPriorityIndicator(priority: String, indicator: View, textView: TextView) {
        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_high, null)) // Warna merah
                textView.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_medium, null)) // Warna kuning
                textView.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(resources.getColor(R.color.priority_low, null)) // Warna biru
                textView.text = "Prioritas: Rendah"
            }
            else -> {
                indicator.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null)) // Default abu-abu
                textView.text = "Prioritas: -"
            }
        }
    }
}
