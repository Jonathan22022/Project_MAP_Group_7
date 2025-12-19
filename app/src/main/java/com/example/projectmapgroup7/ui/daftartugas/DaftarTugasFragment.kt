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

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_daftar_tugas, container, false)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        setupViewPager()
        return view
    }

    private fun setupViewPager() {
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int): Fragment {
                return if (position == 0)
                    TaskListFragment.newInstance(false)
                else
                    TaskListFragment.newInstance(true)
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = if (pos == 0) "IN PROGRESS" else "SELESAI"
        }.attach()
    }
}
