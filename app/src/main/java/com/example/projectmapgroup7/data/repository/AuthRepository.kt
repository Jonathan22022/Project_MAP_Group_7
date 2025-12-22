package com.example.projectmapgroup7.data.repository

import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    // Inisialisasi client Supabase yang diambil dari singleton SupabaseClientInstance
    // Client ini digunakan untuk berkomunikasi dengan database Supabase
    private val client = SupabaseClientInstance.client

    /**
     * Fungsi login untuk melakukan autentikasi user
     *
     * @param username username yang dimasukkan oleh user
     * @param hashedPassword password yang sudah di-hash (keamanan)
     * @return objek User jika data cocok, atau null jika tidak ditemukan
     */
    suspend fun login(username: String, hashedPassword: String): User? {

        // Mengakses tabel "users" pada Supabase menggunakan PostgREST
        // Kemudian melakukan query SELECT dengan filter username dan password
        val users = client.postgrest["users"]
            .select {
                filter {
                    // Filter berdasarkan username
                    eq("username", username)

                    // Filter berdasarkan password yang sudah di-hash
                    eq("password", hashedPassword)
                }
            }
            // Mengubah hasil query menjadi List<User>
            .decodeList<User>()

        // Mengembalikan user pertama jika ada,
        // atau null jika tidak ada data yang cocok
        return users.firstOrNull()
    }
}
