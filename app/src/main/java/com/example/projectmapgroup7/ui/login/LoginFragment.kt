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

    // Fungsi ini dipanggil ketika fragment pertama kali dibuat dan ingin menampilkan tampilan layout-nya.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 🔹 Inflate layout XML 'fragment_login' agar bisa ditampilkan di layar.
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // 🔹 Ambil referensi tombol "Login" dari layout.
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        // Ketika tombol login ditekan, navigasikan ke fragment form login.
        // Fragment tujuan: LoginFormFragment
        findNavController().apply {
            btnLogin.setOnClickListener {
                navigate(R.id.action_loginFragment_to_loginFormFragment)
            }
        }

        // 🔹 Ambil referensi tombol "Register" dari layout.
        val btnRegister: Button = view.findViewById(R.id.btnRegister)
        // Ketika tombol register ditekan, navigasikan ke fragment registrasi.
        // Fragment tujuan: RegisterFragment
        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // 🔹 Kembalikan tampilan (view) fragment agar ditampilkan ke pengguna.
        return view
    }
}
