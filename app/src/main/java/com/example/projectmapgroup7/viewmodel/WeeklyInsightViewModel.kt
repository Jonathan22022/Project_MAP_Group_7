package com.example.projectmapgroup7.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmapgroup7.data.model.WeeklyInsight
import com.example.projectmapgroup7.data.model.WeeklyInsightDetail
import com.example.projectmapgroup7.data.repository.WeeklyInsightRepository
import com.example.projectmapgroup7.model.Task
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * WeeklyInsightViewModel
 *
 * ViewModel yang bertanggung jawab untuk:
 * - Mengelola logika Weekly Insight (analisis produktivitas mingguan)
 * - Mengambil data dari database jika tersedia
 * - Menghitung insight secara lokal jika data belum tersedia
 * - Menyediakan data untuk grafik, ringkasan, dan rekomendasi
 *
 * ViewModel ini mengikuti arsitektur MVVM dan menggunakan Coroutine
 * untuk operasi asynchronous.
 */
class WeeklyInsightViewModel(
    private val insightRepository: WeeklyInsightRepository = WeeklyInsightRepository()
) : ViewModel() {

    // =====================
    // STATE MANAGEMENT
    // =====================

    // Menyimpan status loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Menyimpan pesan error
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Data utama weekly insight yang akan ditampilkan di UI
    private val _weeklyInsightData = MutableLiveData<WeeklyInsightData>()
    val weeklyInsightData: LiveData<WeeklyInsightData> = _weeklyInsightData

    // Data grafik (completed vs pending per hari)
    private val _chartData = MutableLiveData<ChartData>()
    val chartData: LiveData<ChartData> = _chartData

    // Rekomendasi / saran produktivitas
    private val _suggestions = MutableLiveData<String?>()
    val suggestions: MutableLiveData<String?> = _suggestions

    // Formatter tanggal standar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // =====================
    // DATA CLASS UNTUK UI
    // =====================

    /**
     * Data ringkasan weekly insight
     */
    data class WeeklyInsightData(
        val weekStart: String,
        val totalCompleted: Int,
        val totalPending: Int,
        val mostProductiveDay: String,
        val suggestion: String
    )

    /**
     * Data grafik mingguan
     */
    data class ChartData(
        val completedEntries: List<Pair<Float, Float>>,
        val pendingEntries: List<Pair<Float, Float>>,
        val dayLabels: List<String>
    )

    // =====================
    // MAIN ENTRY POINT
    // =====================

    /**
     * Memuat weekly insight untuk user tertentu
     * - Prioritas mengambil dari database
     * - Jika tidak ada, hitung secara lokal
     */
    fun loadWeeklyInsight(idUser: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = ""

                // Ambil insight terakhir dari database
                val (latestInsight, details) =
                    insightRepository.getLatestWeeklyInsight(idUser)

                if (latestInsight != null && details.isNotEmpty()) {
                    // Gunakan data yang tersimpan
                    renderInsightFromRepository(latestInsight, details)
                } else {
                    // Hitung ulang secara lokal
                    calculateInsightLocally(idUser)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat insight: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================
    // RENDER DATA DARI DB
    // =====================

    /**
     * Menampilkan insight dari data repository
     */
    private fun renderInsightFromRepository(
        latestInsight: WeeklyInsight,
        details: List<WeeklyInsightDetail>
    ) {
        _weeklyInsightData.value = WeeklyInsightData(
            weekStart = latestInsight.week_start ?: "-",
            totalCompleted = latestInsight.total_completed,
            totalPending = latestInsight.total_pending,
            mostProductiveDay = latestInsight.most_productive_day ?: "-",
            suggestion = latestInsight.suggestion ?: "Pertahankan ritme kerjamu!"
        )

        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val completedEntries = mutableListOf<Pair<Float, Float>>()
        val pendingEntries = mutableListOf<Pair<Float, Float>>()

        // Mapping data detail ke grafik
        for ((i, day) in dayLabels.withIndex()) {
            val detail = details.find { it.day_of_week == day }
            completedEntries.add(i.toFloat() to (detail?.completed_count ?: 0).toFloat())
            pendingEntries.add(i.toFloat() + 0.35f to (detail?.pending_count ?: 0).toFloat())
        }

        _chartData.value = ChartData(completedEntries, pendingEntries, dayLabels)
        _suggestions.value = latestInsight.suggestion
    }

    // =====================
    // PERHITUNGAN LOKAL
    // =====================

    /**
     * Menghitung weekly insight secara lokal dari data task
     */
    private suspend fun calculateInsightLocally(idUser: String) {
        try {
            val fromDate = getDateDaysAgo(28)
            val toDate = getDateDaysAgo(0)

            // Ambil task dari Supabase
            val client =
                com.example.projectmapgroup7.data.remote.SupabaseClientInstance.client
            val tasks: List<Task> = client.postgrest["tasks"]
                .select {
                    filter {
                        eq("id_user", idUser)
                        gte("deadline", fromDate)
                        lte("deadline", toDate)
                    }
                }
                .decodeList()

            // Kelompokkan task per minggu
            val weeklyData = aggregateTasksByWeek(tasks)

            val latestWeekKey = weeklyData.keys.maxOrNull()
            val latestWeek = latestWeekKey?.let { weeklyData[it] }

            if (latestWeek != null) {
                renderInsightFromAggregate(latestWeek)
                saveInsightToDatabase(idUser, latestWeek)
            } else {
                _errorMessage.value = "Tidak ada data minggu ini"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Gagal menghitung insight lokal: ${e.message}"
        }
    }

    /**
     * Menampilkan hasil agregasi lokal ke UI
     */
    private fun renderInsightFromAggregate(insight: WeeklyAggregate) {
        _weeklyInsightData.value = WeeklyInsightData(
            weekStart = insight.weekStart,
            totalCompleted = insight.totalCompleted,
            totalPending = insight.totalPending,
            mostProductiveDay =
                insight.completedPerDay.maxByOrNull { it.value }?.key ?: "-",
            suggestion = insight.suggestion
        )

        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val completedEntries = mutableListOf<Pair<Float, Float>>()
        val pendingEntries = mutableListOf<Pair<Float, Float>>()

        for ((i, day) in dayLabels.withIndex()) {
            completedEntries.add(i.toFloat() to (insight.completedPerDay[day] ?: 0).toFloat())
            pendingEntries.add(i.toFloat() + 0.35f to (insight.pendingPerDay[day] ?: 0).toFloat())
        }

        _chartData.value = ChartData(completedEntries, pendingEntries, dayLabels)
        _suggestions.value = insight.suggestion
    }

    /**
     * Menyimpan weekly insight hasil perhitungan ke database
     */
    private suspend fun saveInsightToDatabase(
        idUser: String,
        weeklyAggregate: WeeklyAggregate
    ) {
        try {
            val weeklyInsight = WeeklyInsight(
                id_user = idUser,
                week_start = weeklyAggregate.weekStart,
                week_end = getDateDaysAgo(0),
                total_completed = weeklyAggregate.totalCompleted,
                total_pending = weeklyAggregate.totalPending,
                most_productive_day =
                    weeklyAggregate.completedPerDay.maxByOrNull { it.value }?.key ?: "-",
                suggestion = weeklyAggregate.suggestion,
                cluster_label = weeklyAggregate.cluster
            )

            insightRepository.insertWeeklyInsight(weeklyInsight)
        } catch (e: Exception) {
            _errorMessage.value = "Gagal menyimpan insight: ${e.message}"
        }
    }

    // =====================
    // AGREGASI DATA
    // =====================

    /**
     * Struktur data hasil agregasi mingguan
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
     * Mengelompokkan task berdasarkan minggu
     */
    private fun aggregateTasksByWeek(
        tasks: List<Task>
    ): MutableMap<String, WeeklyAggregate> {

        val map = mutableMapOf<String, MutableList<Task>>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        // Kelompokkan task berdasarkan awal minggu
        for (t in tasks) {
            val dl = try { dateFormat.parse(t.deadline) } catch (_: Exception) { null }
            val useDate = dl ?: Date()

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

                    if (completed) {
                        completedPerDay[dayLabel] =
                            (completedPerDay[dayLabel] ?: 0) + 1
                        totalCompleted++
                    } else if (dl.before(today)) {
                        pendingPerDay[dayLabel] =
                            (pendingPerDay[dayLabel] ?: 0) + 1
                        totalPending++
                    }

                    sumPriority += priorityScore(t.prioritization)
                    countPriority++

                    val diff =
                        abs((dl.time - today.time) / (1000L * 60 * 60 * 24))
                    sumDaysToDeadline += diff
                    countDates++
                }
            }

            val avgPriority =
                if (countPriority > 0)
                    sumPriority.toDouble() / countPriority
                else 2.0

            val avgDays =
                if (countDates > 0)
                    sumDaysToDeadline.toDouble() / countDates
                else 0.0

            result[k] = WeeklyAggregate(
                k,
                completedPerDay,
                pendingPerDay,
                totalCompleted,
                totalPending,
                avgPriority,
                avgDays
            )
        }
        return result
    }

    // =====================
    // HELPER FUNCTIONS
    // =====================

    /**
     * Konversi label prioritas menjadi skor numerik
     */
    private fun priorityScore(p: String?) = when (p?.lowercase()?.trim()) {
        "tinggi" -> 3
        "sedang" -> 2
        "rendah" -> 1
        else -> 2
    }

    /**
     * Konversi tanggal ke label hari
     */
    private fun dayLabelFromDate(date: Date): String =
        when (Calendar.getInstance().apply { time = date }
            .get(Calendar.DAY_OF_WEEK)) {
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
     * Mengambil tanggal N hari ke belakang
     */
    private fun getDateDaysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }
}
