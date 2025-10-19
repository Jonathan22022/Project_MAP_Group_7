package com.example.projectmapgroup7.util

// Import library untuk melakukan hashing menggunakan algoritma SHA-256
import java.security.MessageDigest

// Object HashUtils digunakan sebagai utilitas (helper) untuk melakukan hashing data
object HashUtils {

    /**
     * Fungsi untuk menghasilkan hash SHA-256 dari input string.
     *
     * @param input: String yang ingin di-hash.
     * @return String hasil hash dalam format heksadesimal.
     */
    fun sha256(input: String): String {
        // Mengambil instance dari algoritma SHA-256
        val bytes = MessageDigest.getInstance("SHA-256")
            // Mengubah input string menjadi array byte dan menghitung nilai hash-nya
            .digest(input.toByteArray())

        // Mengubah setiap byte hasil hash menjadi string heksadesimal dua digit
        // dan menggabungkannya menjadi satu string hasil akhir
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
