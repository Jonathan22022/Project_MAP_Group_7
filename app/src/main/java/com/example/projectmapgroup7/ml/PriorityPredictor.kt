package com.example.projectmapgroup7.ml

// Import library yang dibutuhkan
import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.io.FileInputStream
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * PriorityPredictor
 * -----------------
 * Kelas ini berfungsi untuk memuat dan menjalankan model TensorFlow Lite (.tflite)
 * yang digunakan untuk memprediksi tingkat prioritas sebuah tugas
 * berdasarkan input vektor (fitur dari deskripsi teks).
 */
class PriorityPredictor(context: Context) {

    // Interpreter adalah objek utama untuk menjalankan model TensorFlow Lite
    private val interpreter: Interpreter

    init {
        // Saat objek dibuat, model .tflite akan dimuat dari folder assets
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
    }

    /**
     * Fungsi untuk memuat file model .tflite dari folder assets.
     * File akan di-mapping ke dalam memori agar bisa diakses langsung oleh TensorFlow.
     */
    private fun loadModelFile(context: Context): MappedByteBuffer {
        // Buka file model dari folder assets
        val fileDescriptor = context.assets.openFd("priority_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        // Mapping file ke memori dengan mode READ_ONLY
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Fungsi utama untuk melakukan prediksi.
     * @param vector: data input dalam bentuk array float (misal hasil ekstraksi teks)
     * @return Int: indeks dari kelas prediksi (0 = rendah, 1 = sedang, 2 = tinggi)
     */
    fun predictPriority(vector: FloatArray): Int {
        // TensorFlow Lite membutuhkan ByteBuffer sebagai input
        val inputBuffer = ByteBuffer.allocateDirect(4 * vector.size) // 4 byte per float
        inputBuffer.order(ByteOrder.nativeOrder()) // Urutan byte sesuai perangkat
        for (v in vector) inputBuffer.putFloat(v) // Masukkan data ke buffer

        // Output model berbentuk array 2D [1][3], misal 3 kelas (rendah, sedang, tinggi)
        val output = Array(1) { FloatArray(3) }

        // Jalankan model inference
        interpreter.run(inputBuffer, output)

        // Cari indeks dengan nilai probabilitas tertinggi
        // (misal output[0] = [0.1, 0.3, 0.6] → hasil = 2)
        return output[0].indices.maxByOrNull { output[0][it] } ?: 1 // default: sedang
    }
}
