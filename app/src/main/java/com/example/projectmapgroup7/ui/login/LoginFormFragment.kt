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
import com.example.projectmapgroup7.databinding.FragmentLoginFormBinding

class LoginFormFragment : Fragment() {

    private var _binding: FragmentLoginFormBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Isi username dan password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val client = SupabaseClientInstance.client

            lifecycleScope.launch {
                try {
                    val users = client.postgrest["users"]
                        .select {
                            filter {
                                eq("username", username)
                                eq("password", password)
                            }
                        }
                        .decodeList<User>()

                    if (users.isNotEmpty()) {
                        val user = users.first()
                        val usernameFromSupabase = user.username
                        val profilePictureUrlFromSupabase = user.profile_picture

                        // Simpan sesi user
                        val sharedPref = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("username", usernameFromSupabase)
                            putString("profile_picture", profilePictureUrlFromSupabase)
                            apply()
                        }

                        Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()

                        // Navigasi ke home dan hapus login dari backstack
                        findNavController().navigate(
                            R.id.action_loginFormFragment_to_nav_home,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build()
                        )
                    } else {
                        Toast.makeText(requireContext(), "Username/password salah!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }

        binding.btnRegisterNow.setOnClickListener {
            findNavController().navigate(R.id.action_loginFormFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}