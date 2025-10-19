package com.example.projectmapgroup7.model

import kotlinx.serialization.Serializable

/**
 * 🧾 Data class Task
 *
 * Kelas ini merepresentasikan satu data tugas (task) yang dimiliki oleh seorang pengguna.
 * Data ini biasanya digunakan untuk menyimpan, menampilkan, dan mengelola daftar tugas
 * baik yang sedang berlangsung maupun yang sudah selesai.
 */
@Serializable // Annotation agar objek bisa diubah menjadi JSON (misalnya saat dikirim ke atau dari Supabase)
data class Task(
    val title: String,               // Judul tugas yang dibuat pengguna
    val description: String,         // Deskripsi atau rincian tugas
    val image_url: String? = null,   // (Opsional) URL gambar yang terkait dengan tugas (misalnya ikon, ilustrasi, atau lampiran)
    val prioritization: String,      // Tingkat prioritas tugas (contoh: "Tinggi", "Sedang", "Rendah")
    val deadline: String,            // Tenggat waktu penyelesaian tugas (format: yyyy-MM-dd atau ISO datetime)
    val is_complete: Boolean = false,// Status tugas (false = belum selesai, true = sudah selesai)
    val id_user: String              // ID pengguna yang memiliki tugas ini (relasi ke tabel pengguna)
)
