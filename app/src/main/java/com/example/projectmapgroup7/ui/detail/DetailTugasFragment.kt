package com.example.projectmapgroup7.DetailTugas

import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.projectmapgroup7.viewmodel.DetailTugasViewModel

/**
 * Fragment untuk menampilkan detail lengkap dari sebuah tugas
 * Meliputi judul, deskripsi, deadline, prioritas, gambar,
 * serta aksi edit, tandai selesai, dan pengaturan notifikasi.
 */
class DetailTugasFragment : Fragment() {

    // ViewModel untuk menangani logika detail tugas
    private val viewModel: DetailTugasViewModel by viewModels()

    /**
     * Lifecycle Fragment untuk membuat tampilan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_detail_tugas.xml
        val view = inflater.inflate(
            R.layout.fragment_detail_tugas,
            container,
            false
        )

        // Inisialisasi komponen UI
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvDetailDescription)
        val tvDeadline = view.findViewById<TextView>(R.id.tvDetailDeadline)
        val tvPriority = view.findViewById<TextView>(R.id.tvDetailPriority)
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val btnDone = view.findViewById<Button>(R.id.btnMarkAsDone)
        val btnEdit = view.findViewById<Button>(R.id.btnEditTask)
        val switchNotif = view.findViewById<Switch>(R.id.switchNotification)

        // Mengambil data task dari arguments
        val title = arguments?.getString("title") ?: return view
        val deadline = arguments?.getString("deadline")

        // Menampilkan data task ke UI
        tvTitle.text = title
        tvDesc.text = arguments?.getString("description")
        tvDeadline.text = "Deadline: ${deadline ?: "-"}"
        tvPriority.text = "Prioritas: ${arguments?.getString("priority")}"

        // Menampilkan gambar task jika tersedia
        arguments?.getString("image_url")?.let {
            Glide.with(this)
                .load(it)
                .into(ivImage)
        }

        /**
         * Tombol Edit Task
         * Navigasi ke EditTaskFragment dengan membawa data task
         */
        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putString("title", arguments?.getString("title"))
                putString("description", arguments?.getString("description"))
                putString("deadline", arguments?.getString("deadline"))
                putString("priority", arguments?.getString("priority"))
                putString("image_url", arguments?.getString("image_url"))
            }

            findNavController().navigate(
                R.id.action_detailTugasFragment_to_editTaskFragment,
                bundle
            )
        }

        /**
         * Tombol Mark As Done
         * Menandai task sebagai selesai
         */
        btnDone.setOnClickListener {
            val pref = requireActivity()
                .getSharedPreferences(
                    "user_session",
                    Context.MODE_PRIVATE
                )

            val userId = pref.getString("id_user", null)
                ?: return@setOnClickListener

            // Memanggil ViewModel untuk update status task
            viewModel.markTaskDone(userId, title)
        }

        /**
         * Observer status task selesai
         */
        viewModel.doneSuccess.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    requireContext(),
                    "Tugas selesai ✅",
                    Toast.LENGTH_SHORT
                ).show()

                // Kembali ke halaman sebelumnya
                findNavController().navigateUp()
            }
        }

        /**
         * Pengaturan notifikasi deadline task
         */
        val notifPref = requireActivity()
            .getSharedPreferences(
                "task_notifications",
                Context.MODE_PRIVATE
            )

        // Ambil status notifikasi sebelumnya
        switchNotif.isChecked =
            notifPref.getBoolean("${title}_notif", true)

        // Listener perubahan switch notifikasi
        switchNotif.setOnCheckedChangeListener { _, checked ->

            // Simpan preferensi notifikasi
            notifPref.edit()
                .putBoolean("${title}_notif", checked)
                .apply()

            if (checked && deadline != null) {
                // Jadwalkan notifikasi deadline
                viewModel.scheduleNotification(
                    requireContext(),
                    title,
                    deadline
                )
            } else {
                // Batalkan notifikasi deadline
                viewModel.cancelNotification(
                    requireContext(),
                    title
                )
            }
        }

        return view
    }
}
