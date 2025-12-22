package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.launch

/**
 * TaskViewModel
 *
 * ViewModel ini digunakan untuk mengelola data daftar tugas (Task),
 * baik yang sudah selesai maupun yang masih aktif.
 *
 * Bertugas sebagai penghubung antara:
 * - UI (Fragment daftar tugas)
 * - Repository (TaskRepository)
 *
 * ViewModel memastikan:
 * - Data tetap bertahan saat rotasi layar
 * - Operasi database dijalankan secara asynchronous (Coroutine)
 */
class TaskViewModel(
    // Repository sebagai sumber data (Supabase / database)
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    // =======================
    // STATE DATA TASK
    // =======================

    // LiveData untuk menyimpan daftar task
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    // LiveData untuk menandai status loading (progress)
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // LiveData untuk menyimpan pesan error (jika terjadi kesalahan)
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // =======================
    // LOAD TASK
    // =======================

    /**
     * Mengambil daftar task berdasarkan status selesai / belum selesai.
     *
     * @param userId     ID user yang sedang login
     * @param isComplete true  -> task selesai
     *                   false -> task aktif
     */
    fun loadTasks(userId: String, isComplete: Boolean) {
        viewModelScope.launch {
            try {
                // Tampilkan indikator loading
                _loading.value = true

                // Ambil data task dari repository
                _tasks.value = repository.getTasks(userId, isComplete)

            } catch (e: Exception) {
                // Simpan pesan error jika gagal
                _error.value = e.message
            } finally {
                // Matikan indikator loading
                _loading.value = false
            }
        }
    }

    // =======================
    // DELETE TASK
    // =======================

    /**
     * Menghapus beberapa task sekaligus berdasarkan judul.
     *
     * @param userId    ID user pemilik task
     * @param titles   List judul task yang akan dihapus
     * @param onSuccess Callback jika penghapusan berhasil
     */
    fun deleteTasks(userId: String, titles: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // Hapus task satu per satu berdasarkan judul
                titles.forEach {
                    repository.deleteTask(userId, it)
                }

                // Callback ke UI jika sukses
                onSuccess()

            } catch (e: Exception) {
                // Kirim pesan error ke UI
                _error.value = e.message
            }
        }
    }
}
