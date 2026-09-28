package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ClipCandidate
import com.example.data.model.WordCaption
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.KineticYellow
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerVerticalShortPreview(
    videoUrl: String,
    clip: ClipCandidate?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var clipDurationMs by remember { mutableLongStateOf(45000L) }
    var showControls by remember { mutableStateOf(false) }

    val startOffsetSec = clip?.startTimeSeconds ?: 0L
    val endOffsetSec = clip?.endTimeSeconds ?: (startOffsetSec + 45L)
    val clipDuration = (endOffsetSec - startOffsetSec).coerceAtLeast(1L)

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            seekTo(startOffsetSec * 1000L)
            playWhenReady = true
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Sync play range and playback ticker
    LaunchedEffect(clip, exoPlayer) {
        clipDurationMs = clipDuration * 1000L
        exoPlayer.seekTo(startOffsetSec * 1000L)
        while (true) {
            val currentPos = exoPlayer.currentPosition
            val clipElapsed = (currentPos - (startOffsetSec * 1000L)).coerceAtLeast(0L)
            playbackPositionMs = clipElapsed

            // Loop smoothly inside clip boundary
            if (currentPos >= (endOffsetSec * 1000L)) {
                exoPlayer.seekTo(startOffsetSec * 1000L)
            }
            delay(40)
        }
    }

    // 9:16 Vertical Container with Studio Glow and Kinetic Subtitle Overlay
    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, Brush.verticalGradient(listOf(NeonViolet, KineticYellow)), RoundedCornerShape(24.dp))
            .background(DarkObsidian)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            },
        contentAlignment = Alignment.Center
    ) {
        // Real ExoPlayer AndroidView cropped centered to 9:16 vertical
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient shading for readability on bottom third
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.35f),
                        0.5f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.5f),
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // Top 9:16 Badge and Audio Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = DarkObsidian.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KineticYellow.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "9:16 SHORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KineticYellow
                    )
                }
            }

            Surface(
                color = DarkObsidian.copy(alpha = 0.75f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute audio",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Kinetic Subtitles in Bottom Third (Word-by-word bold yellow highlight)
        val currentSec = (startOffsetSec.toDouble() + (playbackPositionMs / 1000.0))
        KineticSubtitleOverlay(
            captions = clip?.captions ?: emptyList(),
            currentSeconds = currentSec,
            fallbackTitle = clip?.title ?: "Auto-Generated Short",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        )

        // Playback Overlay & Controls Bar
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                // Center Big Play/Pause Toggle
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(KineticYellow, CircleShape)
                        .testTag("player_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DarkObsidian,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Scrubber Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    val progressRatio = (playbackPositionMs.toFloat() / clipDurationMs.toFloat()).coerceIn(0f, 1f)
                    Slider(
                        value = progressRatio,
                        onValueChange = { targetRatio ->
                            val seekTarget = (startOffsetSec * 1000L) + (targetRatio * clipDurationMs).toLong()
                            exoPlayer.seekTo(seekTarget)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = KineticYellow,
                            activeTrackColor = KineticYellow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(playbackPositionMs),
                            fontSize = 11.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatMs(clipDurationMs),
                            fontSize = 11.sp,
                            color = KineticYellow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KineticSubtitleOverlay(
    captions: List<WordCaption>,
    currentSeconds: Double,
    fallbackTitle: String,
    modifier: Modifier = Modifier
) {
    // Locate active word or active phrase chunk
    val activeWordIndex = captions.indexOfFirst { currentSeconds in it.start..it.end }

    // Grouping: window of 3-5 words around the active word
    val visibleWords = remember(captions, activeWordIndex) {
        if (captions.isEmpty()) emptyList()
        else if (activeWordIndex != -1) {
            val start = (activeWordIndex - 2).coerceAtLeast(0)
            val end = (activeWordIndex + 3).coerceAtMost(captions.size)
            captions.subList(start, end)
        } else {
            // Find closest upcoming or recent word
            val nextIdx = captions.indexOfFirst { it.start > currentSeconds }
            if (nextIdx != -1) {
                val start = (nextIdx - 1).coerceAtLeast(0)
                val end = (nextIdx + 3).coerceAtMost(captions.size)
                captions.subList(start, end)
            } else {
                captions.takeLast(4)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(14.dp)),
        color = Color.Black.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KineticYellow.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (visibleWords.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleWords.forEach { wordCap ->
                        val isActive = (currentSeconds in wordCap.start..wordCap.end)
                        Text(
                            text = wordCap.word.uppercase(),
                            fontSize = if (isActive) 19.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isActive) KineticYellow else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .then(
                                    if (isActive) Modifier
                                        .background(KineticYellow.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                    else Modifier
                                )
                        )
                    }
                }
            } else {
                Text(
                    text = fallbackTitle.uppercase(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = KineticYellow,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
