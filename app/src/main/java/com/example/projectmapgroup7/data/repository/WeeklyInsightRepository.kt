package com.example.projectmapgroup7.data.repository

import android.util.Log
import com.example.projectmapgroup7.data.model.WeeklyInsight
import com.example.projectmapgroup7.data.model.WeeklyInsightDetail
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.from

class WeeklyInsightRepository {

    private val client = SupabaseClientInstance.client

    /**
     * 🔹 Ambil semua insight mingguan milik user tertentu (beserta detail per hari)
     */
    suspend fun getWeeklyInsightsWithDetails(userId: String): List<Pair<WeeklyInsight, List<WeeklyInsightDetail>>> {
        val insights = client.from("weekly_insights")
            .select()
            .decodeList<WeeklyInsight>()
            .filter { it.id_user == userId }

        val details = client.from("weekly_insight_details")
            .select()
            .decodeList<WeeklyInsightDetail>()

        return insights.map { insight ->
            val detailList = details.filter { it.weekly_insight_id == insight.id }
            insight to detailList
        }
    }

    /**
     * 🔹 Ambil insight mingguan terbaru milik user (berdasarkan created_at)
     */
    suspend fun getLatestWeeklyInsight(userId: String): Pair<WeeklyInsight?, List<WeeklyInsightDetail>> {
        // Ambil insight terbaru berdasarkan created_at
        val rawResponse = client.from("weekly_insights")
            .select ()

        Log.d("WeeklyInsightRepo", "Raw JSON (weekly_insights): ${rawResponse.data}")

        val insights = rawResponse.decodeList<WeeklyInsight>()
            .filter { it.id_user == userId }                   // filter user
            .sortedByDescending { it.created_at }             // urutkan terbaru dulu
        val latestInsight = insights.firstOrNull()           // ambil yang paling baru

        // Jika tidak ada data, kembalikan null
        if (latestInsight == null) return null to emptyList()

        // Ambil detail harian untuk insight tersebut
        val rawDetails = client.from("weekly_insight_details")
            .select()
        val details = rawDetails.decodeList<WeeklyInsightDetail>()
            .filter { it.weekly_insight_id == latestInsight?.id }  // filter sesuai insight terbaru

        return latestInsight to details
    }

    /**
     * 🔹 Simpan insight mingguan baru
     */
    suspend fun insertWeeklyInsight(insight: WeeklyInsight): Boolean {
        return try {
            client.from("weekly_insights").insert(
                mapOf(
                    "id_user" to insight.id_user,
                    "week_start" to insight.week_start,
                    "week_end" to insight.week_end,
                    "total_completed" to insight.total_completed,
                    "total_pending" to insight.total_pending,
                    "most_productive_day" to insight.most_productive_day,
                    "suggestion" to insight.suggestion,
                    "cluster_label" to insight.cluster_label
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 🔹 Simpan detail insight per hari
     */
    suspend fun insertInsightDetails(details: List<WeeklyInsightDetail>): Boolean {
        return try {
            client.from("weekly_insight_details").insert(
                details.map {
                    mapOf(
                        "weekly_insight_id" to it.weekly_insight_id,
                        "id_user" to it.id_user,
                        "day_of_week" to it.day_of_week,
                        "completed_count" to it.completed_count,
                        "pending_count" to it.pending_count
                    )
                }
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 🔹 Hapus semua insight milik user (opsional)
     */
    suspend fun deleteUserInsights(userId: String): Boolean {
        return try {
            client.from("weekly_insights")
                .delete {
                    filter {
                        eq("id_user", userId)
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
