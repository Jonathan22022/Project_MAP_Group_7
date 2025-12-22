package com.example.projectmapgroup7.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R

/**
 * LoginFragment
 *
 * Fragment ini merupakan halaman awal autentikasi aplikasi.
 * Menampilkan dua tombol utama:
 * 1. Login    → mengarahkan pengguna ke halaman form login
 * 2. Register → mengarahkan pengguna ke halaman pendaftaran akun baru
 *
 * Fragment ini berfungsi sebagai gerbang awal sebelum
 * pengguna masuk ke sistem aplikasi.
 */
class LoginFragment : Fragment() {

    /**
     * Membuat dan menampilkan tampilan Fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_login.xml
        val view = inflater.inflate(
            R.layout.fragment_login,
            container,
            false
        )

        // Tombol Login → navigasi ke LoginFormFragment
        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_loginFormFragment
            )
        }

        // Tombol Register → navigasi ke RegisterFragment
        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }

        // Kembalikan view yang telah dibuat
        return view
    }
}
