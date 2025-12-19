package com.example.projectmapgroup7.DetailTugas

import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.R
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.projectmapgroup7.viewmodel.DetailTugasViewModel

class DetailTugasFragment : Fragment() {

    private val viewModel: DetailTugasViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_detail_tugas, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvDetailDescription)
        val tvDeadline = view.findViewById<TextView>(R.id.tvDetailDeadline)
        val tvPriority = view.findViewById<TextView>(R.id.tvDetailPriority)
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val btnDone = view.findViewById<Button>(R.id.btnMarkAsDone)
        val btnEdit = view.findViewById<Button>(R.id.btnEditTask)
        val switchNotif = view.findViewById<Switch>(R.id.switchNotification)

        val title = arguments?.getString("title") ?: return view
        val deadline = arguments?.getString("deadline")

        tvTitle.text = title
        tvDesc.text = arguments?.getString("description")
        tvDeadline.text = "Deadline: ${deadline ?: "-"}"
        tvPriority.text = "Prioritas: ${arguments?.getString("priority")}"

        arguments?.getString("image_url")?.let {
            Glide.with(this).load(it).into(ivImage)
        }

        // MARK AS DONE
        btnDone.setOnClickListener {
            val pref = requireActivity()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE)

            val userId = pref.getString("id_user", null) ?: return@setOnClickListener
            viewModel.markTaskDone(userId, title)
        }

        // OBSERVE
        viewModel.doneSuccess.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(requireContext(), "Tugas selesai ✅", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        // NOTIFICATION SWITCH
        val notifPref = requireActivity()
            .getSharedPreferences("task_notifications", Context.MODE_PRIVATE)

        switchNotif.isChecked = notifPref.getBoolean("${title}_notif", true)

        switchNotif.setOnCheckedChangeListener { _, checked ->
            notifPref.edit().putBoolean("${title}_notif", checked).apply()

            if (checked && deadline != null) {
                viewModel.scheduleNotification(requireContext(), title, deadline)
            } else {
                viewModel.cancelNotification(requireContext(), title)
            }
        }

        return view
    }
}
