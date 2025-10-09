package com.example.projectmapgroup7.ui.insight

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.projectmapgroup7.databinding.FragmentInsightBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate

class InsightFragment : Fragment() {

    private var _binding: FragmentInsightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightBinding.inflate(inflater, container, false)

        setupBarChart(binding.barChart)

        return binding.root
    }

    private fun setupBarChart(barChart: BarChart) {
        val entriesCompleted = listOf(
            BarEntry(0f, 5f),
            BarEntry(1f, 7f),
            BarEntry(2f, 4f),
            BarEntry(3f, 9f),
            BarEntry(4f, 3f),
            BarEntry(5f, 4f),
            BarEntry(6f, 2f)
        )

        val entriesPending = listOf(
            BarEntry(0f, 3f),
            BarEntry(1f, 2f),
            BarEntry(2f, 5f),
            BarEntry(3f, 1f),
            BarEntry(4f, 4f),
            BarEntry(5f, 3f),
            BarEntry(6f, 2f)
        )

        val dataSet1 = BarDataSet(entriesCompleted, "Tugas Selesai")
        dataSet1.color = ColorTemplate.COLORFUL_COLORS[3]

        val dataSet2 = BarDataSet(entriesPending, "Tugas Tertunda")
        dataSet2.color = ColorTemplate.COLORFUL_COLORS[0]

        val data = BarData(dataSet1, dataSet2)
        data.barWidth = 0.35f

        barChart.data = data
        barChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        )
        barChart.xAxis.granularity = 1f
        barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        barChart.description = Description().apply {
            text = "Produktivitas Mingguan"
            textColor = Color.BLACK
            textSize = 12f
        }

        barChart.legend.isEnabled = true
        barChart.setFitBars(true)
        barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}