package com.example.projectmapgroup7.ui.editTask

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.databinding.FragmentEditTaskBinding
import com.example.projectmapgroup7.ml.PriorityPredictor
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!
    private val client = SupabaseClientInstance.client
    private val calendar = Calendar.getInstance()
    private var imageUri: Uri? = null
    private lateinit var priorityPredictor: PriorityPredictor

    private var originalImageUrl: String? = null

    // Launchers untuk ambil gambar - FIXED VERSION
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // Gambar sudah disimpan di imageUri yang kita tentukan sebelumnya
                binding.previewGambar.setImageURI(imageUri)
            } else {
                Toast.makeText(requireContext(), "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.previewGambar.setImageURI(it)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }.keys
            if (denied.isEmpty()) {
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "Izin kamera & galeri diperlukan", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        priorityPredictor = PriorityPredictor(requireContext())

        // Ambil data dari arguments
        val title = arguments?.getString("title") ?: ""
        val description = arguments?.getString("description") ?: ""
        val deadline = arguments?.getString("deadline") ?: ""
        val priority = arguments?.getString("priority") ?: ""
        originalImageUrl = arguments?.getString("image_url")

        // Set data ke form
        binding.inputJudul.setText(title)
        binding.inputDeskripsi.setText(description)
        binding.tvTanggal.text = deadline

        // Load gambar jika ada
        if (!originalImageUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(requireContext())
                .load(originalImageUrl)
                .into(binding.previewGambar)
        }

        // Setup tanggal picker
        binding.btnPilihTanggal.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateText()
            }

            // Parse tanggal yang ada jika tersedia
            if (deadline.isNotEmpty()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = format.parse(deadline)
                    date?.let {
                        calendar.time = it
                    }
                } catch (e: Exception) {
                    // Jika parsing gagal, gunakan tanggal sekarang
                    calendar.time = Date()
                }
            }

            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Tombol pilih gambar
        binding.btnUploadGambar.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        // Tombol update tugas
        binding.btnUpdateTugas.setOnClickListener {
            updateTugas()
        }

        // Tombol batal
        binding.btnBatal.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    private fun checkPermissionsAndShowDialog() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun takePhoto() {
        // Buat URI untuk menyimpan foto
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Task_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Foto tugas")
        }

        imageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        imageUri?.let {
            cameraLauncher.launch(it)
        } ?: run {
            Toast.makeText(requireContext(), "Gagal membuat file untuk foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTugas() {
        val judul = binding.inputJudul.text.toString().trim()
        val deskripsi = binding.inputDeskripsi.text.toString().trim()
        val tanggal = binding.tvTanggal.text.toString().trim()

        if (judul.isEmpty() || deskripsi.isEmpty() || tanggal.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
            return
        }

        // Prediksi prioritas menggunakan model
        val prioritas = prediksiPrioritas(deskripsi)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var imageUrl = originalImageUrl

                // Upload gambar baru jika ada
                if (imageUri != null) {
                    imageUrl = uploadImageToSupabase(imageUri!!, judul)
                }

                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Update data di Supabase berdasarkan judul asli dan user
                // Note: Dalam implementasi real, sebaiknya gunakan task ID
                val originalTitle = arguments?.getString("title") ?: ""

                client.postgrest["tasks"].update(
                    {
                        set("title", judul)
                        set("description", deskripsi)
                        set("image_url", imageUrl ?: "")
                        set("prioritization", prioritas)
                        set("deadline", tanggal)
                    }
                ) {
                    filter {
                        eq("title", originalTitle)
                        eq("id_user", idUser)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tugas berhasil diupdate!", Toast.LENGTH_SHORT).show()
                    // Kembali ke halaman sebelumnya
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal update: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun uploadImageToSupabase(uri: Uri, title: String): String? {
        return try {
            val storage = client.storage.from("task_images")
            val fileName = "task_${title}_${System.currentTimeMillis()}.jpg"

            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return null

            storage.upload(fileName, bytes, upsert = true)
            storage.publicUrl(fileName)
        } catch (e: Exception) {
            null
        }
    }

    private fun prediksiPrioritas(text: String): String {
        // Implementasi prediksi prioritas (sama seperti di AddTaskFragment)
        val dummyVector = FloatArray(5000) { 0f }
        val resultIndex = priorityPredictor.predictPriority(dummyVector)

        val lowerText = text.lowercase()
        var score = 0
        if ("penting" in lowerText || "urgent" in lowerText || "segera" in lowerText) score += 2
        if ("hari ini" in lowerText || "deadline" in lowerText) score += 1
        if ("nanti" in lowerText || "santai" in lowerText) score -= 1

        val adjustedIndex = when {
            score >= 2 -> 2 // tinggi
            score == 1 -> 1 // sedang
            else -> resultIndex // default dari model
        }

        return when (adjustedIndex) {
            0 -> "rendah"
            1 -> "sedang"
            2 -> "tinggi"
            else -> "sedang"
        }
    }

    private fun updateDateText() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.tvTanggal.text = format.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}