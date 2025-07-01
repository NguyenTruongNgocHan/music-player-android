package com.example.frontend

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import android.widget.MediaController

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var startPosition: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val trackId = intent.getStringExtra("trackId")
        val title = intent.getStringExtra("title")
        val artist = intent.getStringExtra("artist")
        val thumbnail = intent.getStringExtra("thumbnail")
        val videoUrl = intent.getStringExtra("videoUrl")
        startPosition = intent.getLongExtra("position", 0L)

        videoView = findViewById(R.id.videoView)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        findViewById<ImageButton>(R.id.btnSwitchToMusic).setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("track_id", trackId)
                putExtra("position", videoView.currentPosition.toLong())
                putExtra("title", title)
                putExtra("artist", artist)
                putExtra("thumbnail", thumbnail)
            }
            startActivity(intent)
            finish()
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        if (!videoUrl.isNullOrEmpty()) {
            Log.d("VideoFetch", "Playing video from $videoUrl")
            playVideo(Uri.parse(videoUrl))
        } else {
            Log.e("VideoFetch", "No video URL provided")
            Toast.makeText(this, "No video URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(uri: Uri) {
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            videoView.seekTo(startPosition.toInt())
            videoView.start()
        }

        videoView.setOnErrorListener { _, what, extra ->
            Log.e("VideoPlayer", "Error: $what $extra")
            Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Loop Playback", "Sleep Timer") // speed not supported by VideoView
        AlertDialog.Builder(this)
            .setTitle("Playback Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleLooping()
                    1 -> showSleepTimerDialog()
                }
            }
            .show()
    }

    private var isLooping = false
    private fun toggleLooping() {
        isLooping = !isLooping
        videoView.setOnCompletionListener {
            if (isLooping) videoView.start()
        }
        Toast.makeText(this, "Looping: ${if (isLooping) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
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
                        if (!videoView.isPlaying) {
                            AlertDialog.Builder(this)
                                .setTitle("Playback Timeout")
                                .setMessage("Video didn't play. Try again.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }, 5000)
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
            val aspectRatio = Rational(videoView.width, videoView.height)
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
        videoView.setMediaController(if (isInPictureInPictureMode) null else MediaController(this).apply {
            setAnchorView(videoView)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}
