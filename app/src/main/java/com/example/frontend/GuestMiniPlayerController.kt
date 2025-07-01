package com.example.frontend

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem


object GuestMiniPlayerController {

    private var miniPlayerView: View? = null
    private var currentTrack: Track? = null
    private var currentQueue: List<Track> = emptyList()

    private var exoPlayer: ExoPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun bind(view: View) {
        miniPlayerView = view
        miniPlayerView?.visibility = View.GONE

        miniPlayerView?.setOnClickListener {
            // Show toast or expand mini player, no full PlayerActivity for guest
        }

        miniPlayerView?.findViewById<ImageButton>(R.id.playGuest)?.setOnClickListener {
            togglePlayPause()
        }
    }

    fun show(track: Track, queue: List<Track> = listOf(track)) {
        currentTrack = track
        currentQueue = queue

        miniPlayerView?.apply {
            findViewById<TextView>(R.id.trackTitleGuest).text = track.title
            findViewById<TextView>(R.id.trackArtistGuest).text = track.artist

            val img = findViewById<ImageView>(R.id.trackThumbnailGuest)
            Glide.with(context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.example)
                .centerCrop()
                .into(img)

            prepareAndPlay(track)
            updatePlayPauseIcon()
            visibility = View.VISIBLE
        }
    }

    private fun prepareAndPlay(track: Track) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(miniPlayerView!!.context).build()
        }

        coroutineScope.launch {
            try {
                val service = YouTubeService(miniPlayerView!!.context)
                val streamUrl = service.getAudioStreamUrl(miniPlayerView!!.context, track.id)
                if (streamUrl != null) {
                    exoPlayer?.apply {
                        stop()
                        clearMediaItems()
                        setMediaItem(MediaItem.fromUri(streamUrl))
                        prepare()
                        updatePlayPauseIcon()
                        play()
                    }


                    updatePlayPauseIcon()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun togglePlayPause() {
        val player = exoPlayer
        val pauseBtn = miniPlayerView?.findViewById<ImageButton>(R.id.playGuest)
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                pauseBtn?.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.play()
                pauseBtn?.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun updatePlayPauseIcon() {
        val pauseBtn = miniPlayerView?.findViewById<ImageButton>(R.id.playGuest)
        val player = exoPlayer
        pauseBtn?.setImageResource(
            if (player?.isPlaying == true)
                android.R.drawable.ic_media_pause
            else
                android.R.drawable.ic_media_play
        )
    }

    fun hide() {
        miniPlayerView?.visibility = View.GONE
        currentTrack = null
        currentQueue = emptyList()
        exoPlayer?.stop()
    }

    fun setTrack(track: Track, queue: List<Track> = listOf(track)) {
        currentTrack = track
        currentQueue = queue
    }
}
