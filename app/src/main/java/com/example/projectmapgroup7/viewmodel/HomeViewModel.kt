package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = TaskRepository()

    // 🔹 State data
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    // 🔹 State message (loading / empty / error)
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadActiveTasks(userId: String) {
        viewModelScope.launch {
            try {
                _message.value = "Memuat task..."

                val result = repository.getTasks(
                    userId = userId,
                    isComplete = false
                )

                _tasks.value = result

                _message.value =
                    if (result.isEmpty()) "Belum ada task, ayo tambahkan!"
                    else null

            } catch (e: Exception) {
                _message.value = "Gagal memuat task: ${e.message}"
            }
        }
    }
}
