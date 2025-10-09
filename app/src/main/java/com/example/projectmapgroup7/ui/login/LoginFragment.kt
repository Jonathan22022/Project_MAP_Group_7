package com.example.projectmapgroup7.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.projectmapgroup7.R

//menampilkan halaman login, ketika ditekan tombol login akan menampilkan popup untuk mengisi form login yang ada di LoginFromFragment

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_loginFormFragment)
        }

        val btnRegister: Button = view.findViewById(R.id.btnRegister)
        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        return view
    }
}
