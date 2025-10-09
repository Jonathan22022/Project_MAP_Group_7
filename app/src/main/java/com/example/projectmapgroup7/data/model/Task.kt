package com.example.projectmapgroup7.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val title : String,
    val description : String,
    val image_url : String? = "https://tgbjsowzhpoogknygtly.supabase.co/",
    val prioritization : String,
    val 
)
