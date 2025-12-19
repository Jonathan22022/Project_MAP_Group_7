package com.example.projectmapgroup7.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.viewmodel.AccountViewModel

class AccountFragment : Fragment() {

    private lateinit var imageProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvAccountAge: TextView
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        imageProfile = view.findViewById(R.id.imageProfile)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvAccountAge = view.findViewById(R.id.tvAccountAge)
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks)

        loadSession()
        observeViewModel()

        return view
    }

    private fun loadSession() {
        val pref = requireActivity()
            .getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)

        val username = pref.getString("username", "Guest") ?: "Guest"
        val profilePicture = pref.getString("profile_picture", null)
        val userId = pref.getString("id_user", null)

        tvUsername.text = username

        if (!profilePicture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicture)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageProfile)
        }

        if (username != "Guest" && userId != null) {
            viewModel.loadAccountData(username, userId)
        }
    }

    private fun observeViewModel() {
        viewModel.accountAge.observe(viewLifecycleOwner) {
            tvAccountAge.text = "Usia Akun: $it"
        }

        viewModel.totalTasks.observe(viewLifecycleOwner) {
            tvTotalTasks.text = "Total Tugas: $it"
        }

        viewModel.completedTasks.observe(viewLifecycleOwner) {
            tvCompletedTasks.text = "Tugas Selesai: $it"
        }
    }
}
