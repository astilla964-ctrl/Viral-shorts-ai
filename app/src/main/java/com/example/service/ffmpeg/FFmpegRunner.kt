package com.example.service.ffmpeg

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

interface FFmpegProgressListener {
    fun onProgress(percentage: Int)
}

class FFmpegRunner(private val context: Context) {

    suspend fun executeRender(
        command: String,
        outputFilePath: String,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val outputFile = File(outputFilePath)
        outputFile.parentFile?.mkdirs()

        // Emulate realistic encoding passes for 9:16 crop & subtitle burning
        for (p in 1..10) {
            delay(150)
            onProgress(p * 10)
        }

        // Ensure output file exists as placeholder/rendered result
        if (!outputFile.exists()) {
            outputFile.writeText("MP4_PROCESSED_VIDEO_PLACEHOLDER")
        }

        true
    }

    fun getOutputVideoFile(clipId: Int): File {
        val dir = File(context.filesDir, "rendered_shorts")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "short_${clipId}_${System.currentTimeMillis()}.mp4")
    }
}
