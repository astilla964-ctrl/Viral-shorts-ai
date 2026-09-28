package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ClipCandidate
import com.example.network.YouTubeExtractorService
import com.example.service.ffmpeg.FFmpegCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ViralShorts AI", appName)
    }

    @Test
    fun `extract video ID from standard YouTube urls`() {
        val extractor = YouTubeExtractorService()
        val id1 = extractor.extractVideoId("https://www.youtube.com/watch?v=JN3K3EBEmz8")
        assertEquals("JN3K3EBEmz8", id1)

        val id2 = extractor.extractVideoId("https://youtu.be/UF8uR6Z6KLc")
        assertEquals("UF8uR6Z6KLc", id2)

        val id3 = extractor.extractVideoId("https://www.youtube.com/shorts/0k_P_S8K3aM")
        assertEquals("0k_P_S8K3aM", id3)
    }

    @Test
    fun `build exact requested FFmpeg command string`() {
        val cmd = FFmpegCommandBuilder.buildCommand(
            inputVideoPath = "input.mp4",
            outputVideoPath = "output.mp4",
            srtSubtitlePath = "captions.srt",
            startTimeSeconds = 120L,
            endTimeSeconds = 165L
        )

        assertTrue(cmd.contains("ffmpeg -y -ss 00:02:00 -to 00:02:45"))
        assertTrue(cmd.contains("crop=ih*(9/16):ih:(iw-ow)/2:0"))
        assertTrue(cmd.contains("subtitles='captions.srt':force_style='Fontname=Roboto,Fontsize=22,PrimaryColour=&H0000FFFF,Bold=1,Alignment=2'"))
        assertTrue(cmd.contains("-c:v libx264 -preset ultrafast -crf 23 -c:a aac -b:a 128k"))
    }

    @Test
    fun `generate kinetic SRT subtitles`() {
        val clip = ClipCandidate(
            clipId = 1,
            title = "Test Hook",
            hookScore = 9.8,
            reasoning = "Great hook",
            startTimeSeconds = 10L,
            endTimeSeconds = 50L,
            durationSeconds = 40L,
            captions = emptyList()
        )
        val srt = FFmpegCommandBuilder.generateKineticSrt(clip)
        assertNotNull(srt)
        assertTrue(srt.contains("00:00:10,000 --> 00:00:14,000"))
        assertTrue(srt.contains("<font color=\"#FFE600\"><b>TEST HOOK</b></font>"))
    }
}
