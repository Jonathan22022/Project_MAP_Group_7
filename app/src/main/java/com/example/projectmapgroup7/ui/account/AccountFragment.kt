package com.example.projectmapgroup7.ui.account

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AccountFragment : Fragment() {

    // Deklarasi elemen UI
    private lateinit var imageProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvAccountAge: TextView
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView

    // Variabel untuk menyimpan foto profil saat ini
    private var currentImageUri: Uri? = null

    // Inisialisasi Supabase client (digunakan untuk koneksi ke database & storage)
    private val client by lazy { SupabaseClientInstance.client }

    // --- 📸 Registrasi launcher kamera ---
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // Simpan hasil foto kamera ke penyimpanan dan ubah ke URI
                val uri = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    it,
                    "profile_${System.currentTimeMillis()}",
                    null
                )
                currentImageUri = Uri.parse(uri)
                // Upload ke Supabase setelah foto diambil
                uploadProfilePicture()
            }
        }

    // --- 🖼️ Registrasi launcher galeri ---
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            currentImageUri = uri
            if (uri != null) uploadProfilePicture() // Jika user memilih gambar, langsung upload
        }

    // --- 🛡️ Registrasi launcher untuk meminta izin kamera dan storage ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }.keys
            if (denied.isEmpty()) {
                // Semua izin diberikan → tampilkan pilihan sumber gambar
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "Izin kamera/galeri diperlukan", Toast.LENGTH_SHORT).show()
            }
        }

    // Inflate layout fragment_account.xml
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Hubungkan variabel dengan elemen di layout
        imageProfile = view.findViewById(R.id.imageProfile)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvAccountAge = view.findViewById(R.id.tvAccountAge)
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks)

        // Muat data user (nama, foto, dll)
        loadUserData()

        // Klik pada foto profil → tampilkan pilihan ambil/pilih gambar
        imageProfile.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        return view
    }

    // 🔒 Mengecek apakah izin kamera & galeri sudah diberikan
    private fun checkPermissionsAndShowDialog() {
        val permissionsNeeded = mutableListOf<String>()

        // Tambahkan izin kamera jika belum diberikan
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Untuk Android 13+ gunakan READ_MEDIA_IMAGES, versi lama gunakan READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Jika semua izin sudah ada, tampilkan dialog pilihan sumber gambar
        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog()
        } else {
            // Jika belum → minta izin ke pengguna
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    // 📷 Menampilkan dialog untuk memilih sumber gambar (kamera atau galeri)
    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)      // Pilihan ambil foto
                    1 -> galleryLauncher.launch("image/*") // Pilihan dari galeri
                }
            }
            .show()
    }

    // 🚀 Fungsi untuk upload foto profil ke Supabase Storage
    private fun uploadProfilePicture() {
        val uri = currentImageUri ?: return
        val username = tvUsername.text.toString()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Baca gambar dari URI sebagai byte array
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val fileName = "profile_${username}_${System.currentTimeMillis()}.jpg"

                // Upload ke bucket "profile_pictures" di Supabase Storage
                val storage = client.storage.from("profile_pictures")
                val imageBytes = inputStream!!.readBytes()
                storage.upload(fileName, imageBytes, upsert = true)

                // Ambil URL publik dari file yang baru diupload
                val publicUrl = storage.publicUrl(fileName)

                // Update kolom `profile_picture` di tabel users berdasarkan username
                client.postgrest["users"].update(
                    {
                        set("profile_picture", publicUrl)
                    }
                ) {
                    filter { eq("username", username) }
                }

                // Simpan URL gambar baru ke SharedPreferences (session user)
                val sharedPref = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
                sharedPref.edit().putString("profile_picture", publicUrl).apply()

                // Update tampilan UI di thread utama
                withContext(Dispatchers.Main) {
                    Glide.with(requireContext())
                        .load(publicUrl)
                        .placeholder(R.drawable.ic_account_)
                        .circleCrop()
                        .into(imageProfile)

                    Toast.makeText(requireContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                // Jika gagal upload
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mengupload foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🧠 Muat data user dari session & database Supabase
    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
        val username = sharedPref.getString("username", "Guest")
        val profilePicture = sharedPref.getString("profile_picture", null)

        tvUsername.text = username

        // Tampilkan foto profil (dari URL jika tersedia)
        if (!profilePicture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicture)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageProfile)
        }

        // Jika user bukan Guest → ambil data tambahan dari Supabase
        if (username != "Guest") {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Query data user dari tabel `users`
                    val userData = client.postgrest["users"]
                        .select {
                            filter { eq("username", username!!) }
                        }
                        .decodeSingle<Map<String, Any>>()

                    // Hitung usia akun berdasarkan created_at
                    val createdAt = userData["created_at"]?.toString()
                    val accountAgeText = if (createdAt != null) {
                        calculateAccountAge(createdAt)
                    } else "- hari"

                    // Update teks di UI
                    withContext(Dispatchers.Main) {
                        tvAccountAge.text = "Usia Akun: $accountAgeText"
                        tvTotalTasks.text = "Total Tugas: 0"
                        tvCompletedTasks.text = "Tugas Selesai: 0"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        tvAccountAge.text = "Usia Akun: - hari"
                    }
                }
            }
        }
    }

    // ⏳ Hitung selisih hari antara tanggal pembuatan akun dan sekarang
    private fun calculateAccountAge(createdAt: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val createdDate = formatter.parse(createdAt)
            val now = Date()
            val diff = now.time - (createdDate?.time ?: now.time)
            val days = diff / (1000 * 60 * 60 * 24)
            "$days hari"
        } catch (e: Exception) {
            "- hari"
        }
    }
}
