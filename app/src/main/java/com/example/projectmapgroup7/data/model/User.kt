package com.example.projectmapgroup7.data.model

// Import untuk memungkinkan serialisasi otomatis objek User ke/dari JSON (misalnya saat komunikasi dengan Supabase)
import kotlinx.serialization.Serializable

/**
 * User
 * -----
 * Data class ini merepresentasikan informasi pengguna dalam aplikasi.
 *
 * Objek ini digunakan untuk menyimpan dan mengirim data user ke/dari database Supabase.
 */
@Serializable // Menandakan bahwa class ini bisa dikonversi otomatis ke/dari JSON
data class User(
    // ID unik pengguna (biasanya di-generate otomatis oleh Supabase)
    val id: String? = null,

    // Nama pengguna atau username untuk login/tampilan profil
    val username: String,

    // Alamat email pengguna
    val email: String,

    // Kata sandi pengguna (biasanya dienkripsi sebelum disimpan di database)
    val password: String,

    // Nomor telepon pengguna (opsional)
    val phone: String? = null,

    // NIM atau NIK pengguna (opsional, tergantung konteks aplikasi)
    val nim_nik: String? = null,

    // URL foto profil pengguna (default diarahkan ke gambar placeholder di Supabase Storage)
    val profile_picture: String? = "https://tgbjsowzhpoogknygtly.supabase.co/storage/v1/object/public/profile/ic_account_.xml"
)
