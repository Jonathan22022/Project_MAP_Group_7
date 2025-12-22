package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.launch

/**
 * HomeViewModel
 *
 * ViewModel untuk halaman Home.
 * Bertugas mengambil dan mengelola daftar task aktif (belum selesai)
 * milik user yang sedang login.
 *
 * Menggunakan arsitektur MVVM agar:
 * - UI (Fragment) terpisah dari logika bisnis
 * - Data bersifat lifecycle-aware
 */
class HomeViewModel : ViewModel() {

    // Repository sebagai sumber data (Supabase / database)
    private val repository = TaskRepository()

    // =======================
    // STATE DATA TASK
    // =======================

    // Menyimpan daftar task aktif (is_complete = false)
    private val _tasks = MutableLiveData<List<Task>>()

    // LiveData yang diamati oleh UI (HomeFragment)
    val tasks: LiveData<List<Task>> = _tasks

    // =======================
    // STATE MESSAGE
    // =======================

    // Digunakan untuk menampilkan status:
    // - loading
    // - data kosong
    // - error
    private val _message = MutableLiveData<String?>()

    // LiveData message untuk UI
    val message: LiveData<String?> = _message

    // =======================
    // LOAD ACTIVE TASK
    // =======================

    /**
     * Mengambil task yang belum selesai dari repository
     * berdasarkan user yang sedang login.
     *
     * @param userId ID user yang sedang login
     */
    fun loadActiveTasks(userId: String) {
        viewModelScope.launch {
            try {
                // Tampilkan pesan loading
                _message.value = "Memuat task..."

                // Ambil task aktif dari repository
                val result = repository.getTasks(
                    userId = userId,
                    isComplete = false
                )

                // Simpan hasil ke LiveData
                _tasks.value = result

                // Tampilkan pesan jika task kosong
                _message.value =
                    if (result.isEmpty())
                        "Belum ada task, ayo tambahkan!"
                    else
                        null

            } catch (e: Exception) {
                // Tampilkan pesan error jika gagal memuat data
                _message.value = "Gagal memuat task: ${e.message}"
            }
        }
    }
}
