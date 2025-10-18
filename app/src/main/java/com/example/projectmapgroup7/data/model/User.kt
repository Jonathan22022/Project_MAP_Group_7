package com.example.projectmapgroup7.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = null,
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val nim_nik: String? = null,
    val profile_picture: String? = "https://tgbjsowzhpoogknygtly.supabase.co/storage/v1/object/public/profile/ic_account_.xml"
)