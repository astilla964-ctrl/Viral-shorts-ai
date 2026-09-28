package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viral_clips")
data class ViralClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val youtubeUrl: String,
    val videoTitle: String,
    val videoAuthor: String,
    val thumbnailUrl: String,
    val clipTitle: String,
    val hookScore: Double,
    val reasoning: String,
    val startTimeSeconds: Long,
    val endTimeSeconds: Long,
    val durationSeconds: Long,
    val srtContent: String,
    val captionsJson: String,
    val ffmpegCommand: String,
    val videoPreviewUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)
