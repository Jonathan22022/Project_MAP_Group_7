package com.example.projectmapgroup7.ui.addtask

import android.Manifest
import android.os.Build
import android.app.AlarmManager
import android.util.Log
import android.app.TimePickerDialog
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.databinding.FragmentAddTaskBinding
import com.example.projectmapgroup7.model.Task
import com.example.projectmapgroup7.ml.PriorityPredictor
import com.example.projectmapgroup7.util.DeadlineReceiver
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddTaskFragment : Fragment() {

    // View binding untuk akses langsung ke elemen layout
    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    // Inisialisasi Supabase client dan variabel lainnya
    private val client = SupabaseClientInstance.client
    private val calendar = Calendar.getInstance()
    private var imageUri: Uri? = null
    private lateinit var priorityPredictor: PriorityPredictor  // Model ML untuk prediksi prioritas

    // === Launcher kamera (ambil foto langsung) ===
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        priorityPredictor = PriorityPredictor(requireContext()) // Inisialisasi model TensorFlow Lite

        // Tombol untuk pilih tanggal deadline
        binding.btnPilihTanggal.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(year, month, day)

                // Setelah tanggal dipilih, tampilkan TimePicker
                val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    updateDateTimeText() // tampilkan format yyyy-MM-dd HH:mm:ss
                }

                TimePickerDialog(
                    requireContext(),
                    timeSetListener,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }

            // Tampilkan date picker
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Tombol untuk upload gambar
        binding.btnUploadGambar.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        // Tombol untuk menambahkan tugas
        binding.btnTambahTugas.setOnClickListener {
            tambahTugas()
            checkNotificationPermission()
        }

        return binding.root
    }

    // === Mengecek izin kamera dan galeri sebelum menampilkan dialog sumber gambar ===
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
    }

    // === Fungsi utama untuk menambahkan tugas baru ===
    private fun tambahTugas() {
        val judul = binding.inputJudul.text.toString().trim()
        val deskripsi = binding.inputDeskripsi.text.toString().trim()
        val tanggal = binding.tvTanggal.text.toString().trim()

        // Validasi input
        if (judul.isEmpty() || deskripsi.isEmpty() || tanggal.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
            return
        }

        // Prediksi prioritas tugas menggunakan model ML
        val prioritas = prediksiPrioritas(deskripsi)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Upload gambar ke Supabase Storage jika ada
                val imageUrl = imageUri?.let { uploadImageToSupabase(it, judul) }

                // Ambil ID user dari SharedPreferences
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)
                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Simpan data tugas ke tabel Supabase
                client.postgrest["tasks"].insert(
                    Task(
                        title = judul,
                        description = deskripsi,
                        image_url = imageUrl ?: "",
                        prioritization = prioritas,
                        deadline = tanggal,
                        is_complete = false,
                        id_user = idUser
                    )
                )
                // Mengaktifkan notifikasi reminder deadline
                scheduleDeadlineNotification(judul, tanggal)

                // Notifikasi sukses di UI thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    clearInput() // Reset form
                }
            } catch (e: Exception) {
                // Tampilkan error jika gagal
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // === Upload file gambar ke Supabase Storage ===
    private suspend fun uploadImageToSupabase(uri: Uri, title: String): String? {
        val storage = client.storage.from("task_images") // akses bucket bernama "task_images"
        val fileName = "task_${title}_${System.currentTimeMillis()}.jpg"

        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return null

        // Upload file ke Supabase
        storage.upload(fileName, bytes, upsert = true)
        return storage.publicUrl(fileName) // Kembalikan URL publik gambar
    }

    // === Fungsi prediksi prioritas tugas berdasarkan teks deskripsi ===
    private fun prediksiPrioritas(text: String): String {
        // (sementara) vektor dummy karena model belum dilatih penuh
        val dummyVector = FloatArray(5000) { 0f }
        val resultIndex = priorityPredictor.predictPriority(dummyVector)

        // Koreksi hasil model berdasarkan kata kunci penting di deskripsi
        val lowerText = text.lowercase()
        var score = 0
        if ("penting" in lowerText || "urgent" in lowerText || "segera" in lowerText) score += 2
        if ("hari ini" in lowerText || "deadline" in lowerText) score += 1
        if ("nanti" in lowerText || "santai" in lowerText) score -= 1

        // Skor disesuaikan agar hasil lebih akurat
        val adjustedIndex = when {
            score >= 2 -> 2 // tinggi
            score == 1 -> 1 // sedang
            else -> resultIndex
        }

        // Konversi indeks ke teks prioritas
        return when (adjustedIndex) {
            0 -> "rendah"
            1 -> "sedang"
            2 -> "tinggi"
            else -> "sedang"
        }
    }

    // === Update teks tanggal di tampilan ===
    private fun updateDateTimeText() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        binding.tvTanggal.text = format.format(calendar.time)
    }

    // === Bersihkan form setelah tugas ditambahkan ===
    private fun clearInput() {
        binding.inputJudul.text.clear()
        binding.inputDeskripsi.text.clear()
        binding.previewGambar.setImageResource(0)
        binding.tvTanggal.text = ""
        imageUri = null
    }

    // Membuat notifikasi satu jam sebelum deadline
    private fun scheduleDeadlineNotification(title: String, deadline: String) {
        try {
            Log.d("ScheduleNotif", "===== MULAI SCHEDULE NOTIF =====")
            Log.d("ScheduleNotif", "Input title='$title', deadline='$deadline'")

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(deadline)

            if (date == null) {
                Log.e("ScheduleNotif", "ERROR: Gagal parse deadline string")
                return
            }

            val deadlineMillis = date.time
            val oneHourBefore = deadlineMillis - (60 * 60 * 1000)
            val now = System.currentTimeMillis()

            Log.d("ScheduleNotif", "deadlineMillis = $deadlineMillis")
            Log.d("ScheduleNotif", "oneHourBefore = $oneHourBefore")
            Log.d("ScheduleNotif", "Current time = $now")

            if (oneHourBefore <= now) {
                Log.w("ScheduleNotif", "WARNING: Jadwal notifikasi sudah lewat! Tidak dijalankan.")
            }

            val intent = Intent(requireContext(), DeadlineReceiver::class.java).apply {
                putExtra("title", title)
            }

            val requestCode = title.hashCode()
            Log.d("ScheduleNotif", "PendingIntent requestCode = $requestCode")

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent == null) {
                Log.e("ScheduleNotif", "PendingIntent == NULL, gagal membuat alarm!")
            }

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            Log.d("ScheduleNotif", "setExactAndAllowWhileIdle() dipanggil...")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                oneHourBefore,
                pendingIntent
            )

            Log.d("ScheduleNotif", "===== NOTIFIKASI BERHASIL DIJADWALKAN =====")

        } catch (e: Exception) {
            Log.e("ScheduleNotif", "ERROR saat menjadwalkan notifikasi: ${e.message}", e)
        }
    }

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

    // Hapus binding saat fragment dihancurkan
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}