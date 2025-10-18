package com.example.projectmapgroup7.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.io.FileInputStream
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PriorityPredictor(context: Context) {

    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("priority_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predictPriority(vector: FloatArray): Int {
        val inputBuffer = ByteBuffer.allocateDirect(4 * vector.size)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (v in vector) inputBuffer.putFloat(v)

        val output = Array(1) { FloatArray(3) } // misal: 3 kelas (rendah, sedang, tinggi)
        interpreter.run(inputBuffer, output)

        return output[0].indices.maxByOrNull { output[0][it] } ?: 1
    }
}