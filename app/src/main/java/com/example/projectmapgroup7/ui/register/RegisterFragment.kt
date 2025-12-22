package com.example.projectmapgroup7.ui.register

// Import komponen Android & library yang digunakan
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.projectmapgroup7.util.HashUtils
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.model.User
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.viewmodel.RegisterViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * RegisterFragment
 *
 * Fragment ini digunakan untuk proses registrasi pengguna baru.
 * Pengguna diminta mengisi data seperti:
 * - Username
 * - Email
 * - Password dan konfirmasi password
 * - Nomor telepon
 * - NIM / NIK
 *
 * Setelah validasi berhasil, data akan dikirim melalui RegisterViewModel
 * untuk disimpan ke database (Supabase).
 *
 * Arsitektur yang digunakan: MVVM
 */
class RegisterFragment : Fragment() {

    // ViewModel untuk menangani logika registrasi user
    private lateinit var viewModel: RegisterViewModel

    /**
     * Membuat dan menampilkan tampilan Fragment Register
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_register.xml
        val view = inflater.inflate(
            R.layout.fragment_register,
            container,
            false
        )

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        // Inisialisasi komponen input
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etNimNik = view.findViewById<EditText>(R.id.etNimNik)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        // Tombol Register ditekan
        btnRegister.setOnClickListener {

            // Ambil input user
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            // Validasi input kosong
            if (
                username.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty() ||
                confirm.isEmpty()
            ) {
                toast("Semua field wajib diisi")
                return@setOnClickListener
            }

            // Validasi kesesuaian password
            if (password != confirm) {
                toast("Password tidak sama")
                return@setOnClickListener
            }

            // Proses registrasi melalui ViewModel
            viewModel.register(
                username = username,
                email = email,
                password = password,
                phone = etPhone.text.toString(),
                nimNik = etNimNik.text.toString()
            )
        }

        // Mengamati hasil registrasi
        observeViewModel()

        return view
    }

    /**
     * Mengamati status registrasi dari ViewModel
     */
    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { result ->

            // Jika registrasi berhasil
            result.onSuccess { user ->
                saveSession(user)
                toast("Registrasi berhasil")

                // Kembali ke halaman login
                findNavController().navigate(R.id.loginFragment)
            }

            // Jika registrasi gagal
            result.onFailure {
                toast(it.message ?: "Terjadi kesalahan")
            }
        }
    }

    /**
     * Menyimpan data user ke SharedPreferences
     * sebagai session login
     */
    private fun saveSession(user: User) {
        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                Context.MODE_PRIVATE
            )

        sharedPref.edit().apply {
            putString("id_user", user.id)
            putString("username", user.username)
            putString("profile_picture", user.profile_picture)
            apply()
        }
    }

    /**
     * Fungsi utilitas untuk menampilkan Toast
     */
    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
