package com.example.projectmapgroup7.ui.addtask

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.databinding.FragmentAddTaskBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private var imageUri: Uri? = null

    private val client = SupabaseClientInstance.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)

        // Pilih tanggal deadline
        binding.btnPilihTanggal.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateText()
            }
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Pilih gambar (kamera atau galeri)
        binding.btnUploadGambar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        // Tombol Tambah Tugas
        binding.btnTambahTugas.setOnClickListener {
            val judul = binding.inputJudul.text.toString()
            val deskripsi = binding.inputDeskripsi.text.toString()
            val tanggal = binding.tvTanggal.text.toString()

            if (judul.isEmpty() || deskripsi.isEmpty() || tanggal.isEmpty()) {
                Toast.makeText(requireContext(), "Lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prioritas = tentukanPrioritas(deskripsi)

            lifecycleScope.launch {
                try {
                    val imageUrl = imageUri?.let { uploadImageToSupabase(it, judul) }

                    client.postgrest["tasks"].insert(
                        mapOf(
                            "title" to judul,
                            "description" to deskripsi,
                            "image_url" to imageUrl,
                            "prioritization" to prioritas,
                            "deadline" to tanggal,
                            "is_complete" to false,
                            "id_user" to "user-uuid-saat-login" // TODO: ubah ke id_user sesungguhnya
                        )
                    )

                    Toast.makeText(requireContext(), "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    clearInput()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        return binding.root
    }

    // Upload gambar ke Supabase Storage
    private suspend fun uploadImageToSupabase(uri: Uri, title: String): String? {
        val storage = client.storage.from("task_images")
        val fileName = "${title}_${System.currentTimeMillis()}.jpg"

        // Baca file menjadi ByteArray
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return null

        // Upload ke Supabase Storage
        storage.upload(fileName, bytes)

        // Kembalikan URL publik gambar
        return storage.publicUrl(fileName)
    }

    // Machine Learning sederhana berdasarkan kata
    private fun tentukanPrioritas(text: String): String {
        val tinggi = listOf("tugas", "kuliah", "kerja", "ujian", "deadline", "sekolah")
        val rendah = listOf("olahraga", "bersih", "belanja", "istirahat")

        val lowerText = text.lowercase()
        return when {
            tinggi.any { lowerText.contains(it) } -> "tinggi"
            rendah.any { lowerText.contains(it) } -> "rendah"
            else -> "rendah"
        }
    }

    private fun updateDateText() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.tvTanggal.text = format.format(calendar.time)
    }

    private fun clearInput() {
        binding.inputJudul.text.clear()
        binding.inputDeskripsi.text.clear()
        binding.previewGambar.setImageResource(0)
        binding.tvTanggal.text = ""
        imageUri = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            binding.previewGambar.setImageURI(imageUri)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}