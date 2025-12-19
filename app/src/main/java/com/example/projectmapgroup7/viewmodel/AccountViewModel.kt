package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AccountViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _accountAge = MutableLiveData<String>()
    val accountAge: LiveData<String> = _accountAge

    private val _totalTasks = MutableLiveData<Int>()
    val totalTasks: LiveData<Int> = _totalTasks

    private val _completedTasks = MutableLiveData<Int>()
    val completedTasks: LiveData<Int> = _completedTasks

    fun loadAccountData(username: String, userId: String) {
        viewModelScope.launch {
            try {
                val userData = repository.getUserByUsername(username)
                val createdAt = userData["created_at"]?.toString()
                _accountAge.value = calculateAccountAge(createdAt)

                _totalTasks.value = repository.getTotalTasks(userId)
                _completedTasks.value = repository.getCompletedTasks(userId)

            } catch (e: Exception) {
                _accountAge.value = "- hari"
                _totalTasks.value = 0
                _completedTasks.value = 0
            }
        }
    }

    private fun calculateAccountAge(createdAt: String?): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val createdDate = formatter.parse(createdAt ?: return "- hari")
            val diff = Date().time - (createdDate?.time ?: 0)
            "${diff / (1000 * 60 * 60 * 24)} hari"
        } catch (e: Exception) {
            "- hari"
        }
    }
}
