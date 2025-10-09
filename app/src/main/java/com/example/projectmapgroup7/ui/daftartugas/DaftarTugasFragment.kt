package com.example.projectmapgroup7.ui.daftartugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectmapgroup7.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DaftarTugasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_daftar_tugas, container, false)

        val tabSelesai: TextView = view.findViewById(R.id.tabSelesai)
        val tabProgress: TextView = view.findViewById(R.id.tabProgress)
        val fabDelete: FloatingActionButton = view.findViewById(R.id.fabDelete)

        tabSelesai.setOnClickListener {
            tabSelesai.setTextColor(resources.getColor(R.color.purple_500))
            tabProgress.setTextColor(resources.getColor(R.color.gray))
            tabSelesai.setBackgroundResource(R.drawable.tab_selected_bg)
            tabProgress.background = null
            Toast.makeText(requireContext(), "Menampilkan tugas selesai", Toast.LENGTH_SHORT).show()
        }

        tabProgress.setOnClickListener {
            tabProgress.setTextColor(resources.getColor(R.color.purple_500))
            tabSelesai.setTextColor(resources.getColor(R.color.gray))
            tabProgress.setBackgroundResource(R.drawable.tab_selected_bg)
            tabSelesai.background = null
            Toast.makeText(requireContext(), "Menampilkan tugas in progress", Toast.LENGTH_SHORT).show()
        }

        fabDelete.setOnClickListener {
            Toast.makeText(requireContext(), "Tugas dihapus", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
