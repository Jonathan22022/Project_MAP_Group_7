package com.example.projectmapgroup7.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R

// 🔹 Fragment ini menampilkan halaman awal login,
// yang berisi dua tombol utama: "Login" dan "Register".
// Ketika tombol "Login" ditekan, aplikasi akan menampilkan popup form login (LoginFormFragment).
// Sedangkan tombol "Register" akan mengarahkan pengguna ke halaman registrasi (RegisterFragment).

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_loginFormFragment
            )
        }

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }

        return view
    }
}