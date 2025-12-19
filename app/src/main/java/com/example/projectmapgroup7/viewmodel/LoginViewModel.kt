package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.repository.AuthRepository
import com.example.projectmapgroup7.util.HashUtils
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<Result<User>>()
    val loginState: LiveData<Result<User>> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val hashedPassword = HashUtils.sha256(password)
                val user = repository.login(username, hashedPassword)

                if (user != null) {
                    _loginState.value = Result.success(user)
                } else {
                    _loginState.value =
                        Result.failure(Exception("Username atau password salah"))
                }

            } catch (e: Exception) {
                _loginState.value =
                    Result.failure(Exception("Login gagal: ${e.message}"))
            }
        }
    }
}
