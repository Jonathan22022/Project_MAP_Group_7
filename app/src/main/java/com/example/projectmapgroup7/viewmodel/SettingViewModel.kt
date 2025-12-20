package com.example.projectmapgroup7.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.projectmapgroup7.data.repository.UserRepository
import kotlinx.coroutines.launch

class SettingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = UserRepository()

    private val _uploadResult = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = _uploadResult

    fun uploadProfilePicture(uri: Uri, username: String) {
        viewModelScope.launch {
            try {
                val url = repository.uploadProfilePicture(
                    getApplication(),
                    uri,
                    username
                )
                _uploadResult.value = Result.success(url)
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            }
        }
    }
}
