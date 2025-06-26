package com.example.frontend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var player: ExoPlayer
    private var currentTrack: Track? = null
    private var isServiceInForeground = false // Track foreground state ourselves

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_playback_channel"

        var sharedPlayer: ExoPlayer? = null
    }

    override fun onCreate() {
        super.onCreate()

        if (sharedPlayer == null) {
            sharedPlayer = ExoPlayer.Builder(this).build()
        }

        mediaSession = MediaSession.Builder(this, sharedPlayer!!)
            .setId("PlayerServiceSession")
            .build()
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // 🛡️ Only initialize once
        if (!::player.isInitialized) {
            player = ExoPlayer.Builder(this).build()
        }

        if (!::mediaSession.isInitialized) {
            mediaSession = MediaSession.Builder(this, player)
                .setId("PlayerServiceSession")
                .build()
        }

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
                        player.setMediaItem(MediaItem.fromUri(streamUrl))
                        player.prepare()
                        player.seekTo(position)
                        player.play()
                    }
                }
            }
        }

        if (!isServiceInForeground) {
            startForeground(NOTIFICATION_ID, createNotification())
            isServiceInForeground = true
        } else {
            updateNotification()
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return buildNotification()
    }

    private fun buildNotification(): Notification {
        val track = currentTrack
        val openIntent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track?.title ?: "Now Playing")
            .setContentText(track?.artist ?: "Enjoy your music")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
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