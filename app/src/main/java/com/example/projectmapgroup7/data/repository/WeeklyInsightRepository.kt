package com.example.projectmapgroup7.data.repository

import android.util.Log
import com.example.projectmapgroup7.data.model.WeeklyInsight
import com.example.projectmapgroup7.data.model.WeeklyInsightDetail
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.from

/**
 * Repository ini berfungsi sebagai penghubung antara aplikasi dan database Supabase
 * untuk mengelola data Weekly Insight (ringkasan produktivitas mingguan) dan detail hariannya.
 */
class WeeklyInsightRepository {

    // Inisialisasi client Supabase untuk mengakses database
    private val client = SupabaseClientInstance.client

    /**
     * 🔹 Mengambil semua data insight mingguan milik user tertentu,
     * beserta daftar detail harian yang terkait dengan masing-masing insight.
     */
    suspend fun getWeeklyInsightsWithDetails(userId: String): List<Pair<WeeklyInsight, List<WeeklyInsightDetail>>> {
        // Ambil semua data dari tabel "weekly_insights" dan filter berdasarkan id_user
        val insights = client.from("weekly_insights")
            .select()
            .decodeList<WeeklyInsight>()
            .filter { it.id_user == userId }

        // Ambil semua detail dari tabel "weekly_insight_details"
        val details = client.from("weekly_insight_details")
            .select()
            .decodeList<WeeklyInsightDetail>()

        // Gabungkan data insight dengan detail harian yang cocok berdasarkan id
        return insights.map { insight ->
            val detailList = details.filter { it.weekly_insight_id == insight.id }
            insight to detailList
        }
    }

    /**
     * 🔹 Mengambil 1 data insight mingguan terbaru milik user berdasarkan created_at
     * beserta daftar detail harian untuk insight tersebut.
     */
    suspend fun getLatestWeeklyInsight(userId: String): Pair<WeeklyInsight?, List<WeeklyInsightDetail>> {
        // Ambil semua data dari tabel weekly_insights
        val rawResponse = client.from("weekly_insights")
            .select ()

        // Log untuk debugging: menampilkan data mentah hasil query
        Log.d("WeeklyInsightRepo", "Raw JSON (weekly_insights): ${rawResponse.data}")

        // Decode hasil query menjadi list model WeeklyInsight dan filter sesuai userId
        val insights = rawResponse.decodeList<WeeklyInsight>()
            .filter { it.id_user == userId }                   // Hanya ambil milik user terkait
            .sortedByDescending { it.created_at }              // Urutkan dari yang paling baru
        val latestInsight = insights.firstOrNull()             // Ambil insight pertama (terbaru)

        // Jika tidak ada data, kembalikan pasangan null dan list kosong
        if (latestInsight == null) return null to emptyList()

        // Ambil semua data detail dari tabel "weekly_insight_details"
        val rawDetails = client.from("weekly_insight_details")
            .select()
        // Filter detail berdasarkan id insight yang cocok
        val details = rawDetails.decodeList<WeeklyInsightDetail>()
            .filter { it.weekly_insight_id == latestInsight?.id }

        // Kembalikan pasangan insight dan daftar detail-nya
        return latestInsight to details
    }

    /**
     * 🔹 Menyimpan data Weekly Insight baru ke tabel "weekly_insights"
     */
    suspend fun insertWeeklyInsight(insight: WeeklyInsight): Boolean {
        return try {
            // Lakukan operasi insert dengan memetakan setiap atribut ke kolom tabel
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
            true // Jika berhasil insert
        } catch (e: Exception) {
            e.printStackTrace()
            false // Jika gagal
        }
    }

    /**
     * 🔹 Menyimpan data detail insight harian ke tabel "weekly_insight_details"
     */
    suspend fun insertInsightDetails(details: List<WeeklyInsightDetail>): Boolean {
        return try {
            // Mapping setiap objek WeeklyInsightDetail menjadi map kolom Supabase
            client.from("weekly_insight_details").insert(
                details.map {
                    mapOf(
                        "weekly_insight_id" to it.weekly_insight_id, // Hubungkan ke insight induk
                        "id_user" to it.id_user,
                        "day_of_week" to it.day_of_week,
                        "completed_count" to it.completed_count,
                        "pending_count" to it.pending_count
                    )
                }
            )
            true // Insert berhasil
        } catch (e: Exception) {
            e.printStackTrace()
            false // Insert gagal
        }
    }

    /**
     * 🔹 Menghapus semua data insight milik user tertentu (opsional, misalnya saat reset akun)
     */
    suspend fun deleteUserInsights(userId: String): Boolean {
        return try {
            // Hapus semua baris di tabel "weekly_insights" yang memiliki id_user tertentu
            client.from("weekly_insights")
                .delete {
                    filter {
                        eq("id_user", userId)
                    }
                }
            true // Jika berhasil
        } catch (e: Exception) {
            e.printStackTrace()
            false // Jika gagal
        }
    }
}
