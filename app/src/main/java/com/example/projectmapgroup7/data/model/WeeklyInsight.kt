package com.example.projectmapgroup7.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyInsight(
    val id: String? = null,
    val id_user: String? = null,
    val week_start: String? = null,
    val week_end: String? = null,
    val total_completed: Int = 0,
    val total_pending: Int = 0,
    val most_productive_day: String? = null,
    val suggestion: String? = null,
    val cluster_label: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val details: List<WeeklyInsightDetail>? = null
)