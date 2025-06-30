package com.example.frontend

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.view.View
import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton


class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val trackId = intent.getStringExtra("trackId") ?: return
        val startPosition = intent.getLongExtra("position", 0L)

        playerView = findViewById(R.id.playerView)
        val videoId = intent.getStringExtra("videoId") ?: return
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        findViewById<ImageButton>(R.id.btnSwitchToMusic).setOnClickListener {
            val trackId = intent.getStringExtra("trackId")
            val title = intent.getStringExtra("title")
            val artist = intent.getStringExtra("artist")
            val thumbnail = intent.getStringExtra("thumbnail")
            val position = exoPlayer?.currentPosition ?: 0L

            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("track_id", trackId)
                putExtra("position", position)
                putExtra("title", title)
                putExtra("artist", artist)
                putExtra("thumbnail", thumbnail)
                // Optional: pass full queue if needed
            }
            startActivity(intent)
            finish()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://your-flask-domain.com/extract-video")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                val jsonBody = """{"videoId": "$videoId"}"""
                conn.outputStream.use { it.write(jsonBody.toByteArray()) }

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val videoUrl = json.getString("videoUrl")

                launch(Dispatchers.Main) {
                    playVideo(videoUrl, startPosition)
                }
            } catch (e: Exception) {
                Log.e("VideoFetch", "Error fetching video", e)
            }
        }
    }

    private fun playVideo(url: String, startPosition: Long = 0L) {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        // Seek to passed position before playing
        exoPlayer?.seekTo(startPosition)
        exoPlayer?.play()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Loop Playback", "Playback Speed", "Sleep Timer")
        AlertDialog.Builder(this)
            .setTitle("Playback Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleLooping()
                    1 -> showSpeedDialog()
                    2 -> showSleepTimerDialog()
                }
            }
            .show()
    }

    private fun toggleLooping() {
        exoPlayer?.repeatMode = if (exoPlayer?.repeatMode == ExoPlayer.REPEAT_MODE_ONE)
            ExoPlayer.REPEAT_MODE_OFF else ExoPlayer.REPEAT_MODE_ONE
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "1x", "1.5x", "2x")
        val speedValues = floatArrayOf(0.5f, 1f, 1.5f, 2f)

        AlertDialog.Builder(this)
            .setTitle("Playback Speed")
            .setItems(speeds) { _, index ->
                exoPlayer?.setPlaybackSpeed(speedValues[index])
            }
            .show()
    }

    private fun showSleepTimerDialog() {
        val times = arrayOf("Off", "5 minutes", "10 minutes", "30 minutes")
        val minutes = arrayOf(0, 5, 10, 30)

        AlertDialog.Builder(this)
            .setTitle("Sleep Timer")
            .setItems(times) { _, index ->
                val delay = minutes[index] * 60 * 1000L
                if (delay > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        exoPlayer?.pause()
                    }, delay)
                }
            }
            .show()
    }

    override fun onUserLeaveHint() {
        enterPictureInPictureModeIfSupported()
        super.onUserLeaveHint()
    }

    private fun enterPictureInPictureModeIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(playerView.width, playerView.height)
            val pipBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)

            enterPictureInPictureMode(pipBuilder.build())
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // Hide controls in PiP
            playerView.useController = false
        } else {
            // Show controls again when back to full screen
            playerView.useController = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}
