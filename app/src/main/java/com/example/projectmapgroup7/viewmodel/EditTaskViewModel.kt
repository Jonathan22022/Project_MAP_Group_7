package com.example.projectmapgroup7.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.ml.PriorityPredictor
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * EditTaskViewModel
 *
 * ViewModel yang bertanggung jawab untuk:
 * - Mengelola proses edit / update tugas
 * - Mengunggah ulang gambar jika diganti
 * - Menentukan ulang prioritas tugas menggunakan ML + rule sederhana
 *
 * Menggunakan AndroidViewModel karena membutuhkan Application context
 * (digunakan saat upload gambar).
 */
class EditTaskViewModel(application: Application) : AndroidViewModel(application) {

    // Repository untuk akses database & storage
    private val repository = TaskRepository()

    // Model Machine Learning untuk prediksi prioritas
    private val priorityPredictor = PriorityPredictor(application)

    // =======================
    // EDIT TASK STATE
    // =======================

    // LiveData untuk memberi tahu UI apakah proses edit berhasil atau gagal
    private val _editTaskState = MutableLiveData<Result<Unit>>()
    val editTaskState: LiveData<Result<Unit>> = _editTaskState

    // =======================
    // UPDATE TASK
    // =======================

    /**
     * Memperbarui data tugas
     *
     * @param userId         ID user pemilik tugas
     * @param originalTitle Judul lama (digunakan sebagai identifier task)
     * @param title          Judul baru tugas
     * @param description    Deskripsi tugas
     * @param deadline       Deadline tugas
     * @param imageUri       URI gambar baru (jika diganti)
     * @param oldImageUrl    URL gambar lama (jika tidak diganti)
     */
    fun updateTask(
        userId: String,
        originalTitle: String,
        title: String,
        description: String,
        deadline: String,
        imageUri: Uri?,
        oldImageUrl: String?
    ) {
        // Jalankan proses di background thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tentukan prioritas berdasarkan deskripsi
                val priority = predictPriority(description)

                // Upload gambar baru jika ada, jika tidak gunakan gambar lama
                val imageUrl = imageUri?.let {
                    repository.uploadImage(getApplication(), it, title)
                } ?: oldImageUrl

                // Buat objek Task baru dengan data terbaru
                val updatedTask = Task(
                    title = title,
                    description = description,
                    image_url = imageUrl,
                    prioritization = priority,
                    deadline = deadline,
                    is_complete = false,
                    id_user = userId
                )

                // Update task di database berdasarkan judul lama
                repository.updateTask(
                    userId = userId,
                    originalTitle = originalTitle,
                    task = updatedTask
                )

                // Kirim status sukses ke UI
                _editTaskState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                // Kirim status gagal ke UI
                _editTaskState.postValue(
                    Result.failure(
                        Exception("Gagal update tugas: ${e.message}")
                    )
                )
            }
        }
    }

    // =======================
    // PRIORITY ML
    // =======================

    /**
     * Menentukan prioritas tugas berdasarkan:
     * 1. Prediksi Machine Learning (PriorityPredictor)
     * 2. Rule-based scoring dari kata kunci dalam deskripsi
     *
     * @param text Deskripsi tugas
     * @return String prioritas ("rendah", "sedang", "tinggi")
     */
    private fun predictPriority(text: String): String {

        // Dummy vector sebagai input model ML
        val dummyVector = FloatArray(5000) { 0f }

        // Hasil prediksi awal dari model ML
        val resultIndex = priorityPredictor.predictPriority(dummyVector)

        // Rule-based adjustment berdasarkan kata kunci
        val lower = text.lowercase()
        var score = 0

        if ("penting" in lower || "urgent" in lower || "segera" in lower) score += 2
        if ("hari ini" in lower || "deadline" in lower) score += 1
        if ("nanti" in lower || "santai" in lower) score -= 1

        // Kombinasi rule-based + ML
        val adjusted = when {
            score >= 2 -> 2
            score == 1 -> 1
            else -> resultIndex
        }

        // Mapping index ke label prioritas
        return when (adjusted) {
            0 -> "rendah"
            1 -> "sedang"
            2 -> "tinggi"
            else -> "sedang"
        }
    }
}
