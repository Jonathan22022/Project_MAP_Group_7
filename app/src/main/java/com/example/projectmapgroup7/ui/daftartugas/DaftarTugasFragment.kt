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
 * Fragment ini menampilkan daftar tugas dalam dua tab:
 * - Tab 1: Tugas yang sedang "IN PROGRESS"
 * - Tab 2: Tugas yang sudah "SELESAI"
 *
 * Menggunakan TabLayout + ViewPager2 untuk navigasi antar tab.
 */
class DaftarTugasFragment : Fragment() {

    // Komponen UI untuk tab dan halaman
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Menghubungkan layout fragment_daftar_tugas.xml ke Fragment
        val view = inflater.inflate(R.layout.fragment_daftar_tugas, container, false)

        // Inisialisasi komponen dari layout
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // Panggil fungsi untuk setup ViewPager dan TabLayout
        setupViewPager()

        return view
    }

    /**
     * Fungsi ini mengatur ViewPager2 agar bisa menampilkan dua fragment:
     * - TaskListFragment dengan status "progress"
     * - TaskListFragment dengan status "selesai"
     *
     * Kemudian TabLayoutMediator digunakan untuk menghubungkan tab dengan halaman ViewPager.
     */
    private fun setupViewPager() {
        // Adapter untuk ViewPager, bertugas menentukan fragment di setiap posisi tab
        val adapter = object : FragmentStateAdapter(this) {
            // Jumlah halaman/tab = 2
            override fun getItemCount(): Int = 2

            // Menentukan fragment yang akan ditampilkan berdasarkan posisi tab
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> TaskListFragment.newInstance("progress") // Tab pertama: tugas sedang berjalan
                    else -> TaskListFragment.newInstance("selesai") // Tab kedua: tugas yang sudah selesai
                }
            }
        }

        // Pasang adapter ke ViewPager
        viewPager.adapter = adapter

        // Daftar judul tab
        val tabTitles = arrayOf("IN PROGRESS", "SELESAI")

        // Menghubungkan TabLayout dengan ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position] // Set teks pada setiap tab sesuai array di atas
        }.attach()
    }
}
