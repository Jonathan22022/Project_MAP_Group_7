package com.example.projectmapgroup7.data.repository

import android.content.Context
import android.net.Uri
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class UserRepository {

    // Inisialisasi client Supabase dari singleton SupabaseClientInstance
    // Digunakan untuk mengakses tabel users, tasks, dan Supabase Storage
    private val client = SupabaseClientInstance.client

    /**
     * Mengecek apakah username sudah terdaftar di database
     *
     * @param username username yang ingin dicek
     * @return true jika username sudah ada, false jika belum
     */
    suspend fun isUsernameExists(username: String): Boolean {

        // Query ke tabel "users" dengan filter username
        val result = client.postgrest["users"]
            .select {
                filter {
                    // Filter berdasarkan username
                    eq("username", username)
                }
            }
            // Decode hasil query menjadi List<User>
            .decodeList<User>()

        // Jika hasil tidak kosong, berarti username sudah digunakan
        return result.isNotEmpty()
    }

    /**
     * Mendaftarkan user baru ke database
     *
     * @param user objek User yang akan disimpan
     * @return User data user yang berhasil disimpan
     */
    suspend fun registerUser(user: User): User {

        // Insert data user baru ke tabel "users"
        client.postgrest["users"].insert(user)

        // Mengambil kembali data user yang baru disimpan berdasarkan username
        return client.postgrest["users"]
            .select {
                filter {
                    eq("username", user.username)
                }
            }
            .decodeSingle()
    }

    /**
     * Mengambil data user berdasarkan username
     *
     * @param username username user
     * @return Map berisi data user (key-value)
     */
    suspend fun getUserByUsername(username: String): Map<String, Any> {

        // Query satu data user berdasarkan username
        return client.postgrest["users"]
            .select {
                filter {
                    eq("username", username)
                }
            }
            .decodeSingle()
    }

    /**
     * Menghitung total jumlah task milik user
     *
     * @param userId ID user
     * @return total task yang dimiliki user
     */
    suspend fun getTotalTasks(userId: String): Int {

        // Mengambil seluruh task berdasarkan ID user lalu menghitung jumlahnya
        return client.postgrest["tasks"]
            .select {
                filter {
                    eq("id_user", userId)
                }
            }
            .decodeList<Map<String, Any>>()
            .size
    }

    /**
     * Menghitung jumlah task yang sudah diselesaikan oleh user
     *
     * @param userId ID user
     * @return jumlah task yang sudah selesai
     */
    suspend fun getCompletedTasks(userId: String): Int {

        // Query task yang sudah selesai (is_complete = true)
        return client.postgrest["tasks"]
            .select {
                filter {
                    eq("id_user", userId)
                    eq("is_complete", true)
                }
            }
            .decodeList<Map<String, Any>>()
            .size
    }

    /**
     * Mengupload foto profil user ke Supabase Storage
     * dan menyimpan URL gambar ke database
     *
     * @param context context Android
     * @param uri URI gambar yang dipilih user
     * @param username username pemilik akun
     * @return URL publik foto profil
     */
    suspend fun uploadProfilePicture(
        context: Context,
        uri: Uri,
        username: String
    ): String {

        // Membaca gambar dari URI menjadi byte array
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream!!.readBytes()

        // Membuat nama file unik menggunakan username dan timestamp
        val fileName = "profile_${username}_${System.currentTimeMillis()}.jpg"

        // Mengakses bucket "profile_pictures" di Supabase Storage
        val storage = client.storage.from("profile_pictures")

        // Upload file ke Supabase Storage
        storage.upload(fileName, bytes, upsert = true)

        // Mengambil URL publik dari gambar yang diupload
        val publicUrl = storage.publicUrl(fileName)

        // Update kolom profile_picture pada tabel users
        client.postgrest["users"].update(
            { set("profile_picture", publicUrl) }
        ) {
            filter {
                eq("username", username)
            }
        }

        // Mengembalikan URL foto profil
        return publicUrl
    }
}
