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

/**
 * WeeklyInsightFragment
 *
 * Fragment ini bertugas menampilkan insight mingguan aktivitas pengguna,
 * seperti:
 * - Jumlah tugas yang selesai dan tertunda
 * - Hari paling produktif dalam satu minggu
 * - Saran peningkatan produktivitas
 * - Visualisasi data dalam bentuk Bar Chart
 *
 * Data diperoleh dari WeeklyInsightViewModel dan ditampilkan
 * menggunakan pendekatan MVVM (LiveData + Observer).
 */
class WeeklyInsightFragment : Fragment() {

    // ViewBinding untuk fragment_insight.xml
    // Digunakan agar akses view lebih aman dan terstruktur
    private var _binding: FragmentInsightBinding? = null
    private val binding get() = _binding!!

    // ViewModel untuk mengelola data insight mingguan
    private val viewModel: WeeklyInsightViewModel by viewModels()

    /**
     * Membuat dan menginisialisasi tampilan Fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Dipanggil setelah view berhasil dibuat
     * Digunakan untuk:
     * - Mengatur observer ViewModel
     * - Memuat data awal
     * - Menangani aksi swipe refresh
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        loadData()

        // Swipe untuk refresh data insight
        binding.swipeRefresh?.setOnRefreshListener {
            loadData()
        }
    }

    /**
     * Mengatur observer untuk semua LiveData yang ada di ViewModel
     */
    private fun setupObservers() {

        // Observer status loading (untuk SwipeRefreshLayout)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh?.isRefreshing = isLoading
        }

        // Observer pesan error
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                // Bisa ditampilkan menggunakan Toast / Snackbar
                // Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        // Observer data insight mingguan (teks ringkasan)
        viewModel.weeklyInsightData.observe(viewLifecycleOwner) { insightData ->
            insightData?.let {
                binding.tvJudul.text = "Insight Mingguan (${it.weekStart})"
                binding.tvSummaryCompleted.text =
                    "Tugas selesai: ${it.totalCompleted}"
                binding.tvSummaryPending.text =
                    "Tugas tertunda: ${it.totalPending}"
                binding.tvMostProductive.text =
                    "Hari paling produktif: ${it.mostProductiveDay}"
                binding.tvSuggestion.text =
                    "Saran: ${it.suggestion}"
            }
        }

        // Observer data grafik (Bar Chart)
        viewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            chartData?.let {
                renderChart(it)
            }
        }
    }

    /**
     * Memuat data insight mingguan berdasarkan user yang sedang login
     */
    private fun loadData() {
        lifecycleScope.launch {
            val sharedPref = requireActivity()
                .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

            val idUser = sharedPref.getString("id_user", null)

            // Jika user belum login, data tidak dimuat
            if (idUser.isNullOrEmpty()) {
                return@launch
            }

            // Meminta ViewModel untuk mengambil insight mingguan
            viewModel.loadWeeklyInsight(idUser)
        }
    }

    /**
     * Menampilkan grafik Bar Chart berdasarkan data insight
     */
    private fun renderChart(
        chartData: WeeklyInsightViewModel.ChartData
    ) {
        // Data tugas selesai
        val entriesCompleted = chartData.completedEntries.map {
            BarEntry(it.first, it.second)
        }

        // Data tugas tertunda
        val entriesPending = chartData.pendingEntries.map {
            BarEntry(it.first, it.second)
        }

        // Dataset grafik
        val dsCompleted = BarDataSet(entriesCompleted, "Selesai")
        val dsPending = BarDataSet(entriesPending, "Tertunda")

        // Gabungkan dataset
        val data = BarData(dsCompleted, dsPending)
        data.barWidth = 0.3f

        // Konfigurasi chart
        val chart = binding.barChart
        chart.data = data
        chart.xAxis.valueFormatter =
            IndexAxisValueFormatter(chartData.dayLabels)
        chart.xAxis.granularity = 1f
        chart.description.isEnabled = false
        chart.invalidate() // refresh chart
    }

    /**
     * Membersihkan binding saat Fragment dihancurkan
     * untuk mencegah memory leak
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
