package com.example.projectmapgroup7.data.repository

import android.content.Context
import android.net.Uri
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.model.Task
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class TaskRepository {

    // Inisialisasi client Supabase dari singleton SupabaseClientInstance
    // Client ini digunakan untuk mengakses database dan storage Supabase
    private val client = SupabaseClientInstance.client

    /**
     * Mengambil daftar task berdasarkan user dan status penyelesaian
     *
     * @param userId ID user pemilik task
     * @param isComplete status task (true = selesai, false = belum selesai)
     * @return List<Task> sesuai filter
     */
    suspend fun getTasks(userId: String, isComplete: Boolean): List<Task> {
        return client.postgrest["tasks"]
            .select {
                filter {
                    // Filter berdasarkan ID user
                    eq("id_user", userId)

                    // Filter berdasarkan status penyelesaian task
                    eq("is_complete", isComplete)
                }
            }
            // Decode hasil query menjadi List<Task>
            .decodeList()
    }

    /**
     * Menghapus task berdasarkan user dan judul task
     *
     * @param userId ID user pemilik task
     * @param title judul task yang akan dihapus
     */
    suspend fun deleteTask(userId: String, title: String) {
        client.postgrest["tasks"].delete {
            filter {
                // Memastikan task milik user yang benar
                eq("id_user", userId)

                // Menghapus task berdasarkan judul
                eq("title", title)
            }
        }
    }

    /**
     * Mengunggah gambar task ke Supabase Storage
     *
     * @param context context Android untuk mengakses ContentResolver
     * @param uri URI gambar yang dipilih user
     * @param title judul task (digunakan sebagai bagian nama file)
     * @return URL publik gambar atau null jika gagal
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        title: String
    ): String? {

        // Mengakses bucket storage "task_images"
        val storage = client.storage.from("task_images")

        // Membuat nama file unik menggunakan title dan timestamp
        val fileName = "task_${title}_${System.currentTimeMillis()}.jpg"

        // Membaca data gambar dari URI menjadi byte array
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return null

        // Upload file ke Supabase Storage (upsert = true untuk overwrite jika ada)
        storage.upload(fileName, bytes, upsert = true)

        // Mengembalikan URL publik dari gambar yang diupload
        return storage.publicUrl(fileName)
    }

    /**
     * Menyimpan task baru ke database Supabase
     *
     * @param task objek Task yang akan disimpan
     */
    suspend fun insertTask(task: Task) {
        client.postgrest["tasks"].insert(task)
    }

    /**
     * Menandai task sebagai selesai
     *
     * @param userId ID user pemilik task
     * @param title judul task
     * @param completedAt waktu penyelesaian task
     */
    suspend fun markTaskAsDone(
        userId: String,
        title: String,
        completedAt: String
    ) {
        client.postgrest["tasks"].update({
            // Mengubah status task menjadi selesai
            set("is_complete", true)

            // Menyimpan waktu task diselesaikan
            set("completed_at", completedAt)
        }) {
            filter {
                // Memastikan task yang diupdate sesuai user
                eq("title", title)
                eq("id_user", userId)
            }
        }
    }

    /**
     * Mengupdate data task yang sudah ada
     *
     * @param userId ID user pemilik task
     * @param originalTitle judul lama task (sebagai identifier)
     * @param task objek Task berisi data terbaru
     */
    suspend fun updateTask(
        userId: String,
        originalTitle: String,
        task: Task
    ) {
        client.postgrest["tasks"].update({
            // Update data task dengan nilai terbaru
            set("title", task.title)
            set("description", task.description)
            set("image_url", task.image_url)
            set("prioritization", task.prioritization)
            set("deadline", task.deadline)
        }) {
            filter {
                // Memastikan task yang diupdate adalah milik user
                eq("title", originalTitle)
                eq("id_user", userId)
            }
        }
    }
}
