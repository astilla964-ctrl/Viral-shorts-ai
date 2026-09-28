package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WordCaption(
    @Json(name = "start") val start: Double = 0.0,
    @Json(name = "end") val end: Double = 0.0,
    @Json(name = "word") val word: String = ""
)

@JsonClass(generateAdapter = true)
data class ClipCandidate(
    @Json(name = "clip_id") val clipId: Int = 1,
    @Json(name = "title") val title: String = "",
    @Json(name = "hook_score") val hookScore: Double = 9.0,
    @Json(name = "reasoning") val reasoning: String = "",
    @Json(name = "start_time_seconds") val startTimeSeconds: Long = 0L,
    @Json(name = "end_time_seconds") val endTimeSeconds: Long = 45L,
    @Json(name = "duration_seconds") val durationSeconds: Long = 45L,
    @Json(name = "captions") val captions: List<WordCaption> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiClipsResponse(
    @Json(name = "clips") val clips: List<ClipCandidate> = emptyList()
)

data class TranscriptSegment(
    val start: Double,
    val duration: Double,
    val text: String
)

data class VideoMetadata(
    val videoId: String,
    val title: String,
    val author: String,
    val durationFormatted: String,
    val thumbnailUrl: String,
    val streamOrPreviewUrl: String
)

enum class PipelineStage(val label: String, val stepNumber: Int) {
    IDLE("Ready to Extract", 0),
    EXTRACTING_TRANSCRIPT("Extracting yt-dlp & Transcript", 1),
    ANALYZING_GEMINI("Gemini Flash Viral Hook AI", 2),
    GENERATING_SUBTITLES("Kinetic Word Subtitles (SRT)", 3),
    RENDERING_FFMPEG("FFmpeg 9:16 Crop & Burn", 4),
    COMPLETED("Viral Short Ready", 5),
    FAILED("Pipeline Error", -1)
}

data class PipelineState(
    val stage: PipelineStage = PipelineStage.IDLE,
    val progress: Float = 0f,
    val statusMessage: String = "Paste YouTube URL to extract viral clips",
    val error: String? = null
)
