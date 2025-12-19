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

class LoginFormFragment : Fragment() {

    private var _binding: FragmentLoginFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                toast("Isi username dan password!")
                return@setOnClickListener
            }

            viewModel.login(username, password)
        }

        binding.btnRegisterNow.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFormFragment_to_registerFragment
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                saveSession(user)
                toast("Login berhasil!")

                findNavController().navigate(
                    R.id.action_loginFormFragment_to_nav_home,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build()
                )
            }

            result.onFailure {
                toast(it.message ?: "Terjadi kesalahan")
            }
        }
    }

    private fun saveSession(user: User) {
        val sharedPref =
            requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
