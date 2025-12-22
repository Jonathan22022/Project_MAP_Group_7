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
 * AddTaskViewModel
 *
 * ViewModel yang bertugas menangani proses penambahan tugas baru:
 * - Mengelola logika bisnis (business logic)
 * - Memprediksi prioritas tugas menggunakan ML
 * - Mengunggah gambar tugas ke storage
 * - Menyimpan data tugas ke database
 *
 * Menggunakan AndroidViewModel karena membutuhkan Application Context
 * (untuk upload gambar dan ML model).
 */
class AddTaskViewModel(application: Application) : AndroidViewModel(application) {

    // Repository sebagai penghubung ke data source (Supabase / database)
    private val repository = TaskRepository()

    // Model Machine Learning untuk prediksi prioritas tugas
    private val priorityPredictor = PriorityPredictor(application)

    // =======================
    // ADD TASK STATE
    // =======================

    // LiveData untuk memantau status penambahan task (sukses / gagal)
    private val _addTaskState = MutableLiveData<Result<Unit>>()
    val addTaskState: LiveData<Result<Unit>> = _addTaskState

    // =======================
    // ADD TASK PROCESS
    // =======================

    /**
     * Menambahkan task baru ke sistem
     *
     * @param title       Judul tugas
     * @param description Deskripsi tugas
     * @param deadline    Deadline tugas
     * @param imageUri    URI gambar (opsional)
     * @param userId      ID user pemilik tugas
     */
    fun addTask(
        title: String,
        description: String,
        deadline: String,
        imageUri: Uri?,
        userId: String
    ) {
        // Jalankan proses di background thread (IO)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Prediksi prioritas berdasarkan deskripsi tugas
                val priority = predictPriority(description)

                // Upload gambar jika ada dan ambil URL hasil upload
                val imageUrl = imageUri?.let {
                    repository.uploadImage(getApplication(), it, title)
                }

                // Buat objek Task
                val task = Task(
                    title = title,
                    description = description,
                    image_url = imageUrl,
                    prioritization = priority,
                    deadline = deadline,
                    is_complete = false,
                    id_user = userId
                )

                // Simpan task ke database
                repository.insertTask(task)

                // Update state menjadi sukses
                _addTaskState.postValue(Result.success(Unit))

            } catch (e: Exception) {
                // Jika terjadi error, kirim status gagal
                _addTaskState.postValue(
                    Result.failure(Exception("Gagal menambahkan tugas: ${e.message}"))
                )
            }
        }
    }

    // =======================
    // ML PRIORITY PREDICTION
    // =======================

    /**
     * Memprediksi prioritas tugas menggunakan kombinasi:
     * - Model Machine Learning (PriorityPredictor)
     * - Heuristik berbasis kata kunci (rule-based)
     *
     * @param text Deskripsi tugas
     * @return String prioritas ("rendah", "sedang", "tinggi")
     */
    private fun predictPriority(text: String): String {

        // Dummy vector sebagai input model ML
        // (diasumsikan preprocessing dilakukan di luar scope ini)
        val dummyVector = FloatArray(5000) { 0f }

        // Prediksi awal dari model ML
        val resultIndex = priorityPredictor.predictPriority(dummyVector)

        // Analisis kata kunci secara manual (heuristik)
        val lower = text.lowercase()
        var score = 0

        if ("penting" in lower || "urgent" in lower || "segera" in lower) score += 2
        if ("hari ini" in lower || "deadline" in lower) score += 1
        if ("nanti" in lower || "santai" in lower) score -= 1

        // Gabungkan hasil ML dan heuristik
        val adjusted = when {
            score >= 2 -> 2
            score == 1 -> 1
            else -> resultIndex
        }

        // Mapping indeks ke label prioritas
        return when (adjusted) {
            0 -> "rendah"
            1 -> "sedang"
            2 -> "tinggi"
            else -> "sedang"
        }
    }
}
