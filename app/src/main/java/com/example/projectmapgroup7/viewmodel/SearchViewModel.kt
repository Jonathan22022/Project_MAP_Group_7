package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.model.Task
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val client = SupabaseClientInstance.client

    // LiveData untuk hasil pencarian
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    // LiveData untuk pesan error / empty
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun searchTasks(userId: String, keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", userId)
                            ilike("title", "%$keyword%")
                        }
                    }
                    .decodeList<Task>()

                _tasks.postValue(results)
                _message.postValue(
                    if (results.isEmpty())
                        "Tidak ditemukan tugas dengan judul \"$keyword\""
                    else null
                )

            } catch (e: Exception) {
                _message.postValue("Gagal mencari: ${e.message}")
            }
        }
    }

    fun clearResults() {
        _tasks.value = emptyList()
        _message.value = null
    }
}
