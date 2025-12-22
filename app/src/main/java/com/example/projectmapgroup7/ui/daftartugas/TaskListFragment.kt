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

/**
 * Fragment yang menampilkan daftar tugas
 * Baik tugas yang masih berjalan (IN PROGRESS)
 * maupun tugas yang sudah selesai (SELESAI)
 */
class TaskListFragment : Fragment() {

    // LinearLayout sebagai container untuk menampung daftar task secara dinamis
    private lateinit var containerTugas: LinearLayout

    // FloatingActionButton untuk menghapus task yang dipilih
    private lateinit var fabDelete: FloatingActionButton

    // ViewModel untuk mengelola data task
    private val viewModel: TaskViewModel by viewModels()

    // Menyimpan judul task yang dipilih untuk dihapus
    private val selectedTasks = mutableListOf<String>()

    // Menentukan apakah fragment menampilkan task selesai atau belum
    private var isComplete = false

    /**
     * Companion object untuk mengirim parameter isComplete ke fragment
     */
    companion object {
        fun newInstance(isComplete: Boolean): TaskListFragment {
            return TaskListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isComplete", isComplete)
                }
            }
        }
    }

    /**
     * Mengambil nilai isComplete dari arguments
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isComplete = arguments?.getBoolean("isComplete") ?: false
    }

    /**
     * Lifecycle Fragment untuk membuat tampilan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_task_list.xml
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)

        // Inisialisasi komponen UI
        containerTugas = view.findViewById(R.id.containerTugas)
        fabDelete = view.findViewById(R.id.fabDelete)

        // Event klik tombol hapus
        fabDelete.setOnClickListener {
            deleteSelected()
        }

        // Observasi perubahan data dari ViewModel
        observeViewModel()

        // Memuat data task sesuai status
        loadTasks()

        return view
    }

    /**
     * Mengambil task dari database berdasarkan user dan status task
     */
    private fun loadTasks() {

        // Mengambil userId dari SharedPreferences
        val pref = requireActivity()
            .getSharedPreferences(
                "user_session",
                android.content.Context.MODE_PRIVATE
            )

        val userId = pref.getString("id_user", null) ?: return

        // Meminta ViewModel untuk memuat task
        viewModel.loadTasks(userId, isComplete)
    }

    /**
     * Mengamati LiveData dari ViewModel
     */
    private fun observeViewModel() {

        // Observer daftar task
        viewModel.tasks.observe(viewLifecycleOwner) {
            displayTasks(it)
        }

        // Observer error
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Menghapus task yang dipilih oleh user
     */
    private fun deleteSelected() {

        // Ambil userId dari SharedPreferences
        val pref = requireActivity()
            .getSharedPreferences(
                "user_session",
                android.content.Context.MODE_PRIVATE
            )

        val userId = pref.getString("id_user", null) ?: return

        // Panggil ViewModel untuk menghapus task
        viewModel.deleteTasks(userId, selectedTasks) {
            selectedTasks.clear()
            fabDelete.visibility = View.GONE
            loadTasks()
        }
    }

    /**
     * Menampilkan daftar task ke dalam UI
     */
    private fun displayTasks(tasks: List<Task>) {

        // Bersihkan container sebelum menampilkan ulang
        containerTugas.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        // Jika tidak ada task, tampilkan tampilan kosong
        if (tasks.isEmpty()) {
            containerTugas.addView(
                inflater.inflate(
                    R.layout.item_empty_task,
                    containerTugas,
                    false
                )
            )
            return
        }

        // Menampilkan setiap task
        tasks.forEach { task ->
            val view = inflater.inflate(
                R.layout.item_task_daftar,
                containerTugas,
                false
            )

            // Inisialisasi komponen item task
            val tvTitle = view.findViewById<TextView>(R.id.tvTaskTitle)
            val tvDeadline = view.findViewById<TextView>(R.id.tvTaskDeadline)
            val tvPriority = view.findViewById<TextView>(R.id.tvTaskPriority)
            val cbSelect = view.findViewById<CheckBox>(R.id.cbSelectTask)
            val indicator = view.findViewById<View>(R.id.priorityIndicator)

            // Set data task ke UI
            tvTitle.text = task.title
            tvDeadline.text = "Deadline: ${task.deadline}"

            // Checkbox untuk memilih task yang akan dihapus
            cbSelect.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedTasks.add(task.title)
                } else {
                    selectedTasks.remove(task.title)
                }

                // Tampilkan atau sembunyikan tombol hapus
                fabDelete.visibility =
                    if (selectedTasks.isNotEmpty())
                        View.VISIBLE
                    else
                        View.GONE
            }

            // Set indikator prioritas task
            setPriorityIndicator(
                task.prioritization,
                indicator,
                tvPriority
            )

            // Tambahkan item ke container
            containerTugas.addView(view)
        }
    }

    /**
     * Mengatur warna indikator dan teks prioritas
     */
    private fun setPriorityIndicator(
        priority: String,
        indicator: View,
        tv: TextView
    ) {
        when (priority.lowercase()) {
            "tinggi" -> {
                indicator.setBackgroundColor(
                    resources.getColor(R.color.priority_high, null)
                )
                tv.text = "Prioritas: Tinggi"
            }
            "sedang" -> {
                indicator.setBackgroundColor(
                    resources.getColor(R.color.priority_medium, null)
                )
                tv.text = "Prioritas: Sedang"
            }
            "rendah" -> {
                indicator.setBackgroundColor(
                    resources.getColor(R.color.priority_low, null)
                )
                tv.text = "Prioritas: Rendah"
            }
        }
    }
}
