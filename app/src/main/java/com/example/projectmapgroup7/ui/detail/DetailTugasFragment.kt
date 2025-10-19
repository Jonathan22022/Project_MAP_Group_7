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

// Fragment ini digunakan untuk menampilkan detail sebuah tugas (Task)
class DetailTugasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout XML fragment_detail_tugas untuk fragment ini
        val view = inflater.inflate(R.layout.fragment_detail_tugas, container, false)

        // 🔹 Inisialisasi komponen UI dari layout
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)          // Teks judul tugas
        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription) // Teks deskripsi tugas
        val tvDeadline = view.findViewById<TextView>(R.id.tvDetailDeadline)    // Teks deadline
        val tvPriority = view.findViewById<TextView>(R.id.tvDetailPriority)    // Teks prioritas
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)         // Gambar tugas (jika ada)
        val btnEdit = view.findViewById<Button>(R.id.btnEditTask)              // Tombol untuk edit tugas
        val btnMarkAsDone = view.findViewById<Button>(R.id.btnMarkAsDone)      // Tombol tandai selesai

        // 🔹 Ambil data dari arguments (data dikirim dari fragment sebelumnya)
        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val deadline = arguments?.getString("deadline")
        val priority = arguments?.getString("priority")
        val imageUrl = arguments?.getString("image_url")

        // 🔹 Tampilkan data ke komponen UI
        tvTitle.text = title
        tvDescription.text = description
        tvDeadline.text = "Deadline: $deadline"
        tvPriority.text = "Prioritas: $priority"

        // 🔹 Jika ada gambar, tampilkan dengan Glide (library untuk load gambar dari URL)
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .into(ivImage)
        }

        // 🔹 Navigasi ke EditTaskFragment saat tombol Edit diklik
        btnEdit.setOnClickListener {
            // Kirim data ke fragment edit menggunakan Bundle
            val bundle = Bundle().apply {
                putString("title", title)
                putString("description", description)
                putString("deadline", deadline)
                putString("priority", priority)
                putString("image_url", imageUrl)
            }
            // Pindah ke fragment EditTaskFragment dengan membawa data
            findNavController().navigate(R.id.action_detailTugasFragment_to_editTaskFragment, bundle)
        }

        // 🔹 Saat tombol “Tandai Selesai” diklik, tampilkan dialog konfirmasi
        btnMarkAsDone.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda yakin tugas ini sudah selesai?")
                // Jika user memilih “Ya”, jalankan fungsi markTaskAsDone()
                .setPositiveButton("Ya") { _, _ ->
                    markTaskAsDone(title)
                }
                // Jika memilih “Batal”, tutup dialog
                .setNegativeButton("Batal", null)
                .show()
        }

        return view // Kembalikan tampilan fragment
    }

    // 🔹 Fungsi untuk menandai tugas sebagai selesai
    private fun markTaskAsDone(title: String?) {
        // Pastikan judul tidak kosong (karena akan digunakan untuk update database)
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Judul tugas tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Jalankan proses update di background menggunakan coroutine
        lifecycleScope.launch {
            try {
                // Ambil id_user dari SharedPreferences (data login pengguna)
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 🔹 Ambil instance Supabase client
                val client = SupabaseClientInstance.client

                // 🔹 Ambil waktu saat ini dalam format "yyyy-MM-dd HH:mm:ss"
                val currentTime = java.time.LocalDateTime.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val formattedTime = currentTime.format(formatter)

                // 🔹 Update data di tabel "tasks" Supabase
                //     - is_complete → true (tugas selesai)
                //     - completed_at → waktu sekarang
                //     - Filter berdasarkan title & id_user agar hanya tugas milik user yang diubah
                client.postgrest["tasks"].update({
                    set("is_complete", true)
                    set("completed_at", formattedTime)
                }) {
                    filter {
                        eq("title", title)
                        eq("id_user", idUser)
                    }
                }

                // Tampilkan notifikasi sukses ke pengguna
                Toast.makeText(requireContext(), "Tugas ditandai selesai ✅", Toast.LENGTH_SHORT).show()

                // Kembali ke halaman sebelumnya setelah update
                findNavController().navigateUp()

            } catch (e: Exception) {
                // Tangkap error jika update gagal
                Toast.makeText(requireContext(), "Gagal menandai tugas selesai: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
