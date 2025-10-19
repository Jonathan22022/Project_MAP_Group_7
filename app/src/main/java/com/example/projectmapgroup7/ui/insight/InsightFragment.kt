package com.example.projectmapgroup7.ui.insight

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectmapgroup7.data.model.WeeklyInsight
import com.example.projectmapgroup7.data.model.WeeklyInsightDetail
import com.example.projectmapgroup7.data.remote.SupabaseClientInstance
import com.example.projectmapgroup7.data.repository.WeeklyInsightRepository
import com.example.projectmapgroup7.databinding.FragmentInsightBinding
import com.example.projectmapgroup7.model.Task
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class WeeklyInsightFragment : Fragment() {
    private var _binding: FragmentInsightBinding? = null
    private val binding get() = _binding!!
    private val client = SupabaseClientInstance.client
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Tambahan Repository
    private val insightRepository = WeeklyInsightRepository()

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
        loadWeeklyInsight()
        binding.swipeRefresh?.setOnRefreshListener {
            loadWeeklyInsight()
        }
    }

    private fun loadWeeklyInsight() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)

                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Ambil data insight terbaru dari repository
                val (latestInsight, details) = insightRepository.getLatestWeeklyInsight(idUser)

                if (latestInsight != null && details.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        renderInsightFromRepository(latestInsight, details)
                        binding.swipeRefresh?.isRefreshing = false
                    }
                } else {
                    // Jika belum ada data di Supabase, hitung secara lokal dari tabel tasks
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Belum ada data insight tersimpan — menghitung lokal...", Toast.LENGTH_SHORT).show()
                    }
                    calculateInsightLocally(idUser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal memuat insight: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun calculateInsightLocally(idUser: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fromDate = getDateDaysAgo(28)
                val toDate = getDateDaysAgo(0)

                val tasks: List<Task> = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            gte("deadline", fromDate)
                            lte("deadline", toDate)
                        }
                    }
                    .decodeList<Task>()

                val weeklyData = aggregateTasksByWeek(tasks)
                val clusterResults = runClusteringOnWeekly(weeklyData.values.toList())
                val latestWeekKey = weeklyData.keys.maxOrNull()
                val latestWeek = latestWeekKey?.let { weeklyData[it] }

                withContext(Dispatchers.Main) {
                    if (latestWeek != null) {
                        renderCharts(latestWeek)
                        renderSummary(latestWeek)

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val weeklyInsight = WeeklyInsight(
                                    id_user = idUser,
                                    week_start = latestWeek.weekStart,
                                    week_end = getDateDaysAgo(0),
                                    total_completed = latestWeek.totalCompleted,
                                    total_pending = latestWeek.totalPending,
                                    most_productive_day = latestWeek.completedPerDay.maxByOrNull { it.value }?.key ?: "-",
                                    suggestion = latestWeek.suggestion,
                                    cluster_label = latestWeek.cluster
                                )

                                val insertedInsight = client.from("weekly_insights")
                                    .insert(
                                        listOf(
                                            mapOf(
                                            "id_user" to weeklyInsight.id_user,
                                            "week_start" to weeklyInsight.week_start,
                                            "week_end" to weeklyInsight.week_end,
                                            "total_completed" to weeklyInsight.total_completed,
                                            "total_pending" to weeklyInsight.total_pending,
                                            "most_productive_day" to weeklyInsight.most_productive_day,
                                            "suggestion" to weeklyInsight.suggestion,
                                            "cluster_label" to weeklyInsight.cluster_label
                                            )
                                        )
                                    ) {
                                        select()
                                    }
                                    .decodeSingle<WeeklyInsight>()

                                val detailList = latestWeek.completedPerDay.keys.map { day -> WeeklyInsightDetail(
                                    weekly_insight_id = insertedInsight.id,
                                    id_user = idUser,
                                    day_of_week = day,
                                    completed_count = latestWeek.completedPerDay[day] ?: 0,
                                    pending_count = latestWeek.pendingPerDay[day] ?: 0
                                ) }

                                client.from("weekly_insight_details").insert(
                                    detailList.map { detail ->
                                    mapOf(
                                        "weekly_insight_id" to detail.weekly_insight_id,
                                        "id_user" to detail.id_user,
                                        "day_of_week" to detail.day_of_week,
                                        "completed_count" to detail.completed_count,
                                        "pending_count" to detail.pending_count
                                    )
                                }
                                )

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Insight minggu ini tersimpan!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Gagal menyimpan insight: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                Log.e("InsightInsert", "Error detail", e)
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Tidak ada data minggu ini", Toast.LENGTH_SHORT).show()
                    }
                    binding.swipeRefresh?.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal menghitung insight lokal: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefresh?.isRefreshing = false
                }
            }
        }
    }

    private fun renderInsightFromRepository(
        latestInsight: WeeklyInsight?,
        details: List<WeeklyInsightDetail>
    ) {
        if (latestInsight == null) {
            binding.tvJudul.text = "Insight Mingguan"
            binding.tvSummaryCompleted.text = "Tidak ada data insight tersedia."
            binding.tvSummaryPending.text = ""
            binding.tvMostProductive.text = ""
            binding.tvSuggestion.text = ""
            return
        }

        // --- Tampilkan data insight utama ---
        binding.tvJudul.text = "Insight Mingguan (${latestInsight.week_start ?: "-"})"
        binding.tvSummaryCompleted.text = "Tugas selesai: ${latestInsight.total_completed}"
        binding.tvSummaryPending.text = "Tugas tertunda: ${latestInsight.total_pending}"
        binding.tvMostProductive.text = "Hari paling produktif: ${latestInsight.most_productive_day ?: "-"}"
        binding.tvSuggestion.text = "Saran: ${latestInsight.suggestion ?: "Pertahankan ritme kerjamu!"}"

        // --- Render grafik dari detail harian ---
        if (details.isNotEmpty()) {
            val entriesCompleted = ArrayList<com.github.mikephil.charting.data.BarEntry>()
            val entriesPending = ArrayList<com.github.mikephil.charting.data.BarEntry>()
            val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

            for ((i, d) in dayLabels.withIndex()) {
                val dayDetail = details.find { it.day_of_week == d }
                entriesCompleted.add(BarEntry(i.toFloat(), (dayDetail?.completed_count ?: 0).toFloat()))
                entriesPending.add(BarEntry(i.toFloat() + 0.35f, (dayDetail?.pending_count ?: 0).toFloat()))
            }

            val ds1 = BarDataSet(entriesCompleted, "Selesai")
            val ds2 = BarDataSet(entriesPending, "Tertunda")
            val data = BarData(ds1, ds2)
            data.barWidth = 0.3f

            val chart = binding.barChart
            chart.data = data
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
            chart.xAxis.granularity = 1f
            chart.xAxis.isGranularityEnabled = true
            chart.setFitBars(true)
            chart.description.isEnabled = false
            chart.invalidate()
        }
    }

    // --- Data classes ---
    data class WeeklyAggregate(
        val weekStart: String,
        val completedPerDay: Map<String, Int>,
        val pendingPerDay: Map<String, Int>,
        val totalCompleted: Int,
        val totalPending: Int,
        val avgPriorityScore: Double,
        val avgDaysToDeadline: Double,
        var cluster: Int = 0,
        var suggestion: String = ""
    )

    // Map prioritization to score
    private fun priorityScore(p: String?): Int {
        return when (p?.lowercase()?.trim()) {
            "tinggi" -> 3
            "sedang" -> 2
            "rendah" -> 1
            else -> 2
        }
    }

    private fun aggregateTasksByWeek(tasks: List<Task>): MutableMap<String, WeeklyAggregate> {
        // key = weekStart yyyy-MM-dd (Monday)
        val map = mutableMapOf<String, MutableList<Task>>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        for (t in tasks) {
            val dl = try {
                dateFormat.parse(t.deadline)
            } catch (_: Exception) {
                null
            }

            val useDate = dl ?: Date()
            val weekStartCal = Calendar.getInstance().apply {
                time = useDate
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }

            val key = dateFormat.format(weekStartCal.time)
            val list = map.getOrPut(key) { mutableListOf() }
            list.add(t)
        }

        val result = mutableMapOf<String, WeeklyAggregate>()
        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

        for ((k, list) in map) {
            val completedPerDay = mutableMapOf<String, Int>().apply {
                days.forEach { put(it, 0) }
            }
            val pendingPerDay = mutableMapOf<String, Int>().apply {
                days.forEach { put(it, 0) }
            }

            var totalCompleted = 0
            var totalPending = 0
            var sumPriority = 0
            var countPriority = 0
            var sumDaysToDeadline = 0L
            var countDates = 0
            val today = Date()

            for (t in list) {
                val dl = try {
                    dateFormat.parse(t.deadline)
                } catch (_: Exception) {
                    null
                }

                val completed = t.is_complete

                if (dl != null) {
                    val dayLabel = dayLabelFromDate(dl)

                    if (completed) {
                        completedPerDay[dayLabel] = (completedPerDay[dayLabel] ?: 0) + 1
                        totalCompleted++
                    } else {
                        // if deadline before today => pending
                        if (dl.before(today)) {
                            pendingPerDay[dayLabel] = (pendingPerDay[dayLabel] ?: 0) + 1
                            totalPending++
                        }
                    }

                    // priority score
                    sumPriority += priorityScore(t.prioritization)
                    countPriority++

                    // days to deadline from created date approximated by difference from today
                    val diff = abs((dl.time - today.time) / (1000L * 60 * 60 * 24))
                    sumDaysToDeadline += diff
                    countDates++
                }
            }

            val avgPriority = if (countPriority > 0) sumPriority.toDouble() / countPriority else 2.0
            val avgDays = if (countDates > 0) sumDaysToDeadline.toDouble() / countDates else 0.0

            result[k] = WeeklyAggregate(
                weekStart = k,
                completedPerDay = completedPerDay,
                pendingPerDay = pendingPerDay,
                totalCompleted = totalCompleted,
                totalPending = totalPending,
                avgPriorityScore = avgPriority,
                avgDaysToDeadline = avgDays
            )
        }
        return result
    }

    // --- Simple KMeans implementation for small data ---
    private fun runClusteringOnWeekly(items: List<WeeklyAggregate>, k: Int = 3): List<Int> {
        if (items.isEmpty()) return emptyList()

        // Build feature vectors: [totalCompleted, totalPending, avgPriorityScore, avgDaysToDeadline]
        val X = items.map {
            doubleArrayOf(
                it.totalCompleted.toDouble(),
                it.totalPending.toDouble(),
                it.avgPriorityScore,
                it.avgDaysToDeadline
            )
        }

        val kmeans = SimpleKMeans(k = minOf(k, X.size), maxIter = 100)
        val labels = kmeans.fitPredict(X.toTypedArray())

        // After clustering, create simple mapping cluster -> suggestion
        val clusterToSuggestion = generateSuggestionsFromClusters(items, labels)

        // apply suggestions
        for (i in items.indices) {
            items[i].suggestion = clusterToSuggestion[labels[i]] ?: "Pertahankan ritme kerja"
        }

        return labels
    }

    private fun generateSuggestionsFromClusters(items: List<WeeklyAggregate>, labels: List<Int>): Map<Int, String> {
        val byCluster = mutableMapOf<Int, MutableList<WeeklyAggregate>>()

        for (i in labels.indices) {
            val lbl = labels[i]
            byCluster.getOrPut(lbl) { mutableListOf() }.add(items[i])
        }

        val suggestions = mutableMapOf<Int, String>()

        for ((lbl, list) in byCluster) {
            val avgCompleted = list.map { it.totalCompleted }.average()
            val avgPending = list.map { it.totalPending }.average()
            val avgPriority = list.map { it.avgPriorityScore }.average()

            val s = when {
                avgCompleted < avgPending && avgPriority <= 2.0 ->
                    "Perbaiki prioritas: pecah tugas besar dan selesaikan lebih awal"
                avgCompleted < avgPending && avgPriority > 2.0 ->
                    "Fokus pada penyelesaian tugas penting sebelum menunda"
                avgCompleted >= avgPending && avgPending > 0 ->
                    "Produktif tetapi masih ada tugas tertunda — pertimbangkan alokasi waktu khusus untuk menyelesaikan backlog"
                else ->
                    "Produktivitas stabil — pertahankan dan pertimbangkan automasi untuk tugas berulang"
            }
            suggestions[lbl] = s
        }

        return suggestions
    }

    // --- Rendering ---
    private fun renderCharts(insight: WeeklyAggregate) {
        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val entriesCompleted = ArrayList<BarEntry>()
        val entriesPending = ArrayList<BarEntry>()

        for ((i, d) in dayLabels.withIndex()) {
            entriesCompleted.add(BarEntry(i.toFloat(), insight.completedPerDay[d]?.toFloat() ?: 0f))
            entriesPending.add(BarEntry(i.toFloat() + 0.35f, insight.pendingPerDay[d]?.toFloat() ?: 0f))
        }

        val ds1 = BarDataSet(entriesCompleted, "Selesai")
        val ds2 = BarDataSet(entriesPending, "Tertunda")
        val data = BarData(ds1, ds2)
        data.barWidth = 0.3f

        val chart = binding.barChart
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        chart.xAxis.granularity = 1f
        chart.xAxis.isGranularityEnabled = true
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.invalidate()
    }

    private fun renderSummary(insight: WeeklyAggregate) {
        binding.tvJudul.text = "Insight Mingguan (${insight.weekStart})"
        binding.tvSummaryCompleted.text = "Tugas selesai minggu ini: ${insight.totalCompleted}"
        val totalPending = insight.pendingPerDay.values.sum()
        binding.tvSummaryPending.text = "Tugas tertunda minggu ini: $totalPending"
        val mostProductive = insight.completedPerDay.maxByOrNull { it.value }?.key ?: "Belum pasti"
        binding.tvMostProductive.text = "Waktu paling produktif: $mostProductive"
        binding.tvSuggestion.text = "Saran: ${insight.suggestion}"
    }

    private fun dayLabelFromDate(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Sen"
            Calendar.TUESDAY -> "Sel"
            Calendar.WEDNESDAY -> "Rab"
            Calendar.THURSDAY -> "Kam"
            Calendar.FRIDAY -> "Jum"
            Calendar.SATURDAY -> "Sab"
            Calendar.SUNDAY -> "Min"
            else -> "?"
        }
    }

    private fun getDateDaysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ----------------- Simple KMeans class -----------------
class SimpleKMeans(private val k: Int = 3, private val maxIter: Int = 100) {
    private lateinit var centroids: Array<DoubleArray>

    fun fitPredict(X: Array<DoubleArray>): List<Int> {
        if (X.isEmpty()) return emptyList()

        val n = X.size
        val dim = X[0].size
        val kk = minOf(k, n)

        // init centroids - pick first k distinct rows (or random)
        centroids = Array(kk) { DoubleArray(dim) }
        for (i in 0 until kk) centroids[i] = X[i].copyOf()

        val labels = IntArray(n)

        repeat(maxIter) {
            var changed = false

            // assign
            for (i in 0 until n) {
                val xi = X[i]
                var best = 0
                var bestDist = dist(xi, centroids[0])

                for (c in 1 until kk) {
                    val d = dist(xi, centroids[c])
                    if (d < bestDist) {
                        bestDist = d
                        best = c
                    }
                }

                if (labels[i] != best) {
                    labels[i] = best
                    changed = true
                }
            }

            // update centroids
            val sums = Array(kk) { DoubleArray(dim) { 0.0 } }
            val counts = IntArray(kk)

            for (i in 0 until n) {
                val lbl = labels[i]
                counts[lbl]++
                for (d in 0 until dim) sums[lbl][d] += X[i][d]
            }

            for (c in 0 until kk) {
                if (counts[c] > 0) {
                    for (d in 0 until dim) centroids[c][d] = sums[c][d] / counts[c]
                }
            }

            if (!changed) return labels.toList()
        }

        return labels.toList()
    }

    fun fitPredict(X: Array<DoubleArray>, seed: Long = System.currentTimeMillis()): List<Int> = fitPredict(X)

    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) s += (a[i] - b[i]) * (a[i] - b[i])
        return s
    }
}