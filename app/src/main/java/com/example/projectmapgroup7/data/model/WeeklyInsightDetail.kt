package com.example.projectmapgroup7.data.model

// Import untuk membuat kelas ini bisa diserialisasi (misalnya ke JSON) secara otomatis
import kotlinx.serialization.Serializable

/**
 * WeeklyInsightDetail
 * --------------------
 * Data class ini merepresentasikan detail insight mingguan dari pengguna.
 *
 * Setiap objek berisi informasi mengenai:
 * - jumlah tugas yang diselesaikan dan belum selesai,
 * - hari dalam seminggu,
 * - serta keterkaitan dengan pengguna dan insight mingguan utama.
 */
@Serializable // Annotation ini memungkinkan konversi otomatis ke/dari JSON (diperlukan untuk Supabase)
data class WeeklyInsightDetail(
    // ID unik dari data detail insight mingguan (bisa null jika belum disimpan di server)
    val id: String? = null,

    // ID dari tabel "weekly_insight" (relasi dengan entitas induk WeeklyInsight)
    val weekly_insight_id: String? = null,

    // ID pengguna yang memiliki insight ini
    val id_user: String? = null,

    // Nama hari (contoh: "Senin", "Selasa", dst)
    val day_of_week: String,

    // Jumlah tugas yang sudah diselesaikan pada hari tersebut
    val completed_count: Int = 0,

    // Jumlah tugas yang masih tertunda pada hari tersebut
    val pending_count: Int = 0,

    // Waktu data ini dibuat (biasanya otomatis dari database Supabase)
    val created_at: String? = null
)
