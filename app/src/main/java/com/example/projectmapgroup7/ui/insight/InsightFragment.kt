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
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * 📊 Fragment untuk menampilkan Weekly Insight/Analisis Produktivitas Mingguan
 *
 * Fungsi Utama:
 * - Menampilkan analisis produktivitas tugas mingguan pengguna
 * - Menghitung statistik: tugas selesai, tertunda, hari produktif
 * - Memberikan saran peningkatan berdasarkan algoritma K-Means clustering
 * - Menyimpan dan mengambil data insight dari database Supabase
 *
 * @constructor Membuat fragment insight mingguan
 */
class WeeklyInsightFragment : Fragment() {

    // Binding untuk akses view components
    private var _binding: FragmentInsightBinding? = null
    private val binding get() = _binding!!

    // Client untuk koneksi ke Supabase
    private val client = SupabaseClientInstance.client

    // Format tanggal untuk parsing
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Repository untuk mengelola data insight
    private val insightRepository = WeeklyInsightRepository()

    /**
     * 📐 Membuat tampilan fragment (lifecycle method)
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * 🎯 Setup setelah tampilan dibuat (lifecycle method)
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Muat data insight pertama kali
        loadWeeklyInsight()

        // Setup pull-to-refresh functionality
        binding.swipeRefresh?.setOnRefreshListener {
            loadWeeklyInsight()
        }
    }

    // ==================== DATA LOADING & PROCESSING ====================

    /**
     * 📥 Memuat data insight mingguan dari sumber data
     *
     * Alur:
     * 1. Cek session user
     * 2. Coba ambil data dari repository (Supabase)
     * 3. Jika tidak ada, hitung secara lokal dari data tasks
     * 4. Tampilkan hasil atau error message
     */
    private fun loadWeeklyInsight() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ambil ID user dari shared preferences
                val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val idUser = sharedPref.getString("id_user", null)

                // Validasi user login
                if (idUser.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // 🔹 Coba ambil data insight terbaru dari Supabase
                val (latestInsight, details) = insightRepository.getLatestWeeklyInsight(idUser)

                if (latestInsight != null && details.isNotEmpty()) {
                    // Jika data tersedia di database, render langsung
                    withContext(Dispatchers.Main) {
                        renderInsightFromRepository(latestInsight, details)
                        binding.swipeRefresh?.isRefreshing = false
                    }
                } else {
                    // Jika belum ada data, hitung secara lokal dari tasks
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Belum ada data insight tersimpan — menghitung lokal...", Toast.LENGTH_SHORT).show()
                    }
                    calculateInsightLocally(idUser)
                }
            } catch (e: Exception) {
                // Handle error loading data
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal memuat insight: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    /**
     * 🧮 Menghitung insight secara lokal dari data tasks
     *
     * @param idUser ID user yang sedang login
     *
     * Proses:
     * 1. Ambil data tasks 4 minggu terakhir
     * 2. Kelompokkan tasks per minggu
     * 3. Jalankan clustering untuk analisis pattern
     * 4. Simpan hasil ke database
     * 5. Tampilkan ke UI
     */
    private fun calculateInsightLocally(idUser: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Setup date range: 4 minggu terakhir
                val fromDate = getDateDaysAgo(28)
                val toDate = getDateDaysAgo(0)

                // Ambil tasks dari Supabase dalam rentang tanggal
                val tasks: List<Task> = client.postgrest["tasks"]
                    .select {
                        filter {
                            eq("id_user", idUser)
                            gte("deadline", fromDate)
                            lte("deadline", toDate)
                        }
                    }
                    .decodeList<Task>()

                // 🔹 Kelompokkan tasks berdasarkan minggu
                val weeklyData = aggregateTasksByWeek(tasks)

                // 🔹 Jalankan clustering untuk analisis pattern produktivitas
                val clusterResults = runClusteringOnWeekly(weeklyData.values.toList())

                // 🔹 Ambil data minggu terbaru untuk ditampilkan
                val latestWeekKey = weeklyData.keys.maxOrNull()
                val latestWeek = latestWeekKey?.let { weeklyData[it] }

                withContext(Dispatchers.Main) {
                    if (latestWeek != null) {
                        // Tampilkan data ke UI
                        renderCharts(latestWeek)
                        renderSummary(latestWeek)

                        // Simpan hasil insight ke database
                        lifecycleScope.launch(Dispatchers.IO) {
                            saveInsightToDatabase(idUser, latestWeek)
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

    /**
     * 💾 Menyimpan hasil insight ke database Supabase
     *
     * @param idUser ID user pemilik data
     * @param latestWeek Data agregat mingguan yang akan disimpan
     */
    private suspend fun saveInsightToDatabase(idUser: String, latestWeek: WeeklyAggregate) {
        try {
            // Buat object WeeklyInsight dari data agregat
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

            // 🔹 Insert data utama ke tabel weekly_insights
            val insertedInsight = client.from("weekly_insights")
                .insert(listOf(
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
                )) { select() }
                .decodeSingle<WeeklyInsight>()

            // 🔹 Insert detail data per hari ke tabel weekly_insight_details
            val detailList = latestWeek.completedPerDay.keys.map { day ->
                WeeklyInsightDetail(
                    weekly_insight_id = insertedInsight.id,
                    id_user = idUser,
                    day_of_week = day,
                    completed_count = latestWeek.completedPerDay[day] ?: 0,
                    pending_count = latestWeek.pendingPerDay[day] ?: 0
                )
            }

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
            Log.e("InsightInsert", "Error detail", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Gagal menyimpan insight: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ==================== DATA RENDERING ====================

    /**
     * 🎨 Menampilkan data insight yang diambil dari repository/database
     *
     * @param latestInsight Data insight utama
     * @param details Detail data per hari
     */
    private fun renderInsightFromRepository(latestInsight: WeeklyInsight?, details: List<WeeklyInsightDetail>) {
        // Handle case tidak ada data
        if (latestInsight == null) {
            binding.tvJudul.text = "Insight Mingguan"
            binding.tvSummaryCompleted.text = "Tidak ada data insight tersedia."
            return
        }

        // 🔹 Tampilkan data utama
        binding.tvJudul.text = "Insight Mingguan (${latestInsight.week_start ?: "-"})"
        binding.tvSummaryCompleted.text = "Tugas selesai: ${latestInsight.total_completed}"
        binding.tvSummaryPending.text = "Tugas tertunda: ${latestInsight.total_pending}"
        binding.tvMostProductive.text = "Hari paling produktif: ${latestInsight.most_productive_day ?: "-"}"
        binding.tvSuggestion.text = "Saran: ${latestInsight.suggestion ?: "Pertahankan ritme kerjamu!"}"

        // 🔹 Render grafik batang jika ada data detail
        if (details.isNotEmpty()) {
            renderBarChartFromDetails(details)
        }
    }

    /**
     * 📊 Render grafik batang dari data detail per hari
     *
     * @param details List data detail per hari
     */
    private fun renderBarChartFromDetails(details: List<WeeklyInsightDetail>) {
        val entriesCompleted = ArrayList<BarEntry>()
        val entriesPending = ArrayList<BarEntry>()
        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

        // Siapkan data untuk chart
        for ((i, d) in dayLabels.withIndex()) {
            val dayDetail = details.find { it.day_of_week == d }
            entriesCompleted.add(BarEntry(i.toFloat(), (dayDetail?.completed_count ?: 0).toFloat()))
            entriesPending.add(BarEntry(i.toFloat() + 0.35f, (dayDetail?.pending_count ?: 0).toFloat()))
        }

        // Setup dataset dan chart
        val ds1 = BarDataSet(entriesCompleted, "Selesai")
        val ds2 = BarDataSet(entriesPending, "Tertunda")
        val data = BarData(ds1, ds2)
        data.barWidth = 0.3f

        val chart = binding.barChart
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        chart.xAxis.granularity = 1f
        chart.description.isEnabled = false
        chart.invalidate()
    }

    /**
     * 📈 Render grafik dari data agregat lokal
     *
     * @param insight Data agregat mingguan
     */
    private fun renderCharts(insight: WeeklyAggregate) {
        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val entriesCompleted = ArrayList<BarEntry>()
        val entriesPending = ArrayList<BarEntry>()

        // Siapkan data untuk chart
        for ((i, d) in dayLabels.withIndex()) {
            entriesCompleted.add(BarEntry(i.toFloat(), insight.completedPerDay[d]?.toFloat() ?: 0f))
            entriesPending.add(BarEntry(i.toFloat() + 0.35f, insight.pendingPerDay[d]?.toFloat() ?: 0f))
        }

        // Setup dataset dan chart
        val ds1 = BarDataSet(entriesCompleted, "Selesai")
        val ds2 = BarDataSet(entriesPending, "Tertunda")
        val data = BarData(ds1, ds2)
        data.barWidth = 0.3f

        val chart = binding.barChart
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        chart.xAxis.granularity = 1f
        chart.description.isEnabled = false
        chart.invalidate()
    }

    /**
     * 📝 Menampilkan ringkasan statistik insight
     *
     * @param insight Data agregat mingguan
     */
    private fun renderSummary(insight: WeeklyAggregate) {
        binding.tvJudul.text = "Insight Mingguan (${insight.weekStart})"
        binding.tvSummaryCompleted.text = "Tugas selesai: ${insight.totalCompleted}"
        binding.tvSummaryPending.text = "Tugas tertunda: ${insight.totalPending}"
        binding.tvMostProductive.text = "Hari paling produktif: ${insight.completedPerDay.maxByOrNull { it.value }?.key ?: "-"}"
        binding.tvSuggestion.text = "Saran: ${insight.suggestion}"
    }

    // ==================== DATA PROCESSING & CLUSTERING ====================

    /**
     * 📦 Data class untuk menyimpan agregat data mingguan
     *
     * @property weekStart Tanggal mulai minggu (Senin)
     * @property completedPerDay Map jumlah tugas selesai per hari
     * @property pendingPerDay Map jumlah tugas tertunda per hari
     * @property totalCompleted Total tugas selesai minggu ini
     * @property totalPending Total tugas tertunda minggu ini
     * @property avgPriorityScore Rata-rata skor prioritas tugas
     * @property avgDaysToDeadline Rata-rata hari menuju deadline
     * @property cluster Label cluster dari algoritma K-Means
     * @property suggestion Saran produktivitas berdasarkan cluster
     */
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

    /**
     * 🔢 Konversi prioritas teks ke skor numerik
     *
     * @param p String prioritas ("tinggi", "sedang", "rendah")
     * @return Skor numerik (3, 2, 1)
     */
    private fun priorityScore(p: String?) = when (p?.lowercase()?.trim()) {
        "tinggi" -> 3
        "sedang" -> 2
        "rendah" -> 1
        else -> 2  // default jika tidak dikenali
    }

    /**
     * 📅 Mengelompokkan tasks berdasarkan minggu dan menghitung statistik
     *
     * @param tasks List tasks yang akan diproses
     * @return Map dengan key: tanggal mulai minggu, value: data agregat
     */
    private fun aggregateTasksByWeek(tasks: List<Task>): MutableMap<String, WeeklyAggregate> {
        val map = mutableMapOf<String, MutableList<Task>>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY  // Set Senin sebagai hari pertama minggu

        // 🔹 Kelompokkan tasks berdasarkan minggu (dimulai Senin)
        for (t in tasks) {
            val dl = try { dateFormat.parse(t.deadline) } catch (_: Exception) { null }
            val useDate = dl ?: Date()  // Gunakan tanggal sekarang jika parsing gagal

            // Hitung tanggal Senin dari minggu tersebut
            val weekStartCal = Calendar.getInstance().apply {
                time = useDate
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val key = dateFormat.format(weekStartCal.time)
            map.getOrPut(key) { mutableListOf() }.add(t)
        }

        val result = mutableMapOf<String, WeeklyAggregate>()
        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

        // 🔹 Hitung statistik untuk setiap minggu
        for ((k, list) in map) {
            val completedPerDay = days.associateWith { 0 }.toMutableMap()
            val pendingPerDay = days.associateWith { 0 }.toMutableMap()
            var totalCompleted = 0
            var totalPending = 0
            var sumPriority = 0
            var countPriority = 0
            var sumDaysToDeadline = 0L
            var countDates = 0
            val today = Date()

            for (t in list) {
                val dl = try { dateFormat.parse(t.deadline) } catch (_: Exception) { null }
                val completed = t.is_complete

                if (dl != null) {
                    val dayLabel = dayLabelFromDate(dl)

                    // Hitung jumlah tugas selesai dan tertunda per hari
                    if (completed) {
                        completedPerDay[dayLabel] = (completedPerDay[dayLabel] ?: 0) + 1
                        totalCompleted++
                    } else if (dl.before(today)) {
                        // Tugas dianggap tertunda jika deadline sudah lewat dan belum selesai
                        pendingPerDay[dayLabel] = (pendingPerDay[dayLabel] ?: 0) + 1
                        totalPending++
                    }

                    // Akumulasi data untuk rata-rata
                    sumPriority += priorityScore(t.prioritization)
                    countPriority++

                    // Hitung jarak hari ke deadline
                    val diff = abs((dl.time - today.time) / (1000L * 60 * 60 * 24))
                    sumDaysToDeadline += diff
                    countDates++
                }
            }

            // Hitung nilai rata-rata
            val avgPriority = if (countPriority > 0) sumPriority.toDouble() / countPriority else 2.0
            val avgDays = if (countDates > 0) sumDaysToDeadline.toDouble() / countDates else 0.0

            result[k] = WeeklyAggregate(k, completedPerDay, pendingPerDay, totalCompleted, totalPending, avgPriority, avgDays)
        }
        return result
    }

    /**
     * 🧠 Menjalankan algoritma K-Means clustering pada data mingguan
     *
     * @param items List data agregat mingguan
     * @param k Jumlah cluster (default: 3)
     * @return List label cluster untuk setiap data
     */
    private fun runClusteringOnWeekly(items: List<WeeklyAggregate>, k: Int = 3): List<Int> {
        if (items.isEmpty()) return emptyList()

        // Siapkan data untuk clustering: [completed, pending, avgPriority, avgDaysToDeadline]
        val X = items.map {
            doubleArrayOf(
                it.totalCompleted.toDouble(),
                it.totalPending.toDouble(),
                it.avgPriorityScore,
                it.avgDaysToDeadline
            )
        }

        // Jalankan K-Means clustering
        val kmeans = SimpleKMeans(k = minOf(k, X.size), maxIter = 100)
        val labels = kmeans.fitPredict(X.toTypedArray())

        // Generate saran berdasarkan hasil clustering
        val clusterToSuggestion = generateSuggestionsFromClusters(items, labels)

        // Assign saran ke setiap item berdasarkan clusternya
        for (i in items.indices) {
            items[i].suggestion = clusterToSuggestion[labels[i]] ?: "Pertahankan ritme kerja"
        }

        return labels
    }

    /**
     * 💡 Generate saran produktivitas berdasarkan karakteristik cluster
     *
     * @param items Data agregat mingguan
     * @param labels Hasil label clustering
     * @return Map saran untuk setiap cluster
     */
    private fun generateSuggestionsFromClusters(items: List<WeeklyAggregate>, labels: List<Int>): Map<Int, String> {
        val byCluster = mutableMapOf<Int, MutableList<WeeklyAggregate>>()

        // Kelompokkan data berdasarkan cluster
        for (i in labels.indices) {
            byCluster.getOrPut(labels[i]) { mutableListOf() }.add(items[i])
        }

        val suggestions = mutableMapOf<Int, String>()

        // Analisis setiap cluster untuk generate saran yang sesuai
        for ((lbl, list) in byCluster) {
            val avgCompleted = list.map { it.totalCompleted }.average()
            val avgPending = list.map { it.totalPending }.average()
            val avgPriority = list.map { it.avgPriorityScore }.average()

            // Tentukan saran berdasarkan pattern data
            val s = when {
                avgCompleted < avgPending && avgPriority <= 2.0 ->
                    "Perbaiki prioritas: pecah tugas besar dan selesaikan lebih awal"
                avgCompleted < avgPending && avgPriority > 2.0 ->
                    "Fokus pada penyelesaian tugas penting sebelum menunda"
                avgCompleted >= avgPending && avgPending > 0 ->
                    "Produktif tapi masih ada backlog, alokasikan waktu tambahan"
                else ->
                    "Produktivitas stabil — pertahankan ritmemu!"
            }
            suggestions[lbl] = s
        }
        return suggestions
    }

    // ==================== UTILITY FUNCTIONS ====================

    /**
     * 📅 Konversi Date ke label hari (Sen, Sel, ..., Min)
     *
     * @param date Tanggal yang akan dikonversi
     * @return Label hari dalam bahasa Indonesia
     */
    private fun dayLabelFromDate(date: Date): String = when (Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Sen"
        Calendar.TUESDAY -> "Sel"
        Calendar.WEDNESDAY -> "Rab"
        Calendar.THURSDAY -> "Kam"
        Calendar.FRIDAY -> "Jum"
        Calendar.SATURDAY -> "Sab"
        Calendar.SUNDAY -> "Min"
        else -> "?"
    }

    /**
     * 📅 Mendapatkan tanggal dalam format string beberapa hari yang lalu
     *
     * @param days Jumlah hari yang lalu
     * @return String tanggal dalam format yyyy-MM-dd
     */
    private fun getDateDaysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    /**
     * 🧹 Cleanup resources ketika view dihancurkan
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== K-MEANS CLUSTERING IMPLEMENTATION ====================

/**
 * 🎯 Implementasi sederhana algoritma K-Means clustering
 *
 * @property k Jumlah cluster yang diinginkan
 * @property maxIter Maksimum iterasi untuk konvergensi
 *
 * Algoritma:
 * 1. Inisialisasi centroid secara acak
 * 2. Assign data ke centroid terdekat (Euclidean distance)
 * 3. Update centroid berdasarkan rata-rata anggota cluster
 * 4. Ulangi hingga konvergen atau mencapai maxIter
 */
class SimpleKMeans(private val k: Int = 3, private val maxIter: Int = 100) {
    private lateinit var centroids: Array<DoubleArray>

    /**
     * 🏗️ Melatih model K-Means dan memprediksi cluster untuk data input
     *
     * @param X Array data input (2D array)
     * @return List label cluster untuk setiap data point
     */
    fun fitPredict(X: Array<DoubleArray>): List<Int> {
        if (X.isEmpty()) return emptyList()

        val n = X.size
        val dim = X[0].size
        val kk = minOf(k, n)  // Pastikan k tidak lebih besar dari jumlah data

        // 🔹 Inisialisasi centroid: ambil k data pertama sebagai centroid awal
        centroids = Array(kk) { X[it].copyOf() }
        val labels = IntArray(n)

        // Iterasi hingga konvergen atau mencapai maxIter
        repeat(maxIter) {
            var changed = false

            // 🔹 STEP 1: Assign setiap data point ke centroid terdekat
            for (i in 0 until n) {
                val xi = X[i]
                var best = 0
                var bestDist = dist(xi, centroids[0])

                // Cari centroid terdekat
                for (c in 1 until kk) {
                    val d = dist(xi, centroids[c])
                    if (d < bestDist) {
                        bestDist = d
                        best = c
                    }
                }

                // Update label jika berubah
                if (labels[i] != best) {
                    labels[i] = best
                    changed = true
                }
            }

            // 🔹 STEP 2: Update centroid berdasarkan rata-rata anggota cluster
            val sums = Array(kk) { DoubleArray(dim) }
            val counts = IntArray(kk)

            // Hitung jumlah dan total untuk setiap cluster
            for (i in 0 until n) {
                val lbl = labels[i]
                counts[lbl]++
                for (d in 0 until dim) sums[lbl][d] += X[i][d]
            }

            // Update centroid dengan nilai rata-rata
            for (c in 0 until kk) {
                if (counts[c] > 0) {
                    for (d in 0 until dim) centroids[c][d] = sums[c][d] / counts[c]
                }
            }

            // Stop iterasi jika tidak ada perubahan (konvergen)
            if (!changed) return labels.toList()
        }

        return labels.toList()
    }

    /**
     * 📏 Menghitung Euclidean distance antara dua vektor
     *
     * @param a Vektor pertama
     * @param b Vektor kedua
     * @return Euclidean distance
     */
    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) {
            s += (a[i] - b[i]) * (a[i] - b[i])
        }
        return s
    }
}