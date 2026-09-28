package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PipelineStage
import com.example.data.model.PipelineState
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
fun PipelineProgressCard(
    pipelineState: PipelineState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = pipelineState.progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status message and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isProcessing = pipelineState.stage != PipelineStage.IDLE &&
                            pipelineState.stage != PipelineStage.COMPLETED &&
                            pipelineState.stage != PipelineStage.FAILED

                    if (isProcessing) {
                        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(pulse)
                                .background(KineticYellow, CircleShape)
                        )
                    } else if (pipelineState.stage == PipelineStage.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(EmeraldGreen, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(TextMuted, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = pipelineState.stage.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pipelineState.stage == PipelineStage.COMPLETED) EmeraldGreen else KineticYellow
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth progress indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = KineticYellow,
                trackColor = DarkObsidian
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pipelineState.statusMessage,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Distinct Stepper Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PipelineStepItem(
                    stepNumber = 1,
                    title = "Extract",
                    icon = Icons.Default.Download,
                    isCurrent = pipelineState.stage == PipelineStage.EXTRACTING_TRANSCRIPT,
                    isCompleted = pipelineState.stage.stepNumber > 1
                )
                PipelineStepDivider(isCompleted = pipelineState.stage.stepNumber > 1)
                PipelineStepItem(
                    stepNumber = 2,
                    title = "Gemini AI",
                    icon = Icons.Default.AutoAwesome,
                    isCurrent = pipelineState.stage == PipelineStage.ANALYZING_GEMINI,
                    isCompleted = pipelineState.stage.stepNumber > 2
                )
                PipelineStepDivider(isCompleted = pipelineState.stage.stepNumber > 2)
                PipelineStepItem(
                    stepNumber = 3,
                    title = "Subtitles",
                    icon = Icons.Default.Subtitles,
                    isCurrent = pipelineState.stage == PipelineStage.GENERATING_SUBTITLES,
                    isCompleted = pipelineState.stage.stepNumber > 3
                )
                PipelineStepDivider(isCompleted = pipelineState.stage.stepNumber > 3)
                PipelineStepItem(
                    stepNumber = 4,
                    title = "FFmpeg",
                    icon = Icons.Default.Crop,
                    isCurrent = pipelineState.stage == PipelineStage.RENDERING_FFMPEG,
                    isCompleted = pipelineState.stage == PipelineStage.COMPLETED
                )
            }
        }
    }
}

@Composable
private fun PipelineStepItem(
    stepNumber: Int,
    title: String,
    icon: ImageVector,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    val circleColor by animateColorAsState(
        targetValue = when {
            isCompleted -> EmeraldGreen
            isCurrent -> KineticYellow
            else -> DarkSurface
        },
        label = "circle_color"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isCompleted -> DarkObsidian
            isCurrent -> DarkObsidian
            else -> TextMuted
        },
        label = "icon_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(circleColor, CircleShape)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) Color.White else DarkBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) KineticYellow else if (isCompleted) TextPrimary else TextMuted
        )
    }
}

@Composable
private fun PipelineStepDivider(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(2.dp)
            .background(if (isCompleted) EmeraldGreen else DarkBorder)
    )
}
