package com.example.projectmapgroup7.ui.setting

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.viewmodel.SettingViewModel

/**
 * SettingFragment
 *
 * Fragment ini berfungsi sebagai halaman pengaturan aplikasi.
 * Fitur utama yang disediakan:
 * 1. Mengubah foto profil (kamera / galeri)
 * 2. Mengatur tema aplikasi (Light / Dark Mode)
 * 3. Mengatur notifikasi global
 * 4. Menampilkan informasi akun pengguna
 *
 * Arsitektur: MVVM
 * Upload foto profil ditangani oleh SettingViewModel
 */
class SettingFragment : Fragment() {

    // ViewModel untuk menangani logika upload foto profil
    private val viewModel: SettingViewModel by viewModels()

    // URI gambar profil yang sedang dipilih
    private var currentImageUri: Uri? = null

    // =================================================
    // CAMERA & GALLERY LAUNCHER
    // =================================================

    /**
     * Launcher untuk mengambil foto dari kamera
     * Menghasilkan Bitmap lalu disimpan ke MediaStore
     */
    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            bitmap?.let {
                val uri = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    it,
                    "profile_${System.currentTimeMillis()}",
                    null
                )
                currentImageUri = Uri.parse(uri)
                uploadProfilePicture()
            }
        }

    /**
     * Launcher untuk memilih gambar dari galeri
     */
    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            currentImageUri = uri
            if (uri != null) uploadProfilePicture()
        }

    /**
     * Launcher untuk meminta izin kamera dan galeri
     */
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                showImageSourceDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Izin kamera/galeri diperlukan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // =================================================
    // LIFECYCLE
    // =================================================

    /**
     * Membuat dan menampilkan tampilan SettingFragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_setting.xml
        val view = inflater.inflate(
            R.layout.fragment_setting,
            container,
            false
        )

        // Terapkan tema yang tersimpan
        applySavedTheme()

        // Setup fitur-fitur setting
        setupThemeSwitch(view)
        setupNotificationSwitch(view)
        setupAccountDialog(view)

        // Observer ViewModel
        observeViewModel()

        return view
    }

    // =================================================
    // OBSERVER VIEWMODEL
    // =================================================

    /**
     * Mengamati hasil upload foto profil
     */
    private fun observeViewModel() {
        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { url ->

                // Simpan URL foto profil ke SharedPreferences
                val sharedPref = requireActivity()
                    .getSharedPreferences(
                        "user_session",
                        AppCompatActivity.MODE_PRIVATE
                    )

                sharedPref.edit()
                    .putString("profile_picture", url)
                    .apply()

                Toast.makeText(
                    requireContext(),
                    "Foto profil berhasil diperbarui",
                    Toast.LENGTH_SHORT
                ).show()
            }

            result.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Upload gagal: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =================================================
    // ACCOUNT DIALOG
    // =================================================

    /**
     * Menampilkan dialog akun pengguna
     * Berisi foto profil, username, dan tombol ubah foto
     */
    private fun setupAccountDialog(view: View) {

        val layoutAccount =
            view.findViewById<LinearLayout>(R.id.layoutAccount)

        layoutAccount.setOnClickListener {

            val dialogView =
                layoutInflater.inflate(R.layout.dialog_account, null)

            val imgProfile =
                dialogView.findViewById<ImageView>(R.id.imgProfile)
            val tvUsername =
                dialogView.findViewById<TextView>(R.id.tvUsername)
            val btnChangePhoto =
                dialogView.findViewById<Button>(R.id.btnChangePhoto)

            // Load data user dari SharedPreferences
            loadUserData(imgProfile, tvUsername)

            // Aksi ubah foto profil
            btnChangePhoto.setOnClickListener {
                checkPermissionsAndShowDialog()
            }

            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .show()
        }
    }

    // =================================================
    // LOAD USER DATA
    // =================================================

    /**
     * Memuat username dan foto profil user
     */
    private fun loadUserData(
        imageProfile: ImageView,
        tvUsername: TextView
    ) {

        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                AppCompatActivity.MODE_PRIVATE
            )

        val username =
            sharedPref.getString("username", "Guest")
        val profilePicture =
            sharedPref.getString("profile_picture", null)

        tvUsername.text = username

        if (!profilePicture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicture)
                .circleCrop()
                .into(imageProfile)
        }
    }

    // =================================================
    // PERMISSION HANDLING
    // =================================================

    /**
     * Mengecek izin kamera dan galeri
     * Menyesuaikan dengan versi Android
     */
    private fun checkPermissionsAndShowDialog() {

        val permissionsNeeded = mutableListOf<String>()

        // Izin kamera
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        // Izin galeri (Android 13+ dan di bawahnya)
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(
                permissionsNeeded.toTypedArray()
            )
        }
    }

    // =================================================
    // IMAGE SOURCE DIALOG
    // =================================================

    /**
     * Menampilkan pilihan sumber gambar:
     * Kamera atau Galeri
     */
    private fun showImageSourceDialog() {

        val options = arrayOf(
            "Ambil Foto",
            "Pilih dari Galeri"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    // =================================================
    // UPLOAD FOTO PROFIL
    // =================================================

    /**
     * Mengirim foto profil ke ViewModel untuk diupload
     */
    private fun uploadProfilePicture() {

        val uri = currentImageUri ?: return

        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                AppCompatActivity.MODE_PRIVATE
            )

        val username =
            sharedPref.getString("username", "Guest")
                ?: return

        viewModel.uploadProfilePicture(uri, username)
    }

    // =================================================
    // THEME SETTING
    // =================================================

    /**
     * Switch untuk mengatur Dark / Light Mode
     */
    private fun setupThemeSwitch(view: View) {

        val switchTheme =
            view.findViewById<Switch>(R.id.switchTheme)

        val sharedPref = requireActivity()
            .getSharedPreferences(
                "app_settings",
                AppCompatActivity.MODE_PRIVATE
            )

        val isDarkMode =
            sharedPref.getBoolean("dark_mode", false)

        switchTheme.isChecked = isDarkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit()
                .putBoolean("dark_mode", isChecked)
                .apply()

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    // =================================================
    // NOTIFICATION SETTING
    // =================================================

    /**
     * Switch untuk mengatur notifikasi global
     */
    private fun setupNotificationSwitch(view: View) {

        val switchNotification =
            view.findViewById<Switch>(R.id.switchNotification)

        val sharedPref = requireActivity()
            .getSharedPreferences(
                "app_settings",
                AppCompatActivity.MODE_PRIVATE
            )

        switchNotification.isChecked =
            sharedPref.getBoolean(
                "global_notification",
                true
            )

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Matikan Notifikasi?")
                    .setMessage(
                        "Pengingat deadline tidak akan muncul."
                    )
                    .setPositiveButton("Ya") { _, _ ->
                        sharedPref.edit()
                            .putBoolean(
                                "global_notification",
                                false
                            )
                            .apply()
                        switchNotification.isChecked = false
                    }
                    .setNegativeButton("Batal") { _, _ ->
                        switchNotification.isChecked = true
                    }
                    .show()
            } else {
                sharedPref.edit()
                    .putBoolean(
                        "global_notification",
                        true
                    )
                    .apply()
            }
        }
    }

    /**
     * Menerapkan tema yang tersimpan saat fragment dibuat
     */
    private fun applySavedTheme() {

        val sharedPref = requireActivity()
            .getSharedPreferences(
                "app_settings",
                AppCompatActivity.MODE_PRIVATE
            )

        val isDarkMode =
            sharedPref.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
