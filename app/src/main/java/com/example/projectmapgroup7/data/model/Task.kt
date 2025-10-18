package com.example.projectmapgroup7.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val title: String,
    val description: String,
    val image_url: String? = null,
    val prioritization: String,
    val deadline: String,
    val is_complete: Boolean = false,
    val id_user: String
)