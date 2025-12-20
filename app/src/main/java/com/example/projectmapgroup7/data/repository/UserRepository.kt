package com.example.projectmapgroup7.data.repository

import android.content.Context
import android.net.Uri
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class UserRepository {

    private val client = SupabaseClientInstance.client

    suspend fun isUsernameExists(username: String): Boolean {
        val result = client.postgrest["users"]
            .select {
                filter { eq("username", username) }
            }
            .decodeList<User>()

        return result.isNotEmpty()
    }

    suspend fun registerUser(user: User): User {
        client.postgrest["users"].insert(user)

        return client.postgrest["users"]
            .select {
                filter { eq("username", user.username) }
            }
            .decodeSingle()
    }

    suspend fun getUserByUsername(username: String): Map<String, Any> {
        return client.postgrest["users"]
            .select { filter { eq("username", username) } }
            .decodeSingle()
    }

    suspend fun getTotalTasks(userId: String): Int {
        return client.postgrest["tasks"]
            .select { filter { eq("id_user", userId) } }
            .decodeList<Map<String, Any>>()
            .size
    }

    suspend fun getCompletedTasks(userId: String): Int {
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

    suspend fun uploadProfilePicture(
        context: Context,
        uri: Uri,
        username: String
    ): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream!!.readBytes()

        val fileName = "profile_${username}_${System.currentTimeMillis()}.jpg"
        val storage = client.storage.from("profile_pictures")

        storage.upload(fileName, bytes, upsert = true)
        val publicUrl = storage.publicUrl(fileName)

        client.postgrest["users"].update(
            { set("profile_picture", publicUrl) }
        ) {
            filter { eq("username", username) }
        }

        return publicUrl
    }
}
