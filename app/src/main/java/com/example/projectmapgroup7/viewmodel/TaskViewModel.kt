package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.TaskRepository
import com.example.projectmapgroup7.model.Task
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadTasks(userId: String, isComplete: Boolean) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _tasks.value = repository.getTasks(userId, isComplete)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTasks(userId: String, titles: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                titles.forEach {
                    repository.deleteTask(userId, it)
                }
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
