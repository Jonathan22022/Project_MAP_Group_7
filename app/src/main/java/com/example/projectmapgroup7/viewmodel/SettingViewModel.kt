package com.example.projectmapgroup7.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * SettingViewModel
 *
 * ViewModel ini bertanggung jawab untuk menangani
 * logika bisnis pada halaman Pengaturan (Setting),
 * khususnya proses upload dan update foto profil pengguna.
 *
 * ViewModel berfungsi sebagai penghubung antara:
 * - UI (SettingFragment)
 * - Repository (UserRepository)
 *
 * Menggunakan AndroidViewModel karena membutuhkan context aplikasi
 * untuk proses upload file.
 */
class SettingViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Repository untuk mengelola data user (Supabase / backend)
    private val repository = UserRepository()

    // =======================
    // STATE UPLOAD FOTO PROFIL
    // =======================

    // LiveData internal untuk menyimpan hasil upload (URL foto profil)
    private val _uploadResult = MutableLiveData<Result<String>>()

    // LiveData yang diekspos ke UI
    // Result<String> berisi:
    // - success -> URL foto profil
    // - failure -> error saat upload
    val uploadResult: LiveData<Result<String>> = _uploadResult

    // =======================
    // PROSES UPLOAD FOTO PROFIL
    // =======================

    /**
     * Mengupload foto profil pengguna ke server.
     *
     * Alur proses:
     * 1. Menerima URI gambar dari kamera / galeri
     * 2. Mengirim data ke UserRepository
     * 3. Mengembalikan URL foto profil jika berhasil
     * 4. Mengirim status ke UI melalui LiveData
     *
     * @param uri      URI gambar foto profil
     * @param username Username pengguna (digunakan sebagai nama file)
     */
    fun uploadProfilePicture(uri: Uri, username: String) {
        viewModelScope.launch {
            try {
                // Upload gambar melalui repository
                val url = repository.uploadProfilePicture(
                    getApplication(),
                    uri,
                    username
                )

                // Kirim hasil sukses ke UI
                _uploadResult.value = Result.success(url)

            } catch (e: Exception) {
                // Kirim hasil gagal ke UI
                _uploadResult.value = Result.failure(e)
            }
        }
    }
}
