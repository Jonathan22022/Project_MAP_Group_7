package com.example.projectmapgroup7.data.model

import kotlinx.serialization.Serializable

/**
 * 🧩 Data class ini merepresentasikan model "WeeklyInsight"
 * yang digunakan untuk menyimpan ringkasan produktivitas pengguna selama 1 minggu.
 *
 * Data ini diambil dari / disimpan ke tabel "weekly_insights" di Supabase.
 */
@Serializable // Annotation agar bisa diserialisasi saat dikirim/diterima lewat Supabase (JSON)
data class WeeklyInsight(
    val id: String? = null,                  // ID unik dari insight mingguan (biasanya di-generate otomatis oleh database)
    val id_user: String? = null,             // ID user pemilik insight mingguan
    val week_start: String? = null,          // Tanggal awal minggu (format: yyyy-MM-dd)
    val week_end: String? = null,            // Tanggal akhir minggu (format: yyyy-MM-dd)
    val total_completed: Int = 0,            // Jumlah total tugas yang diselesaikan selama minggu tersebut
    val total_pending: Int = 0,              // Jumlah total tugas yang belum selesai selama minggu tersebut
    val most_productive_day: String? = null, // Hari dengan jumlah tugas terselesaikan terbanyak (contoh: "Senin")
    val suggestion: String? = null,          // Rekomendasi atau saran otomatis berdasarkan performa minggu ini
    val cluster_label: Int? = null,          // Label hasil clustering (misalnya untuk analisis produktivitas)
    val created_at: String? = null,          // Tanggal insight ini dibuat di database
    val updated_at: String? = null,          // Tanggal insight ini terakhir diperbarui
    val details: List<WeeklyInsightDetail>? = null // Daftar detail insight per hari (relasi 1-to-many ke tabel "weekly_insight_details")
)
