package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.*
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.repository.UserRepository
import com.example.projectmapgroup7.util.HashUtils
import kotlinx.coroutines.launch

/**
 * RegisterViewModel
 *
 * ViewModel yang bertanggung jawab untuk proses registrasi user baru.
 * Berfungsi sebagai penghubung antara UI (RegisterFragment)
 * dan UserRepository (akses database / Supabase).
 *
 * Menggunakan coroutine agar proses registrasi berjalan asynchronous
 * dan tidak mengganggu UI thread.
 */
class RegisterViewModel : ViewModel() {

    // Repository untuk operasi data user (cek username & insert user)
    private val repository = UserRepository()

    // =======================
    // STATE REGISTRASI
    // =======================

    // MutableLiveData untuk menyimpan hasil registrasi
    // success  -> data User yang berhasil dibuat
    // failure  -> pesan error
    private val _registerState = MutableLiveData<Result<User>>()

    // LiveData yang diamati oleh Fragment
    val registerState: LiveData<Result<User>> = _registerState

    // =======================
    // PROSES REGISTRASI
    // =======================

    /**
     * Melakukan registrasi user baru.
     *
     * Alur proses:
     * 1. Cek apakah username sudah digunakan
     * 2. Jika sudah → kirim error
     * 3. Jika belum → hash password
     * 4. Buat objek User
     * 5. Simpan data user ke database
     * 6. Kirim hasil registrasi ke UI
     *
     * @param username  username user
     * @param email     email user
     * @param password  password mentah (akan di-hash)
     * @param phone     nomor telepon (opsional)
     * @param nimNik    NIM/NIK (opsional)
     */
    fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        nimNik: String
    ) {
        viewModelScope.launch {
            try {
                // Cek apakah username sudah terdaftar
                if (repository.isUsernameExists(username)) {
                    _registerState.value =
                        Result.failure(Exception("Username sudah digunakan"))
                    return@launch
                }

                // Hash password sebelum disimpan
                val hashedPassword = HashUtils.sha256(password)

                // Membuat objek User
                val user = User(
                    username = username,
                    email = email,
                    password = hashedPassword,
                    phone = phone.ifEmpty { null },
                    nim_nik = nimNik.ifEmpty { null }
                )

                // Simpan user ke database
                val createdUser = repository.registerUser(user)

                // Registrasi berhasil
                _registerState.value = Result.success(createdUser)

            } catch (e: Exception) {
                // Tangani error (network, database, dll)
                _registerState.value =
                    Result.failure(Exception("Gagal registrasi: ${e.message}"))
            }
        }
    }
}
