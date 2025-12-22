package com.example.projectmapgroup7.ui.daftartugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.projectmapgroup7.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2

/**
 * Fragment yang menampilkan daftar tugas dalam dua tab:
 * 1. IN PROGRESS → tugas yang belum selesai
 * 2. SELESAI → tugas yang sudah diselesaikan
 *
 * Navigasi antar tab menggunakan TabLayout dan ViewPager2
 */
class DaftarTugasFragment : Fragment() {

    // TabLayout untuk menampilkan tab navigasi
    private lateinit var tabLayout: TabLayout

    // ViewPager2 untuk menampilkan fragment sesuai tab yang dipilih
    private lateinit var viewPager: ViewPager2

    /**
     * Lifecycle Fragment untuk membuat tampilan UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout fragment_daftar_tugas.xml
        val view = inflater.inflate(
            R.layout.fragment_daftar_tugas,
            container,
            false
        )

        // Inisialisasi TabLayout dan ViewPager2 dari layout
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // Setup ViewPager dan TabLayout
        setupViewPager()

        // Kembalikan view yang sudah dibuat
        return view
    }

    /**
     * Mengatur ViewPager2 dan TabLayout
     * untuk menampilkan fragment sesuai tab
     */
    private fun setupViewPager() {

        // Adapter ViewPager untuk mengatur fragment pada tiap tab
        viewPager.adapter = object : FragmentStateAdapter(this) {

            // Jumlah tab yang ditampilkan
            override fun getItemCount() = 2

            // Menentukan fragment berdasarkan posisi tab
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) {
                    // Tab pertama: tugas yang belum selesai
                    TaskListFragment.newInstance(false)
                } else {
                    // Tab kedua: tugas yang sudah selesai
                    TaskListFragment.newInstance(true)
                }
            }
        }

        // Menghubungkan TabLayout dengan ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = if (pos == 0) "IN PROGRESS" else "SELESAI"
        }.attach()
    }
}
