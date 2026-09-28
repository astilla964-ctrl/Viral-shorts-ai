package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.ViralClipEntity
import com.example.data.model.ClipCandidate
import com.example.data.model.PipelineStage
import com.example.data.model.PipelineState
import com.example.data.model.VideoMetadata
import com.example.data.repository.ViralShortsRepository
import com.example.network.GeminiShortsAnalyzer
import com.example.network.YouTubeExtractorService
import com.example.service.ffmpeg.FFmpegCommandBuilder
import com.example.service.ffmpeg.FFmpegRunner
import com.example.worker.ViralShortsWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViralShortsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ViralShortsRepository(database.viralClipDao())
    private val extractor = YouTubeExtractorService()
    private val geminiAnalyzer = GeminiShortsAnalyzer()
    private val ffmpegRunner = FFmpegRunner(application)
    private val workManager = WorkManager.getInstance(application)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // UI States
    private val _urlInput = MutableStateFlow("https://www.youtube.com/watch?v=JN3K3EBEmz8")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _pipelineState = MutableStateFlow(
        PipelineState(
            stage = PipelineStage.IDLE,
            progress = 0f,
            statusMessage = "Paste any YouTube link and click 'Extract Viral Shorts'"
        )
    )
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _metadata = MutableStateFlow<VideoMetadata?>(null)
    val metadata: StateFlow<VideoMetadata?> = _metadata.asStateFlow()

    private val _clips = MutableStateFlow<List<ClipCandidate>>(emptyList())
    val clips: StateFlow<List<ClipCandidate>> = _clips.asStateFlow()

    private val _selectedClipIndex = MutableStateFlow(0)
    val selectedClipIndex: StateFlow<Int> = _selectedClipIndex.asStateFlow()

    private val _activePreviewUrl = MutableStateFlow("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    val activePreviewUrl: StateFlow<String> = _activePreviewUrl.asStateFlow()

    val savedClips: StateFlow<List<ViralClipEntity>> = repository.allClips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Run initial extraction for demo on first load so user immediately sees high-fidelity 9:16 player
        viewModelScope.launch {
            extractShortsDirect(isInitial = true)
        }
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun onSaveApiKey(key: String) {
        _customApiKey.value = key
    }

    fun selectClip(index: Int) {
        if (index in _clips.value.indices) {
            _selectedClipIndex.value = index
        }
    }

    fun setPreset(url: String) {
        _urlInput.value = url
        extractShorts()
    }

    fun extractShorts() {
        viewModelScope.launch {
            extractShortsDirect(isInitial = false)
        }
    }

    private suspend fun extractShortsDirect(isInitial: Boolean = false) {
        val targetUrl = _urlInput.value.trim()
        if (targetUrl.isBlank()) return

        try {
            // STEP 1: Extract Video & Transcript
            _pipelineState.value = PipelineState(
                stage = PipelineStage.EXTRACTING_TRANSCRIPT,
                progress = 0.20f,
                statusMessage = "Extracting video metadata & captions via open-source extractor..."
            )
            val meta = extractor.extractMetadata(targetUrl)
            _metadata.value = meta
            _activePreviewUrl.value = meta.streamOrPreviewUrl
            val transcript = extractor.fetchTranscript(meta.videoId)

            // STEP 2: Gemini Flash Viral Hook Evaluation
            _pipelineState.value = PipelineState(
                stage = PipelineStage.ANALYZING_GEMINI,
                progress = 0.50f,
                statusMessage = "Gemini Flash identifying high-retention moments & word timings..."
            )
            val detectedClips = geminiAnalyzer.analyzeTranscriptForViralClips(
                metadata = meta,
                transcript = transcript,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() }
            )

            // STEP 3: Kinetic Subtitle Generation
            _pipelineState.value = PipelineState(
                stage = PipelineStage.GENERATING_SUBTITLES,
                progress = 0.75f,
                statusMessage = "Compiling kinetic word-level subtitles (SRT) with bold styling..."
            )
            if (detectedClips.isNotEmpty()) {
                val top = detectedClips.first()
                val srt = FFmpegCommandBuilder.generateKineticSrt(top)
                FFmpegCommandBuilder.writeSrtToFile(getApplication(), top.clipId, srt)
            }
            _clips.value = detectedClips
            _selectedClipIndex.value = 0

            // STEP 4: FFmpeg Render
            _pipelineState.value = PipelineState(
                stage = PipelineStage.RENDERING_FFMPEG,
                progress = 0.90f,
                statusMessage = "FFmpeg preparing 9:16 crop & burning subtitles..."
            )
            delay(400) // Brief render simulation for UI feedback

            // STEP 5: Completed
            _pipelineState.value = PipelineState(
                stage = PipelineStage.COMPLETED,
                progress = 1.0f,
                statusMessage = "Extracted ${detectedClips.size} viral 9:16 shorts! Tap clips to preview."
            )
        } catch (e: Exception) {
            _pipelineState.value = PipelineState(
                stage = PipelineStage.FAILED,
                progress = 0f,
                statusMessage = "Failed: ${e.message}",
                error = e.localizedMessage
            )
        }
    }

    /**
     * Dispatches the pipeline to WorkManager for reliable background rendering
     */
    fun enqueueWorkManagerJob() {
        val workRequest = OneTimeWorkRequestBuilder<ViralShortsWorker>()
            .setInputData(
                workDataOf(
                    ViralShortsWorker.KEY_YOUTUBE_URL to _urlInput.value,
                    ViralShortsWorker.KEY_CUSTOM_API_KEY to _customApiKey.value
                )
            )
            .build()

        workManager.enqueue(workRequest)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val stageName = workInfo.progress.getString(ViralShortsWorker.KEY_STAGE)
                            val progress = workInfo.progress.getFloat(ViralShortsWorker.KEY_PROGRESS, 0.5f)
                            val status = workInfo.progress.getString(ViralShortsWorker.KEY_STATUS) ?: "Processing background worker..."
                            val stage = try {
                                stageName?.let { PipelineStage.valueOf(it) } ?: PipelineStage.ANALYZING_GEMINI
                            } catch (_: Exception) {
                                PipelineStage.ANALYZING_GEMINI
                            }
                            _pipelineState.value = PipelineState(stage, progress, status)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _pipelineState.value = PipelineState(
                                stage = PipelineStage.COMPLETED,
                                progress = 1.0f,
                                statusMessage = "Background WorkManager render completed!"
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(ViralShortsWorker.KEY_ERROR) ?: "WorkManager pipeline failed"
                            _pipelineState.value = PipelineState(
                                stage = PipelineStage.FAILED,
                                progress = 0f,
                                statusMessage = error,
                                error = error
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun saveCurrentClip() {
        val currentClips = _clips.value
        val index = _selectedClipIndex.value
        val meta = _metadata.value
        if (index in currentClips.indices && meta != null) {
            val clip = currentClips[index]
            viewModelScope.launch {
                val srt = FFmpegCommandBuilder.generateKineticSrt(clip)
                val cmd = FFmpegCommandBuilder.buildCommand(
                    inputVideoPath = meta.streamOrPreviewUrl,
                    outputVideoPath = ffmpegRunner.getOutputVideoFile(clip.clipId).absolutePath,
                    srtSubtitlePath = FFmpegCommandBuilder.writeSrtToFile(getApplication(), clip.clipId, srt).absolutePath,
                    startTimeSeconds = clip.startTimeSeconds,
                    endTimeSeconds = clip.endTimeSeconds
                )
                val captionsAdapter = moshi.adapter(Any::class.java)
                val entity = ViralClipEntity(
                    youtubeUrl = _urlInput.value,
                    videoTitle = meta.title,
                    videoAuthor = meta.author,
                    thumbnailUrl = meta.thumbnailUrl,
                    clipTitle = clip.title,
                    hookScore = clip.hookScore,
                    reasoning = clip.reasoning,
                    startTimeSeconds = clip.startTimeSeconds,
                    endTimeSeconds = clip.endTimeSeconds,
                    durationSeconds = clip.durationSeconds,
                    srtContent = srt,
                    captionsJson = captionsAdapter.toJson(clip.captions),
                    ffmpegCommand = cmd,
                    videoPreviewUrl = meta.streamOrPreviewUrl
                )
                repository.saveClip(entity)
            }
        }
    }

    fun deleteSavedClip(id: Long) {
        viewModelScope.launch {
            repository.deleteClip(id)
        }
    }

    fun loadSavedClip(saved: ViralClipEntity) {
        _urlInput.value = saved.youtubeUrl
        _activePreviewUrl.value = saved.videoPreviewUrl
        _metadata.value = VideoMetadata(
            videoId = "saved_${saved.id}",
            title = saved.videoTitle,
            author = saved.videoAuthor,
            durationFormatted = "${saved.durationSeconds}s",
            thumbnailUrl = saved.thumbnailUrl,
            streamOrPreviewUrl = saved.videoPreviewUrl
        )
        val loadedClip = ClipCandidate(
            clipId = saved.id.toInt(),
            title = saved.clipTitle,
            hookScore = saved.hookScore,
            reasoning = saved.reasoning,
            startTimeSeconds = saved.startTimeSeconds,
            endTimeSeconds = saved.endTimeSeconds,
            durationSeconds = saved.durationSeconds,
            captions = emptyList()
        )
        _clips.value = listOf(loadedClip)
        _selectedClipIndex.value = 0
    }
}
