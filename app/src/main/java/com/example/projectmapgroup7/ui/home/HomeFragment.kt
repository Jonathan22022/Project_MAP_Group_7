package com.example.projectmapgroup7.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectmapgroup7.databinding.FragmentHomeBinding
import com.example.projectmapgroup7.R
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // ✅ Ambil NavigationView dari Activity Utama
        val navigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val imageViewProfile = headerView.findViewById<ImageView>(R.id.imageViewProfile)

        val sharedPref = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Guest")
        val profilePictureUrl = sharedPref.getString("profile_picture", null)

        tvUserName.text = userName

        if (profilePictureUrl != null) {
            Glide.with(this)
                .load(profilePictureUrl)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageViewProfile)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
