package com.example.projectmapgroup7.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.projectmapgroup7.util.HashUtils
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        val username = view.findViewById<EditText>(R.id.etUsername)
        val email = view.findViewById<EditText>(R.id.etEmail)
        val password = view.findViewById<EditText>(R.id.etPassword)
        val confirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val phone = view.findViewById<EditText>(R.id.etPhone)
        val nimNik = view.findViewById<EditText>(R.id.etNimNik)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val usernameVal = username.text.toString().trim()
            val emailVal = email.text.toString().trim()
            val passwordVal = password.text.toString().trim()
            val confirmVal = confirmPassword.text.toString().trim()
            val phoneVal = phone.text.toString().trim()
            val nimNikVal = nimNik.text.toString().trim()

            // Validasi input
            if (usernameVal.isEmpty() || emailVal.isEmpty() || passwordVal.isEmpty() || confirmVal.isEmpty()) {
                Toast.makeText(requireContext(), "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordVal != confirmVal) {
                Toast.makeText(requireContext(), "Password tidak sama!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordVal.length < 6) {
                Toast.makeText(requireContext(), "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(usernameVal, emailVal, passwordVal, phoneVal, nimNikVal)
        }

        return view
    }

    private fun registerUser(
        username: String,
        email: String,
        password: String,
        phone: String,
        nimNik: String
    ) {
        val client = SupabaseClientInstance.client

        lifecycleScope.launch {
            try {
                // Cek apakah username sudah ada
                val existingUser = client.postgrest["users"]
                    .select {
                        filter {
                            eq("username", username)
                        }
                    }
                    .decodeList<User>()

                if (existingUser.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Username sudah digunakan!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Buat objek user
                val hashedPassword = HashUtils.sha256(password)

                val newUser = User(
                    username = username,
                    email = email,
                    password = hashedPassword,
                    phone = if (phone.isEmpty()) null else phone,
                    nim_nik = if (nimNik.isEmpty()) null else nimNik
                )

                // Insert user baru menggunakan data class
                client.postgrest["users"].insert(newUser)
                // Ambil user yang baru dibuat
                val createdUser = client.postgrest["users"]
                    .select {
                        filter {
                            eq("username", username)
                        }
                    }
                    .decodeSingle<User>()

                // Simpan ID User agar langsung login otomatis
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("id_user", createdUser.id)
                    putString("username", createdUser.username)
                    putString("profile_picture", createdUser.profile_picture)
                    apply()
                }

                Toast.makeText(requireContext(), "Registrasi berhasil!", Toast.LENGTH_SHORT).show()

                // Navigate back to login
                findNavController().navigate(R.id.loginFragment)

            } catch (e: Exception) {
                // Error handling yang lebih detail
                val errorMessage = when {
                    e.message?.contains("duplicate key") == true -> "Username atau email sudah digunakan"
                    e.message?.contains("network") == true -> "Koneksi internet bermasalah"
                    else -> "Gagal registrasi: ${e.message}"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}