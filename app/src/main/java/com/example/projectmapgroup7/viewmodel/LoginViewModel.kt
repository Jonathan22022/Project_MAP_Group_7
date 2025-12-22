package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.repository.AuthRepository
import com.example.projectmapgroup7.util.HashUtils
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 *
 * ViewModel yang menangani proses login user.
 * Bertugas sebagai penghubung antara UI (LoginFragment)
 * dan AuthRepository (Supabase / backend).
 *
 * Menggunakan coroutine agar proses login berjalan
 * secara asynchronous dan tidak memblok UI thread.
 */
class LoginViewModel : ViewModel() {

    // Repository untuk autentikasi user
    private val repository = AuthRepository()

    // =======================
    // STATE LOGIN
    // =======================

    // MutableLiveData untuk menyimpan hasil login
    // (success -> User, failure -> Exception)
    private val _loginState = MutableLiveData<Result<User>>()

    // LiveData yang diamati oleh Fragment
    val loginState: LiveData<Result<User>> = _loginState

    // =======================
    // PROSES LOGIN
    // =======================

    /**
     * Melakukan login menggunakan username dan password.
     *
     * Alur proses:
     * 1. Password di-hash menggunakan SHA-256
     * 2. Data dikirim ke repository untuk dicek ke database
     * 3. Jika user ditemukan → login berhasil
     * 4. Jika tidak → tampilkan error
     *
     * @param username username yang dimasukkan user
     * @param password password mentah (akan di-hash)
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                // Hash password sebelum dikirim ke database
                val hashedPassword = HashUtils.sha256(password)

                // Panggil repository untuk proses login
                val user = repository.login(username, hashedPassword)

                // Jika user ditemukan → login berhasil
                if (user != null) {
                    _loginState.value = Result.success(user)
                } else {
                    // Jika username atau password salah
                    _loginState.value =
                        Result.failure(Exception("Username atau password salah"))
                }

            } catch (e: Exception) {
                // Tangani error lain (network, server, dll)
                _loginState.value =
                    Result.failure(Exception("Login gagal: ${e.message}"))
            }
        }
    }
}
