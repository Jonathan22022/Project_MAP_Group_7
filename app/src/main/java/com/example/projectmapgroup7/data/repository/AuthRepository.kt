package com.example.projectmapgroup7.data.repository

import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    private val client = SupabaseClientInstance.client

    suspend fun login(username: String, hashedPassword: String): User? {
        val users = client.postgrest["users"]
            .select {
                filter {
                    eq("username", username)
                    eq("password", hashedPassword)
                }
            }
            .decodeList<User>()

        return users.firstOrNull()
    }
}
