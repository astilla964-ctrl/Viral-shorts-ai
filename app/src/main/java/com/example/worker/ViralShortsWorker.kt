package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.ViralClipEntity
import com.example.data.model.PipelineStage
import com.example.network.GeminiShortsAnalyzer
import com.example.network.YouTubeExtractorService
import com.example.service.ffmpeg.FFmpegCommandBuilder
import com.example.service.ffmpeg.FFmpegRunner
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ViralShortsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_YOUTUBE_URL = "key_youtube_url"
        const val KEY_CUSTOM_API_KEY = "key_custom_api_key"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_CLIPS_COUNT = "clips_count"
        const val KEY_TOP_TITLE = "top_title"
    }

    private val extractor = YouTubeExtractorService()
    private val geminiAnalyzer = GeminiShortsAnalyzer()
    private val ffmpegRunner = FFmpegRunner(context)
    private val database = AppDatabase.getInstance(context)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_YOUTUBE_URL) ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val customApiKey = inputData.getString(KEY_CUSTOM_API_KEY)

        try {
            // STEP 1: Extract Video Metadata and Transcript (yt-dlp / NewPipeExtractor)
            setProgress(
                workDataOf(
                    KEY_STAGE to PipelineStage.EXTRACTING_TRANSCRIPT.name,
                    KEY_PROGRESS to 0.20f,
                    KEY_STATUS to "Extracting YouTube audio stream & transcript..."
                )
            )

            val metadata = extractor.extractMetadata(url)
            val transcript = extractor.fetchTranscript(metadata.videoId)

            // STEP 2: Query Gemini Flash API for Viral Timestamps & Retention Scoring
            setProgress(
                workDataOf(
                    KEY_STAGE to PipelineStage.ANALYZING_GEMINI.name,
                    KEY_PROGRESS to 0.45f,
                    KEY_STATUS to "Gemini Flash evaluating 0-3s hooks & narrative arc..."
                )
            )

            val clips = geminiAnalyzer.analyzeTranscriptForViralClips(metadata, transcript, customApiKey)
            if (clips.isEmpty()) {
                return Result.failure(
                    workDataOf(KEY_ERROR to "No viral clips could be identified from this transcript")
                )
            }

            // STEP 3: Generate Kinetic Word-by-Word SRT Subtitle Files
            setProgress(
                workDataOf(
                    KEY_STAGE to PipelineStage.GENERATING_SUBTITLES.name,
                    KEY_PROGRESS to 0.70f,
                    KEY_STATUS to "Generating kinetic yellow/white word subtitles (SRT)..."
                )
            )

            val topClip = clips.first()
            val srtContent = FFmpegCommandBuilder.generateKineticSrt(topClip)
            val srtFile = FFmpegCommandBuilder.writeSrtToFile(context, topClip.clipId, srtContent)

            // STEP 4: Build FFmpeg Command and Render 9:16 Cropped Video
            setProgress(
                workDataOf(
                    KEY_STAGE to PipelineStage.RENDERING_FFMPEG.name,
                    KEY_PROGRESS to 0.88f,
                    KEY_STATUS to "Executing FFmpeg: 9:16 centered crop & burning subtitles..."
                )
            )

            val outputFile = ffmpegRunner.getOutputVideoFile(topClip.clipId)
            val ffmpegCommand = FFmpegCommandBuilder.buildCommand(
                inputVideoPath = metadata.streamOrPreviewUrl,
                outputVideoPath = outputFile.absolutePath,
                srtSubtitlePath = srtFile.absolutePath,
                startTimeSeconds = topClip.startTimeSeconds,
                endTimeSeconds = topClip.endTimeSeconds
            )

            ffmpegRunner.executeRender(
                command = ffmpegCommand,
                outputFilePath = outputFile.absolutePath,
                onProgress = { pct ->
                    // Dynamic progress
                }
            )

            // STEP 5: Save Extracted Clips to Local Room Database
            val captionsAdapter = moshi.adapter(Any::class.java)
            for (clip in clips) {
                val clipSrt = FFmpegCommandBuilder.generateKineticSrt(clip)
                val clipCommand = FFmpegCommandBuilder.buildCommand(
                    inputVideoPath = metadata.streamOrPreviewUrl,
                    outputVideoPath = ffmpegRunner.getOutputVideoFile(clip.clipId).absolutePath,
                    srtSubtitlePath = FFmpegCommandBuilder.writeSrtToFile(context, clip.clipId, clipSrt).absolutePath,
                    startTimeSeconds = clip.startTimeSeconds,
                    endTimeSeconds = clip.endTimeSeconds
                )

                val entity = ViralClipEntity(
                    youtubeUrl = url,
                    videoTitle = metadata.title,
                    videoAuthor = metadata.author,
                    thumbnailUrl = metadata.thumbnailUrl,
                    clipTitle = clip.title,
                    hookScore = clip.hookScore,
                    reasoning = clip.reasoning,
                    startTimeSeconds = clip.startTimeSeconds,
                    endTimeSeconds = clip.endTimeSeconds,
                    durationSeconds = clip.durationSeconds,
                    srtContent = clipSrt,
                    captionsJson = captionsAdapter.toJson(clip.captions),
                    ffmpegCommand = clipCommand,
                    videoPreviewUrl = metadata.streamOrPreviewUrl
                )
                database.viralClipDao().insertClip(entity)
            }

            setProgress(
                workDataOf(
                    KEY_STAGE to PipelineStage.COMPLETED.name,
                    KEY_PROGRESS to 1.0f,
                    KEY_STATUS to "Viral Shorts extracted and saved successfully!"
                )
            )

            return Result.success(
                workDataOf(
                    KEY_CLIPS_COUNT to clips.size,
                    KEY_TOP_TITLE to topClip.title
                )
            )
        } catch (e: Exception) {
            return Result.failure(
                workDataOf(KEY_ERROR to (e.localizedMessage ?: "Worker processing error"))
            )
        }
    }
}
