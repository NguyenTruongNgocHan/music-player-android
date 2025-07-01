package com.example.frontend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var player: ExoPlayer
    private var currentTrack: Track? = null

    companion object {
        var sharedPlayer: ExoPlayer? = null
    }

    override fun onCreate() {
        super.onCreate()

        // Reuse player if already created
        player = sharedPlayer ?: ExoPlayer.Builder(this).build().also {
            sharedPlayer = it
        }

        // Setup MediaSession with optional callbacks (can be extended)
        mediaSession = MediaSession.Builder(this, player)
            .setId("PlayerServiceSession")
            .setCallback(object : MediaSession.Callback {})
            .build()

        // Required for Android 8+ to ensure notification channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "media_playback",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.action?.let { action ->
            when (action) {
                "PLAY" -> player.play()
                "PAUSE" -> player.pause()
                "SET_TRACK" -> {
                    val track = intent.getSerializableExtra("track") as? Track
                    val position = intent.getLongExtra("position", 0L)
                    val streamUrl = intent.getStringExtra("streamUrl")

                    if (track != null && !streamUrl.isNullOrEmpty()) {
                        currentTrack = track

                        val mediaItem = MediaItem.Builder()
                            .setUri(streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setArtworkUri(Uri.parse(track.thumbnailUrl))
                                    .build()
                            )
                            .build()

                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.seekTo(position)
                        player.play()
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
