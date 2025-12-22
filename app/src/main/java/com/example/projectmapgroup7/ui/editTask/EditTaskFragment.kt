package com.example.projectmapgroup7.ui.editTask

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.databinding.FragmentEditTaskBinding
import com.example.projectmapgroup7.viewmodel.EditTaskViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment untuk mengedit data tugas yang sudah ada.
 * Fitur utama:
 * - Mengubah judul, deskripsi, dan deadline tugas
 * - Mengganti atau mempertahankan gambar tugas
 * - Mengambil gambar dari kamera atau galeri
 * - Validasi input sebelum update
 * - Terhubung dengan ViewModel (MVVM)
 */
class EditTaskFragment : Fragment() {

    // ViewBinding untuk mengakses komponen UI dengan aman
    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    // ViewModel untuk menangani logika update tugas
    private val viewModel: EditTaskViewModel by viewModels()

    // Kalender untuk memilih tanggal & waktu deadline
    private val calendar = Calendar.getInstance()

    // URI gambar baru (jika user mengganti gambar)
    private var imageUri: Uri? = null

    // URL gambar lama (jika user tidak mengganti gambar)
    private var originalImageUrl: String? = null

    // ================= CAMERA =================
    /**
     * Launcher untuk mengambil foto menggunakan kamera
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // Menampilkan hasil foto ke ImageView
                binding.previewGambar.setImageURI(imageUri)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Gagal mengambil foto",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ================= GALLERY =================
    /**
     * Launcher untuk memilih gambar dari galeri
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.previewGambar.setImageURI(it)
            }
        }

    // ================= PERMISSION =================
    /**
     * Launcher untuk meminta izin kamera dan galeri
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }.keys

            if (denied.isEmpty()) {
                // Semua izin diberikan
                showImageSourceDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Izin kamera & galeri diperlukan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * Lifecycle Fragment: membuat dan menyiapkan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)

        // Mengisi data awal dari task
        setInitialData()

        // Mengamati hasil update dari ViewModel
        observeViewModel()

        // Menyiapkan listener tombol
        setupListeners()

        return binding.root
    }

    // ================= INIT DATA =================
    /**
     * Mengisi data awal ke form edit berdasarkan arguments
     */
    private fun setInitialData() {
        val title = arguments?.getString("title") ?: ""
        val description = arguments?.getString("description") ?: ""
        val deadline = arguments?.getString("deadline") ?: ""
        originalImageUrl = arguments?.getString("image_url")

        binding.inputJudul.setText(title)
        binding.inputDeskripsi.setText(description)
        binding.tvTanggal.text = deadline

        // Menampilkan gambar lama jika ada
        if (!originalImageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(originalImageUrl)
                .into(binding.previewGambar)
        }
    }

    // ================= OBSERVE VIEWMODEL =================
    /**
     * Mengamati hasil update task dari ViewModel
     */
    private fun observeViewModel() {
        viewModel.editTaskState.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(
                    requireContext(),
                    "Tugas berhasil diupdate!",
                    Toast.LENGTH_SHORT
                ).show()

                // Kembali ke halaman sebelumnya
                findNavController().navigateUp()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    it.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ================= LISTENERS =================
    /**
     * Menyiapkan semua event klik tombol
     */
    private fun setupListeners() {
        binding.btnPilihTanggal.setOnClickListener { showDatePicker() }
        binding.btnUploadGambar.setOnClickListener { checkPermissionsAndShowDialog() }
        binding.btnUpdateTugas.setOnClickListener { updateTask() }
        binding.btnBatal.setOnClickListener { findNavController().navigateUp() }
    }

    // ================= UPDATE TASK =================
    /**
     * Validasi input dan memanggil ViewModel untuk update tugas
     */
    private fun updateTask() {
        val title = binding.inputJudul.text.toString().trim()
        val description = binding.inputDeskripsi.text.toString().trim()
        val deadline = binding.tvTanggal.text.toString().trim()

        // Validasi input
        if (title.isEmpty() || description.isEmpty() || deadline.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Lengkapi semua kolom!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Ambil user ID dari SharedPreferences
        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                android.content.Context.MODE_PRIVATE
            )

        val userId = sharedPref.getString("id_user", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "User belum login!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val originalTitle = arguments?.getString("title") ?: ""

        // Memanggil ViewModel untuk update data
        viewModel.updateTask(
            userId = userId,
            originalTitle = originalTitle,
            title = title,
            description = description,
            deadline = deadline,
            imageUri = imageUri,
            oldImageUrl = originalImageUrl
        )
    }

    // ================= DATE & TIME =================
    /**
     * Menampilkan DatePicker untuk memilih tanggal
     */
    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Menampilkan TimePicker untuk memilih waktu
     */
    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateDateTimeText()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    /**
     * Menampilkan hasil tanggal & waktu ke TextView
     */
    private fun updateDateTimeText() {
        val format =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        binding.tvTanggal.text = format.format(calendar.time)
    }

    // ================= IMAGE =================
    /**
     * Mengecek izin kamera dan galeri
     */
    private fun checkPermissionsAndShowDialog() {
        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    /**
     * Dialog pemilihan sumber gambar (kamera / galeri)
     */
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

    /**
     * Mengambil foto menggunakan kamera
     */
    private fun takePhoto() {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.TITLE,
                "Task_${System.currentTimeMillis()}"
            )
        }

        imageUri = requireContext()
            .contentResolver
            .insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

        imageUri?.let {
            cameraLauncher.launch(it)
        }
    }

    /**
     * Membersihkan binding saat Fragment dihancurkan
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
