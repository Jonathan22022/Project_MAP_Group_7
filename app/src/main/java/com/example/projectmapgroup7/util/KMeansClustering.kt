package com.example.projectmapgroup7.util

class KMeansClustering(private val k: Int = 3, private val maxIter: Int = 100) {
    private lateinit var centroids: Array<DoubleArray>

    fun fitPredict(X: Array<DoubleArray>): List<Int> {
        if (X.isEmpty()) return emptyList()

        val n = X.size
        val dim = X[0].size
        val kk = minOf(k, n)

        centroids = Array(kk) { X[it].copyOf() }
        val labels = IntArray(n)

        repeat(maxIter) {
            var changed = false

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

            val sums = Array(kk) { DoubleArray(dim) }
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

    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) {
            s += (a[i] - b[i]) * (a[i] - b[i])
        }
        return s
    }
}