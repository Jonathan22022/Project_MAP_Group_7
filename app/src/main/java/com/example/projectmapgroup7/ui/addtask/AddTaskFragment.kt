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

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddTaskViewModel
    private val calendar = Calendar.getInstance()
    private var imageUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // Simpan hasil foto ke MediaStore dan dapatkan URI-nya
                val uri = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    it,
                    "task_${System.currentTimeMillis()}",
                    null
                )
                imageUri = Uri.parse(uri)
                binding.previewGambar.setImageURI(imageUri) // tampilkan foto di ImageView
            }
        }

    // === Launcher galeri (ambil gambar dari penyimpanan) ===
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.previewGambar.setImageURI(it)
            }
        }

    // === Launcher untuk meminta izin kamera/galeri ===
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }.keys
            if (denied.isEmpty()) {
                showImageSourceDialog() // jika semua izin diberikan
            } else {
                Toast.makeText(requireContext(), "Izin kamera & galeri diperlukan", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("AddTask", "Izin notifikasi DISETUJUI")
            } else {
                Log.w("AddTask", "Izin notifikasi DITOLAK")
            }
        }
    }
    private fun checkPermissionsAndShowDialog() {
        val permissionsNeeded = mutableListOf<String>()

        // Cek izin kamera
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Cek izin membaca gambar (berbeda untuk Android 13+)
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

        // Jika sudah diizinkan semua → tampilkan dialog pilih sumber gambar
        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    // === Dialog untuk memilih sumber gambar ===
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
    }private fun checkNotificationPermission() {
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

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Izin Notifikasi Dibutuhkan")
            .setMessage("Agar deadline reminder dapat muncul, aplikasi membutuhkan izin notifikasi.")
            .setPositiveButton("Izinkan") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddTaskViewModel::class.java]

        setupDateTimePicker()
        setupObservers()

        binding.btnUploadGambar.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        binding.btnTambahTugas.setOnClickListener {
            submitTask()
            checkNotificationPermission()
        }

        return binding.root
    }

    private fun submitTask() {
        val title = binding.inputJudul.text.toString().trim()
        val desc = binding.inputDeskripsi.text.toString().trim()
        val deadline = binding.tvTanggal.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty() || deadline.isEmpty()) {
            toast("Lengkapi semua kolom!")
            return
        }

        val userId = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("id_user", null)

        if (userId.isNullOrEmpty()) {
            toast("User belum login!")
            return
        }

        viewModel.addTask(title, desc, deadline, imageUri, userId)
    }

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

    // ===== DATE TIME =====
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

    private fun updateDateTimeText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        binding.tvTanggal.text = sdf.format(calendar.time)
    }

    private fun clearInput() {
        binding.inputJudul.text.clear()
        binding.inputDeskripsi.text.clear()
        binding.previewGambar.setImageDrawable(null)
        binding.tvTanggal.text = ""
        imageUri = null
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
