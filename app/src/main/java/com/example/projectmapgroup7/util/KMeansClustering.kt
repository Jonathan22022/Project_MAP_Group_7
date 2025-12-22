package com.example.projectmapgroup7.util

/**
 * KMeansClustering
 *
 * Implementasi sederhana algoritma K-Means Clustering.
 * Digunakan untuk mengelompokkan data numerik ke dalam k cluster
 * berdasarkan jarak Euclidean.
 *
 * @param k jumlah cluster (default = 3)
 * @param maxIter jumlah iterasi maksimum (default = 100)
 */
class KMeansClustering(
    private val k: Int = 3,
    private val maxIter: Int = 100
) {

    // Menyimpan centroid (titik pusat) dari setiap cluster
    private lateinit var centroids: Array<DoubleArray>

    /**
     * Melakukan proses training (fit) dan prediksi cluster
     *
     * @param X data input dalam bentuk array 2D
     *          X[n][d] = data ke-n dengan d dimensi
     * @return List label cluster untuk setiap data
     */
    fun fitPredict(X: Array<DoubleArray>): List<Int> {

        // Jika data kosong, kembalikan list kosong
        if (X.isEmpty()) return emptyList()

        val n = X.size          // jumlah data
        val dim = X[0].size    // jumlah dimensi fitur

        // Jika jumlah cluster > jumlah data, batasi k = n
        val kk = minOf(k, n)

        // =================================================
        // INISIALISASI CENTROID
        // =================================================
        // Mengambil k data pertama sebagai centroid awal
        centroids = Array(kk) { X[it].copyOf() }

        // Label cluster untuk setiap data
        val labels = IntArray(n)

        // =================================================
        // ITERASI K-MEANS
        // =================================================
        repeat(maxIter) {

            var changed = false // Menandai apakah label berubah

            // =====================
            // ASSIGNMENT STEP
            // =====================
            // Menentukan cluster terdekat untuk setiap data
            for (i in 0 until n) {

                val xi = X[i]
                var best = 0
                var bestDist = dist(xi, centroids[0])

                // Bandingkan jarak ke semua centroid
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

            // =====================
            // UPDATE STEP
            // =====================
            // Hitung centroid baru dari rata-rata anggota cluster
            val sums = Array(kk) { DoubleArray(dim) }
            val counts = IntArray(kk)

            for (i in 0 until n) {
                val lbl = labels[i]
                counts[lbl]++
                for (d in 0 until dim) {
                    sums[lbl][d] += X[i][d]
                }
            }

            // Update nilai centroid
            for (c in 0 until kk) {
                if (counts[c] > 0) {
                    for (d in 0 until dim) {
                        centroids[c][d] = sums[c][d] / counts[c]
                    }
                }
            }

            // Jika tidak ada perubahan label, konvergen → stop
            if (!changed) return labels.toList()
        }

        // Jika iterasi maksimum tercapai
        return labels.toList()
    }

    /**
     * Menghitung jarak Euclidean kuadrat
     * (tanpa akar, karena hanya untuk perbandingan)
     *
     * @param a vektor data
     * @param b vektor centroid
     * @return jarak Euclidean kuadrat
     */
    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) {
            s += (a[i] - b[i]) * (a[i] - b[i])
        }
        return s
    }
}
