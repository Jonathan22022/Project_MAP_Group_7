package com.example.projectmapgroup7.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.ml.PriorityPredictor
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository()
    private val priorityPredictor = PriorityPredictor(application)

    private val _editTaskState = MutableLiveData<Result<Unit>>()
    val editTaskState: LiveData<Result<Unit>> = _editTaskState

    fun updateTask(
        userId: String,
        originalTitle: String,
        title: String,
        description: String,
        deadline: String,
        imageUri: Uri?,
        oldImageUrl: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val priority = predictPriority(description)

                val imageUrl = imageUri?.let {
                    repository.uploadImage(getApplication(), it, title)
                } ?: oldImageUrl

                val updatedTask = Task(
                    title = title,
                    description = description,
                    image_url = imageUrl,
                    prioritization = priority,
                    deadline = deadline,
                    is_complete = false,
                    id_user = userId
                )

                repository.updateTask(
                    userId = userId,
                    originalTitle = originalTitle,
                    task = updatedTask
                )

                _editTaskState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _editTaskState.postValue(
                    Result.failure(Exception("Gagal update tugas: ${e.message}"))
                )
            }
        }
    }

    // ===== PRIORITY ML =====
    private fun predictPriority(text: String): String {
        val dummyVector = FloatArray(5000) { 0f }
        val resultIndex = priorityPredictor.predictPriority(dummyVector)

        val lower = text.lowercase()
        var score = 0
        if ("penting" in lower || "urgent" in lower || "segera" in lower) score += 2
        if ("hari ini" in lower || "deadline" in lower) score += 1
        if ("nanti" in lower || "santai" in lower) score -= 1

        val adjusted = when {
            score >= 2 -> 2
            score == 1 -> 1
            else -> resultIndex
        }

        return when (adjusted) {
            0 -> "rendah"
            1 -> "sedang"
            2 -> "tinggi"
            else -> "sedang"
        }
    }
}
