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

    // Instance Supabase untuk koneksi database dan storage
    private val client = SupabaseClientInstance.client

    // Calendar digunakan untuk menyimpan tanggal deadline
    private val calendar = Calendar.getInstance()

    // URI untuk gambar (foto tugas)
    private var imageUri: Uri? = null

    // Model machine learning untuk memprediksi prioritas tugas
    private lateinit var priorityPredictor: PriorityPredictor

    // URL gambar lama (sebelum diedit)
    private var originalImageUrl: String? = null

    /**
     * Launcher untuk mengambil foto dari kamera.
     * Jika berhasil, hasil foto disimpan di imageUri.
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                binding.previewGambar.setImageURI(imageUri)
            } else {
                Toast.makeText(requireContext(), "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Launcher untuk memilih gambar dari galeri.
     * Menyimpan URI gambar yang dipilih.
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.previewGambar.setImageURI(it)
            }
        }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = android.app.TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                updateDateTimeText()
            },
            hour, minute, true
        )
        timePicker.show()
    }

    /**
     * Launcher untuk meminta izin kamera & galeri.
     * Jika ditolak, menampilkan peringatan.
     */
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

        // Mengambil data tugas dari arguments (data dikirim dari fragment sebelumnya)
        val title = arguments?.getString("title") ?: ""
        val description = arguments?.getString("description") ?: ""
        val deadline = arguments?.getString("deadline") ?: ""
        val priority = arguments?.getString("priority") ?: ""
        originalImageUrl = arguments?.getString("image_url")

        // Menampilkan data lama ke input form
        binding.inputJudul.setText(title)
        binding.inputDeskripsi.setText(description)
        binding.tvTanggal.text = deadline

        // Menampilkan gambar lama (jika ada)
        if (!originalImageUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(requireContext())
                .load(originalImageUrl)
                .into(binding.previewGambar)
        }

        // Tombol untuk memilih tanggal deadline
        binding.btnPilihTanggal.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(year, month, day)
                showTimePicker()
            }

            // Jika ada tanggal lama, set tanggal tersebut di date picker
            if (deadline.isNotEmpty()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    calendar.time = format.parse(deadline)!!
                } catch (e: Exception) {
                    calendar.time = Date()
                }
            }

            // Menampilkan DatePicker dialog
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Tombol upload gambar (kamera/galeri)
        binding.btnUploadGambar.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        // Tombol update tugas
        binding.btnUpdateTugas.setOnClickListener {
            updateTugas()
        }

        // Tombol batal kembali ke halaman sebelumnya
        binding.btnBatal.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    /**
     * Mengecek apakah izin kamera & penyimpanan sudah diberikan.
     * Jika belum, meminta izin. Jika sudah, tampilkan dialog pemilihan sumber gambar.
     */
    private fun checkPermissionsAndShowDialog() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Untuk Android 13 ke atas, gunakan READ_MEDIA_IMAGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Untuk Android versi lama, gunakan READ_EXTERNAL_STORAGE
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

    /**
     * Menampilkan dialog untuk memilih sumber gambar:
     * Kamera atau Galeri.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()             // Kamera
                    1 -> galleryLauncher.launch("image/*")  // Galeri
                }
            }
            .show()
    }

    /**
     * Membuka kamera dan menyimpan hasilnya ke MediaStore.
     */
    private fun takePhoto() {
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

    /**
     * Fungsi utama untuk mengupdate data tugas ke Supabase.
     */
    private fun updateTugas() {
        val judul = binding.inputJudul.text.toString().trim()
        val deskripsi = binding.inputDeskripsi.text.toString().trim()
        val tanggal = binding.tvTanggal.text.toString().trim()

        // Validasi input form
        if (judul.isEmpty() || deskripsi.isEmpty() || tanggal.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
            return
        }

        // Prediksi prioritas berdasarkan deskripsi
        val prioritas = prediksiPrioritas(deskripsi)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var imageUrl = originalImageUrl

                // Jika user memilih gambar baru → upload ke Supabase Storage
                if (imageUri != null) {
                    imageUrl = uploadImageToSupabase(imageUri!!, judul)
                }

                // Ambil ID user dari SharedPreferences
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Update data tugas di tabel Supabase
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

                // Setelah berhasil, tampilkan notifikasi dan kembali ke halaman sebelumnya
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tugas berhasil diupdate!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal update: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Mengupload gambar ke Supabase Storage.
     * Mengembalikan URL publik dari gambar yang diunggah.
     */
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

    /**
     * Fungsi prediksi prioritas tugas berdasarkan teks deskripsi.
     * Menggunakan kombinasi hasil model ML dan rule sederhana berbasis kata kunci.
     */
    private fun prediksiPrioritas(text: String): String {
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

    /**
     * Mengupdate tampilan tanggal di TextView sesuai pilihan pengguna.
     */
    private fun updateDateTimeText() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        binding.tvTanggal.text = format.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
