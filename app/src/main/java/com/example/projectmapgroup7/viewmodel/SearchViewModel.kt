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

/**
 * SearchViewModel
 *
 * ViewModel ini bertanggung jawab untuk menangani
 * fitur pencarian task berdasarkan judul.
 *
 * ViewModel akan:
 * - Mengambil data task dari Supabase
 * - Memfilter task berdasarkan userId dan keyword
 * - Mengirim hasil pencarian ke UI melalui LiveData
 */
class SearchViewModel : ViewModel() {

    // Client Supabase untuk mengakses database (PostgREST)
    private val client = SupabaseClientInstance.client

    // =======================
    // STATE HASIL PENCARIAN
    // =======================

    // Menyimpan daftar task hasil pencarian
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    // Menyimpan pesan untuk UI (kosong / error)
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // =======================
    // PROSES PENCARIAN TASK
    // =======================

    /**
     * Mencari task milik user berdasarkan judul (title).
     *
     * Proses:
     * 1. Query ke tabel "tasks" di Supabase
     * 2. Filter berdasarkan id_user
     * 3. Filter judul menggunakan ILIKE (case-insensitive)
     * 4. Kirim hasil ke UI melalui LiveData
     *
     * @param userId   ID user yang sedang login
     * @param keyword kata kunci pencarian
     */
    fun searchTasks(userId: String, keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Query ke database Supabase
                val results = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", userId)
                            ilike("title", "%$keyword%")
                        }
                    }
                    .decodeList<Task>()

                // Kirim hasil pencarian ke UI
                _tasks.postValue(results)

                // Set pesan jika hasil kosong
                _message.postValue(
                    if (results.isEmpty())
                        "Tidak ditemukan tugas dengan judul \"$keyword\""
                    else null
                )

            } catch (e: Exception) {
                // Tangani error (network, query, dll)
                _message.postValue("Gagal mencari: ${e.message}")
            }
        }
    }

    // =======================
    // RESET HASIL PENCARIAN
    // =======================

    /**
     * Menghapus hasil pencarian dan pesan.
     * Biasanya dipanggil saat input pencarian kosong.
     */
    fun clearResults() {
        _tasks.value = emptyList()
        _message.value = null
    }
}
