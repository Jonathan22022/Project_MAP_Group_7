package com.example.projectmapgroup7.data.repository

import android.content.Context
import android.net.Uri
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.model.Task
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class TaskRepository {

    private val client = SupabaseClientInstance.client

    suspend fun getTasks(userId: String, isComplete: Boolean): List<Task> {
        return client.postgrest["tasks"]
            .select {
                filter {
                    eq("id_user", userId)
                    eq("is_complete", isComplete)
                }
            }
            .decodeList()
    }

    suspend fun deleteTask(userId: String, title: String) {
        client.postgrest["tasks"].delete {
            filter {
                eq("id_user", userId)
                eq("title", title)
            }
        }
    }

    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        title: String
    ): String? {
        val storage = client.storage.from("task_images")
        val fileName = "task_${title}_${System.currentTimeMillis()}.jpg"

        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return null

        storage.upload(fileName, bytes, upsert = true)
        return storage.publicUrl(fileName)
    }

    suspend fun insertTask(task: Task) {
        client.postgrest["tasks"].insert(task)
    }

    suspend fun markTaskAsDone(
        userId: String,
        title: String,
        completedAt: String
    ) {
        client.postgrest["tasks"].update({
            set("is_complete", true)
            set("completed_at", completedAt)
        }) {
            filter {
                eq("title", title)
                eq("id_user", userId)
            }
        }
    }

    suspend fun updateTask(
        userId: String,
        originalTitle: String,
        task: Task
    ) {
        client.postgrest["tasks"].update({
            set("title", task.title)
            set("description", task.description)
            set("image_url", task.image_url)
            set("prioritization", task.prioritization)
            set("deadline", task.deadline)
        }) {
            filter {
                eq("title", originalTitle)
                eq("id_user", userId)
            }
        }
    }
}