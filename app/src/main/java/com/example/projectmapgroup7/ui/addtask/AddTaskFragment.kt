package com.example.projectmapgroup7.ui.addtask

import android.Manifest
import android.os.Build
import android.util.Log
import android.app.TimePickerDialog
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projectmapgroup7.databinding.FragmentAddTaskBinding
import androidx.lifecycle.ViewModelProvider
import com.example.projectmapgroup7.viewmodel.AddTaskViewModel
import java.text.SimpleDateFormat
import java.util.*

class AddTaskFragment : Fragment() {

    // ViewBinding untuk FragmentAddTask
    // _binding digunakan untuk lifecycle Fragment (di-null saat onDestroyView)
    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    // ViewModel untuk mengelola logika penambahan task
    private lateinit var viewModel: AddTaskViewModel

    // Calendar untuk menyimpan tanggal & waktu deadline
    private val calendar = Calendar.getInstance()

    // URI gambar task (kamera / galeri)
    private var imageUri: Uri? = null

    /**
     * Launcher kamera
     * Mengambil foto dari kamera dan mengembalikan bitmap preview
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // Simpan bitmap ke MediaStore dan ambil URI-nya
                val uri = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    it,
                    "task_${System.currentTimeMillis()}",
                    null
                )
                imageUri = Uri.parse(uri)

                // Tampilkan foto ke ImageView preview
                binding.previewGambar.setImageURI(imageUri)
            }
        }

    /**
     * Launcher galeri
     * Mengambil gambar dari penyimpanan perangkat
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.previewGambar.setImageURI(it)
            }
        }

    /**
     * Launcher permintaan izin kamera & galeri
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }.keys
            if (denied.isEmpty()) {
                // Jika semua izin diberikan, tampilkan dialog pilihan sumber gambar
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
     * Callback hasil permintaan izin (khusus izin notifikasi)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2001) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("AddTask", "Izin notifikasi DISETUJUI")
            } else {
                Log.w("AddTask", "Izin notifikasi DITOLAK")
            }
        }
    }

    /**
     * Mengecek izin kamera dan galeri sebelum memilih gambar
     */
    private fun checkPermissionsAndShowDialog() {
        val permissionsNeeded = mutableListOf<String>()

        // Cek izin kamera
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Cek izin membaca gambar (berbeda antara Android 13+ dan versi lama)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Jika semua izin sudah diberikan
        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    /**
     * Dialog untuk memilih sumber gambar (kamera atau galeri)
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    /**
     * Mengecek izin notifikasi (Android 13+)
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showNotificationPermissionDialog()
            }
        }
    }

    /**
     * Dialog permintaan izin notifikasi
     */
    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Izin Notifikasi Dibutuhkan")
            .setMessage(
                "Agar deadline reminder dapat muncul, aplikasi membutuhkan izin notifikasi."
            )
            .setPositiveButton("Izinkan") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Lifecycle Fragment untuk membuat tampilan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inisialisasi ViewBinding
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this)[AddTaskViewModel::class.java]

        // Setup date & time picker
        setupDateTimePicker()

        // Setup observer LiveData
        setupObservers()

        // Klik tombol upload gambar
        binding.btnUploadGambar.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        // Klik tombol tambah tugas
        binding.btnTambahTugas.setOnClickListener {
            submitTask()
            checkNotificationPermission()
        }

        return binding.root
    }

    /**
     * Mengirim data task ke ViewModel
     */
    private fun submitTask() {
        val title = binding.inputJudul.text.toString().trim()
        val desc = binding.inputDeskripsi.text.toString().trim()
        val deadline = binding.tvTanggal.text.toString().trim()

        // Validasi input
        if (title.isEmpty() || desc.isEmpty() || deadline.isEmpty()) {
            toast("Lengkapi semua kolom!")
            return
        }

        // Ambil userId dari SharedPreferences
        val userId = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("id_user", null)

        if (userId.isNullOrEmpty()) {
            toast("User belum login!")
            return
        }

        // Panggil ViewModel untuk menambahkan task
        viewModel.addTask(title, desc, deadline, imageUri, userId)
    }

    /**
     * Observer untuk status penambahan task
     */
    private fun setupObservers() {
        viewModel.addTaskState.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                toast("Tugas berhasil ditambahkan!")
                clearInput()
            }
            result.onFailure {
                toast(it.message ?: "Terjadi kesalahan")
            }
        }
    }

    /**
     * Setup DatePicker & TimePicker untuk deadline
     */
    private fun setupDateTimePicker() {
        binding.btnPilihTanggal.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    calendar.set(y, m, d)
                    TimePickerDialog(
                        requireContext(),
                        { _, h, min ->
                            calendar.set(Calendar.HOUR_OF_DAY, h)
                            calendar.set(Calendar.MINUTE, min)
                            calendar.set(Calendar.SECOND, 0)
                            updateDateTimeText()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Menampilkan tanggal & waktu yang dipilih ke TextView
     */
    private fun updateDateTimeText() {
        val sdf = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )
        binding.tvTanggal.text = sdf.format(calendar.time)
    }

    /**
     * Mengosongkan semua input setelah task berhasil ditambahkan
     */
    private fun clearInput() {
        binding.inputJudul.text.clear()
        binding.inputDeskripsi.text.clear()
        binding.previewGambar.setImageDrawable(null)
        binding.tvTanggal.text = ""
        imageUri = null
    }

    /**
     * Helper untuk menampilkan Toast
     */
    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Membersihkan binding saat Fragment dihancurkan
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
