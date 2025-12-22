package com.example.projectmapgroup7.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.example.projectmapgroup7.viewmodel.LoginViewModel

/**
 * LoginFormFragment
 *
 * Fragment ini digunakan untuk proses autentikasi pengguna (login).
 * Pengguna memasukkan username dan password, kemudian data tersebut
 * diverifikasi melalui LoginViewModel.
 *
 * Jika login berhasil:
 * - Data user disimpan ke SharedPreferences (session)
 * - Pengguna diarahkan ke halaman Home
 *
 * Arsitektur yang digunakan: MVVM
 */
class LoginFormFragment : Fragment() {

    // ViewBinding untuk fragment_login_form.xml
    // Digunakan untuk mengakses view secara aman
    private var _binding: FragmentLoginFormBinding? = null
    private val binding get() = _binding!!

    // ViewModel untuk menangani logika login
    private lateinit var viewModel: LoginViewModel

    /**
     * Membuat tampilan Fragment menggunakan ViewBinding
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Dipanggil setelah view selesai dibuat
     * Digunakan untuk:
     * - Inisialisasi ViewModel
     * - Menangani aksi klik tombol
     * - Mengamati perubahan data dari ViewModel
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Tombol Login
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validasi input
            if (username.isEmpty() || password.isEmpty()) {
                toast("Isi username dan password!")
                return@setOnClickListener
            }

            // Proses login melalui ViewModel
            viewModel.login(username, password)
        }

        // Navigasi ke halaman Register
        binding.btnRegisterNow.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFormFragment_to_registerFragment
            )
        }

        // Mengamati hasil login
        observeViewModel()
    }

    /**
     * Mengamati perubahan state login dari ViewModel
     */
    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { result ->

            // Jika login berhasil
            result.onSuccess { user ->
                saveSession(user)
                toast("Login berhasil!")

                // Navigasi ke Home dan menghapus Login dari back stack
                findNavController().navigate(
                    R.id.action_loginFormFragment_to_nav_home,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build()
                )
            }

            // Jika login gagal
            result.onFailure {
                toast(it.message ?: "Terjadi kesalahan")
            }
        }
    }

    /**
     * Menyimpan data user ke SharedPreferences
     * Digunakan sebagai session login
     */
    private fun saveSession(user: User) {
        val sharedPref = requireActivity()
            .getSharedPreferences(
                "user_session",
                AppCompatActivity.MODE_PRIVATE
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

    /**
     * Membersihkan binding saat Fragment dihancurkan
     * untuk mencegah memory leak
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
