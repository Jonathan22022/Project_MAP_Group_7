package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.repository.UserRepository
import com.example.projectmapgroup7.util.HashUtils
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _registerState = MutableLiveData<Result<User>>()
    val registerState: LiveData<Result<User>> = _registerState

    fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        nimNik: String
    ) {
        viewModelScope.launch {
            try {
                if (repository.isUsernameExists(username)) {
                    _registerState.value =
                        Result.failure(Exception("Username sudah digunakan"))
                    return@launch
                }

                val hashedPassword = HashUtils.sha256(password)

                val user = User(
                    username = username,
                    email = email,
                    password = hashedPassword,
                    phone = phone.ifEmpty { null },
                    nim_nik = nimNik.ifEmpty { null }
                )

                val createdUser = repository.registerUser(user)
                _registerState.value = Result.success(createdUser)

            } catch (e: Exception) {
                _registerState.value =
                    Result.failure(Exception("Gagal registrasi: ${e.message}"))
            }
        }
    }
}
