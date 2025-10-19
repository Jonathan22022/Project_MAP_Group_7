package com.example.projectmapgroup7.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyInsightDetail(
    val id: String? = null,
    val weekly_insight_id: String? = null,
    val id_user: String? = null,
    val day_of_week: String,
    val completed_count: Int = 0,
    val pending_count: Int = 0,
    val created_at: String? = null
)