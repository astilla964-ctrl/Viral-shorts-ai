package com.example.service.ffmpeg

import android.content.Context
import com.example.data.model.ClipCandidate
import com.example.data.model.WordCaption
import java.io.File

object FFmpegCommandBuilder {

    /**
     * Builds the exact FFmpeg command string requested for Android (ffmpeg-kit-full):
     * - Trims the video using -ss and -to
     * - Crops 1920x1080 horizontal to 1080x1920 vertical: crop=ih*(9/16):ih:(iw-ow)/2:0
     * - Burns animated subtitles with custom force_style:
     *   Fontname=Roboto,Fontsize=22,PrimaryColour=&H0000FFFF,Bold=1,Alignment=2
     */
    fun buildCommand(
        inputVideoPath: String,
        outputVideoPath: String,
        srtSubtitlePath: String,
        startTimeSeconds: Long,
        endTimeSeconds: Long
    ): String {
        val srtEscaped = srtSubtitlePath.replace("\\", "/").replace(":", "\\:")
        val startFormatted = formatTimestampToHms(startTimeSeconds)
        val endFormatted = formatTimestampToHms(endTimeSeconds)

        return """ffmpeg -y -ss $startFormatted -to $endFormatted -i "$inputVideoPath" -vf "crop=ih*(9/16):ih:(iw-ow)/2:0,subtitles='$srtEscaped':force_style='Fontname=Roboto,Fontsize=22,PrimaryColour=&H0000FFFF,Bold=1,Alignment=2'" -c:v libx264 -preset ultrafast -crf 23 -c:a aac -b:a 128k "$outputVideoPath""""
    }

    /**
     * Generates a kinetic, word-by-word animated SubRip (.srt) subtitle string
     * with yellow/white highlighting for the active word.
     */
    fun generateKineticSrt(clip: ClipCandidate): String {
        val sb = StringBuilder()
        val captions = clip.captions
        if (captions.isEmpty()) {
            return generateFallbackSrt(clip)
        }

        // Group words into short rhythmic 3-5 word bursts for maximum retention
        val chunkSize = 4
        var srtIndex = 1

        for (i in captions.indices step chunkSize) {
            val chunk = captions.subList(i, minOf(i + chunkSize, captions.size))
            val chunkStart = chunk.first().start
            val chunkEnd = chunk.last().end

            // Each word in the chunk can be highlighted in turn
            for (wIndex in chunk.indices) {
                val activeWord = chunk[wIndex]
                val subStart = activeWord.start
                val subEnd = activeWord.end

                val formattedText = chunk.joinToString(" ") { wc ->
                    if (wc == activeWord) {
                        "<font color=\"#FFE600\"><b>${wc.word.uppercase()}</b></font>"
                    } else {
                        "<font color=\"#FFFFFF\">${wc.word}</font>"
                    }
                }

                sb.append(srtIndex++)
                sb.append("\n")
                sb.append(formatTimeForSrt(subStart))
                sb.append(" --> ")
                sb.append(formatTimeForSrt(subEnd))
                sb.append("\n")
                sb.append(formattedText)
                sb.append("\n\n")
            }
        }

        return sb.toString()
    }

    private fun generateFallbackSrt(clip: ClipCandidate): String {
        return """1
${formatTimeForSrt(clip.startTimeSeconds.toDouble())} --> ${formatTimeForSrt((clip.startTimeSeconds + 4).toDouble())}
<font color="#FFE600"><b>${clip.title.uppercase()}</b></font>

2
${formatTimeForSrt((clip.startTimeSeconds + 4).toDouble())} --> ${formatTimeForSrt((clip.startTimeSeconds + 8).toDouble())}
<font color="#FFFFFF">Viral Moment Highlight</font>
"""
    }

    fun writeSrtToFile(context: Context, clipId: Int, srtContent: String): File {
        val dir = File(context.cacheDir, "subtitles")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "clip_${clipId}_captions.srt")
        file.writeText(srtContent)
        return file
    }

    private fun formatTimestampToHms(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun formatTimeForSrt(seconds: Double): String {
        val totalMillis = (seconds * 1000).toLong()
        val h = totalMillis / 3600000
        val m = (totalMillis % 3600000) / 60000
        val s = (totalMillis % 60000) / 1000
        val ms = totalMillis % 1000
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
    }
}
