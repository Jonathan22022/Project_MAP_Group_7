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
 * Fragment ini berfungsi untuk menangani proses registrasi pengguna baru.
 * Data pengguna akan dikirim ke Supabase (tabel "users") setelah validasi berhasil.
 */
class RegisterFragment : Fragment() {

    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etNimNik = view.findViewById<EditText>(R.id.etNimNik)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                toast("Semua field wajib diisi")
                return@setOnClickListener
            }

            if (password != confirm) {
                toast("Password tidak sama")
                return@setOnClickListener
            }

            viewModel.register(
                username,
                email,
                password,
                etPhone.text.toString(),
                etNimNik.text.toString()
            )
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                saveSession(user)
                toast("Registrasi berhasil")
                findNavController().navigate(R.id.loginFragment)
            }

            result.onFailure {
                toast(it.message ?: "Terjadi kesalahan")
            }
        }
    }

    private fun saveSession(user: User) {
        val sharedPref =
            requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)

        sharedPref.edit().apply {
            putString("id_user", user.id)
            putString("username", user.username)
            putString("profile_picture", user.profile_picture)
            apply()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
