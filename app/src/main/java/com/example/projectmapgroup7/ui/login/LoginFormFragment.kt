package com.example.projectmapgroup7.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import com.example.projectmapgroup7.util.HashUtils
import com.example.projectmapgroup7.databinding.FragmentLoginFormBinding

class LoginFormFragment : Fragment() {

    // View binding untuk mengakses elemen UI secara langsung tanpa findViewById
    private var _binding: FragmentLoginFormBinding? = null
    private val binding get() = _binding!!

    // Dipanggil untuk membuat tampilan fragment dari layout XML
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Menghubungkan layout dengan binding class
        _binding = FragmentLoginFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Dipanggil setelah view selesai dibuat
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Event listener untuk tombol "Login"
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val hashedPassword = HashUtils.sha256(password) // Hash password sebelum dikirim ke server

            // Validasi input kosong
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Isi username dan password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val client = SupabaseClientInstance.client // Mengambil instance Supabase client

            // Jalankan proses login di thread coroutine (asynchronous)
            lifecycleScope.launch {
                try {
                    // 🔹 Query ke tabel "users" di Supabase untuk mencocokkan username & password hash
                    val users = client.postgrest["users"]
                        .select {
                            filter {
                                eq("username", username)
                                eq("password", hashedPassword)
                            }
                        }
                        .decodeList<User>() // Mengubah hasil query menjadi daftar objek User

                    // Jika user ditemukan (login sukses)
                    if (users.isNotEmpty()) {
                        val user = users.first()
                        val usernameFromSupabase = user.username
                        val profilePictureUrlFromSupabase = user.profile_picture

                        // 🔹 Simpan sesi login menggunakan SharedPreferences
                        val sharedPref = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("id_user", user.id)
                            putString("username", usernameFromSupabase)
                            putString("profile_picture", profilePictureUrlFromSupabase)
                            apply()
                        }

                        Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()

                        // 🔹 Arahkan user ke halaman home dan hapus fragment login dari backstack
                        findNavController().navigate(
                            R.id.action_loginFormFragment_to_nav_home,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true) // Hapus login dari backstack agar tidak bisa kembali ke halaman login
                                .build()
                        )
                    } else {
                        // Jika user tidak ditemukan, tampilkan pesan error
                        Toast.makeText(requireContext(), "Username/password salah!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Tangani error seperti koneksi gagal atau query error
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }

        // 🔹 Tombol "Register Sekarang" — navigasi ke halaman register
        binding.btnRegisterNow.setOnClickListener {
            findNavController().navigate(R.id.action_loginFormFragment_to_registerFragment)
        }
    }

    // Bersihkan binding untuk menghindari memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
