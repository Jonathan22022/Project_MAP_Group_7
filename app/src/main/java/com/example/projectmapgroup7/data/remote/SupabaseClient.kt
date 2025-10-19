package com.example.projectmapgroup7.data.remote

// Import library Supabase untuk membuat koneksi dan fitur pendukungnya
import com.example.projectmapgroup7.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * SupabaseClientInstance
 * ----------------------
 * Object Singleton ini digunakan untuk membuat dan menyimpan instance
 * client Supabase yang bisa digunakan di seluruh aplikasi.
 *
 * Tujuannya agar koneksi ke Supabase (database & storage)
 * hanya dibuat sekali dan dapat diakses dengan mudah.
 */
object SupabaseClientInstance {

    // Inisialisasi client Supabase utama
    val client = createSupabaseClient(
        // URL dan KEY diambil dari BuildConfig (agar aman dan tidak ditulis langsung di kode)
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        // Install modul Postgrest → untuk operasi database (insert, select, update, delete)
        install(Postgrest)

        // Install modul Storage → untuk upload dan akses file (misalnya gambar tugas)
        install(Storage)
    }
}
