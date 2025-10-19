package com.example.projectmapgroup7.DetailTugas

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class DetailTugasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detail_tugas, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        val tvDeadline = view.findViewById<TextView>(R.id.tvDetailDeadline)
        val tvPriority = view.findViewById<TextView>(R.id.tvDetailPriority)
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val btnEdit = view.findViewById<Button>(R.id.btnEditTask)
        val btnMarkAsDone = view.findViewById<Button>(R.id.btnMarkAsDone)

        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val deadline = arguments?.getString("deadline")
        val priority = arguments?.getString("priority")
        val imageUrl = arguments?.getString("image_url")

        tvTitle.text = title
        tvDescription.text = description
        tvDeadline.text = "Deadline: $deadline"
        tvPriority.text = "Prioritas: $priority"

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .into(ivImage)
        }

        // 🔹 Navigasi ke EditTaskFragment
        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putString("title", title)
                putString("description", description)
                putString("deadline", deadline)
                putString("priority", priority)
                putString("image_url", imageUrl)
            }
            findNavController().navigate(R.id.action_detailTugasFragment_to_editTaskFragment, bundle)
        }

        // 🔹 Tombol “Tandai Selesai”
        btnMarkAsDone.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda yakin tugas ini sudah selesai?")
                .setPositiveButton("Ya") { _, _ ->
                    markTaskAsDone(title)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        return view
    }

    private fun markTaskAsDone(title: String?) {
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Judul tugas tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val client = SupabaseClientInstance.client

                // 🔹 Ambil waktu sekarang (dalam format ISO 8601)
                val currentTime = java.time.LocalDateTime.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val formattedTime = currentTime.format(formatter)

                // 🔹 Update kolom is_complete dan completed_at
                client.postgrest["tasks"].update({
                    set("is_complete", true)
                    set("completed_at", formattedTime)
                }) {
                    filter {
                        eq("title", title)
                        eq("id_user", idUser)
                    }
                }

                Toast.makeText(requireContext(), "Tugas ditandai selesai ✅", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menandai tugas selesai: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
