package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClipCandidate
import com.example.service.ffmpeg.FFmpegCommandBuilder
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.KineticYellow
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClipSelectorTabBar(
    clips: List<ClipCandidate>,
    selectedClipIndex: Int,
    onSelectClip: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (clips.isEmpty()) return

    ScrollableTabRow(
        selectedTabIndex = selectedClipIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = KineticYellow,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedClipIndex in tabPositions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedClipIndex]),
                    color = KineticYellow,
                    height = 3.dp
                )
            }
        },
        divider = {}
    ) {
        clips.forEachIndexed { index, clip ->
            val isSelected = index == selectedClipIndex
            Tab(
                selected = isSelected,
                onClick = { onSelectClip(index) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceElevated else DarkSurface)
                        .border(
                            1.dp,
                            if (isSelected) KineticYellow.copy(alpha = 0.8f) else DarkBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Clip #${clip.clipId}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 13.sp
                    )

                    // Viral Score Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (clip.hookScore >= 9.5) KineticYellow else NeonViolet,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = DarkObsidian,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${clip.hookScore}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkObsidian
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipDetailsCard(
    clip: ClipCandidate,
    videoPreviewUrl: String,
    onSaveToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSrtPreview by remember { mutableStateOf(false) }

    val srtContent = remember(clip) {
        FFmpegCommandBuilder.generateKineticSrt(clip)
    }

    val ffmpegCommand = remember(clip) {
        FFmpegCommandBuilder.buildCommand(
            inputVideoPath = videoPreviewUrl,
            outputVideoPath = "/sdcard/Movies/Shorts/viral_short_${clip.clipId}.mp4",
            srtSubtitlePath = "/data/user/0/com.example/cache/subtitles/clip_${clip.clipId}_captions.srt",
            startTimeSeconds = clip.startTimeSeconds,
            endTimeSeconds = clip.endTimeSeconds
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Clip Title & Score Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Timing: ${clip.startTimeSeconds}s - ${clip.endTimeSeconds}s (${clip.durationSeconds}s total)",
                        style = MaterialTheme.typography.bodySmall,
                        color = KineticYellow
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (clip.hookScore >= 9.5) KineticYellow else NeonViolet,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SCORE ${clip.hookScore}",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = DarkObsidian
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Reasoning Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkObsidian.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Gemini Retention Analysis:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonViolet
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = clip.reasoning,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // FFmpeg Command Inspection Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkObsidian, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = KineticYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FFmpeg Render Command",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KineticYellow
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FFmpeg Command", ffmpegCommand))
                            Toast.makeText(context, "FFmpeg command copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_ffmpeg_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy command",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = ffmpegCommand,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF80FF80) // Terminal green
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Preview Toggle Button & Drawer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showSrtPreview = !showSrtPreview },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("toggle_srt_button")
                ) {
                    Text(
                        text = if (showSrtPreview) "Hide Kinetic SRT" else "Inspect Kinetic SRT (${clip.captions.size} words)",
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Captions SRT", srtContent))
                        Toast.makeText(context, "SRT subtitles copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp).testTag("copy_srt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy SRT",
                        tint = KineticYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showSrtPreview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(DarkObsidian, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Kinetic SubRip Subtitles (Bold Yellow Highlight):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KineticYellow
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = srtContent.take(500) + if (srtContent.length > 500) "\n... [truncated]" else "",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Save to Library & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveToLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_clip_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KineticYellow,
                        contentColor = DarkObsidian
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Short to Room",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
