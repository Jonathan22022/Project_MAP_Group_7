package com.example.projectmapgroup7.ui.insight

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projectmapgroup7.databinding.FragmentInsightBinding
import com.example.projectmapgroup7.viewmodel.WeeklyInsightViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class WeeklyInsightFragment : Fragment() {

    private var _binding: FragmentInsightBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeeklyInsightViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        loadData()

        binding.swipeRefresh?.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh?.isRefreshing = isLoading
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                // Tampilkan error ke user (bisa menggunakan Toast atau Snackbar)
                // Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        // Observe insight data
        viewModel.weeklyInsightData.observe(viewLifecycleOwner) { insightData ->
            insightData?.let {
                binding.tvJudul.text = "Insight Mingguan (${it.weekStart})"
                binding.tvSummaryCompleted.text = "Tugas selesai: ${it.totalCompleted}"
                binding.tvSummaryPending.text = "Tugas tertunda: ${it.totalPending}"
                binding.tvMostProductive.text = "Hari paling produktif: ${it.mostProductiveDay}"
                binding.tvSuggestion.text = "Saran: ${it.suggestion}"
            }
        }

        // Observe chart data
        viewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            chartData?.let {
                renderChart(it)
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", null)

            if (idUser.isNullOrEmpty()) {
                // Handle user not logged in
                return@launch
            }

            viewModel.loadWeeklyInsight(idUser)
        }
    }

    private fun renderChart(chartData: WeeklyInsightViewModel.ChartData) {
        val entriesCompleted = chartData.completedEntries.map {
            BarEntry(it.first, it.second)
        }
        val entriesPending = chartData.pendingEntries.map {
            BarEntry(it.first, it.second)
        }

        val ds1 = BarDataSet(entriesCompleted, "Selesai")
        val ds2 = BarDataSet(entriesPending, "Tertunda")
        val data = BarData(ds1, ds2)
        data.barWidth = 0.3f

        val chart = binding.barChart
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(chartData.dayLabels)
        chart.xAxis.granularity = 1f
        chart.description.isEnabled = false
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}