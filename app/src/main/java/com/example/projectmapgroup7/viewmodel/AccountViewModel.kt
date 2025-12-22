package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AccountViewModel
 *
 * ViewModel yang bertanggung jawab untuk:
 * - Mengambil dan mengelola data akun pengguna
 * - Menghitung usia akun (sejak tanggal pembuatan)
 * - Mengambil jumlah total tugas dan tugas yang telah diselesaikan
 *
 * ViewModel ini berperan sebagai penghubung antara UI (AccountFragment)
 * dan data source (UserRepository).
 */
class AccountViewModel(
    // Repository sebagai sumber data (Supabase / database)
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    // =======================
    // LIVE DATA
    // =======================

    // Menyimpan usia akun dalam bentuk teks (contoh: "10 hari")
    private val _accountAge = MutableLiveData<String>()
    val accountAge: LiveData<String> = _accountAge

    // Menyimpan jumlah total tugas milik user
    private val _totalTasks = MutableLiveData<Int>()
    val totalTasks: LiveData<Int> = _totalTasks

    // Menyimpan jumlah tugas yang sudah selesai
    private val _completedTasks = MutableLiveData<Int>()
    val completedTasks: LiveData<Int> = _completedTasks

    // =======================
    // LOAD ACCOUNT DATA
    // =======================

    /**
     * Mengambil seluruh data akun user:
     * - Usia akun
     * - Total tugas
     * - Tugas yang telah diselesaikan
     *
     * @param username Username user (untuk mengambil created_at)
     * @param userId   ID user (untuk menghitung tugas)
     */
    fun loadAccountData(username: String, userId: String) {

        // Coroutine berjalan sesuai lifecycle ViewModel
        viewModelScope.launch {
            try {
                // Ambil data user berdasarkan username
                val userData = repository.getUserByUsername(username)

                // Ambil tanggal pembuatan akun
                val createdAt = userData["created_at"]?.toString()

                // Hitung usia akun
                _accountAge.value = calculateAccountAge(createdAt)

                // Ambil jumlah total tugas
                _totalTasks.value = repository.getTotalTasks(userId)

                // Ambil jumlah tugas selesai
                _completedTasks.value = repository.getCompletedTasks(userId)

            } catch (e: Exception) {
                // Jika terjadi error, tampilkan nilai default
                _accountAge.value = "- hari"
                _totalTasks.value = 0
                _completedTasks.value = 0
            }
        }
    }

    // =======================
    // ACCOUNT AGE CALCULATION
    // =======================

    /**
     * Menghitung usia akun berdasarkan waktu pembuatan akun
     *
     * @param createdAt Timestamp pembuatan akun (format ISO)
     * @return String usia akun (contoh: "15 hari")
     */
    private fun calculateAccountAge(createdAt: String?): String {
        return try {
            // Format waktu dari Supabase (ISO format)
            val formatter =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            // Konversi string ke Date
            val createdDate = formatter.parse(createdAt ?: return "- hari")

            // Selisih waktu (millisecond)
            val diff = Date().time - (createdDate?.time ?: 0)

            // Konversi ke hari
            "${diff / (1000 * 60 * 60 * 24)} hari"
        } catch (e: Exception) {
            "- hari"
        }
    }
}
