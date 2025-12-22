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

    // Deklarasi ImageView untuk menampilkan foto profil user
    private lateinit var imageProfile: ImageView

    // TextView untuk menampilkan username
    private lateinit var tvUsername: TextView

    // TextView untuk menampilkan usia akun
    private lateinit var tvAccountAge: TextView

    // TextView untuk menampilkan total task user
    private lateinit var tvTotalTasks: TextView

    // TextView untuk menampilkan jumlah task yang sudah selesai
    private lateinit var tvCompletedTasks: TextView

    // Inisialisasi AccountViewModel menggunakan delegate viewModels()
    // ViewModel ini bertanggung jawab mengelola data akun
    private val viewModel: AccountViewModel by viewModels()

    /**
     * Method lifecycle Fragment untuk membuat tampilan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_account.xml
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Menghubungkan komponen UI dengan id pada layout XML
        imageProfile = view.findViewById(R.id.imageProfile)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvAccountAge = view.findViewById(R.id.tvAccountAge)
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks)

        // Memuat data session user dari SharedPreferences
        loadSession()

        // Mengamati perubahan data dari ViewModel
        observeViewModel()

        // Mengembalikan view yang sudah dibuat
        return view
    }

    /**
     * Mengambil data session user dari SharedPreferences
     * dan menampilkannya ke UI
     */
    private fun loadSession() {

        // Mengambil SharedPreferences dengan nama "user_session"
        val pref = requireActivity()
            .getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)

        // Mengambil data username, foto profil, dan ID user
        val username = pref.getString("username", "Guest") ?: "Guest"
        val profilePicture = pref.getString("profile_picture", null)
        val userId = pref.getString("id_user", null)

        // Menampilkan username ke TextView
        tvUsername.text = username

        // Jika user memiliki foto profil, tampilkan menggunakan Glide
        if (!profilePicture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicture)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageProfile)
        }

        // Jika user sudah login (bukan Guest), ambil data akun dari ViewModel
        if (username != "Guest" && userId != null) {
            viewModel.loadAccountData(username, userId)
        }
    }

    /**
     * Mengamati LiveData dari ViewModel
     * dan memperbarui UI ketika data berubah
     */
    private fun observeViewModel() {

        // Observer untuk usia akun
        viewModel.accountAge.observe(viewLifecycleOwner) {
            tvAccountAge.text = "Usia Akun: $it"
        }

        // Observer untuk total task user
        viewModel.totalTasks.observe(viewLifecycleOwner) {
            tvTotalTasks.text = "Total Tugas: $it"
        }

        // Observer untuk jumlah task yang sudah selesai
        viewModel.completedTasks.observe(viewLifecycleOwner) {
            tvCompletedTasks.text = "Tugas Selesai: $it"
        }
    }
}
