package com.example.projectmapgroup7.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientInstance {
    val client = createSupabaseClient(
        supabaseUrl = "https://tgbjsowzhpoogknygtly.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRnYmpzb3d6aHBvb2drbnlndGx5Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1OTgzODI3OSwiZXhwIjoyMDc1NDE0Mjc5fQ.6fr2_PNYm2BYJ-ArjUbRAzblbF7vp_lL_k-Sp8zvMwQ"
    ) {
        install(Postgrest)
        install(Storage)
    }
}
