package com.example.frontend

import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

object MiniPlayerController {

    private var miniPlayerView: View? = null
    private var currentTrack: Track? = null
    private var currentQueue: List<Track> = emptyList()
    private var currentIndex: Int = 0


    fun bind(view: View) {
        miniPlayerView = view
        miniPlayerView?.visibility = View.GONE

        miniPlayerView?.setOnClickListener {
            val context = view.context
            val track = currentTrack
            if (track != null) {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra("track_id", track.id)
                    putExtra("queue", ArrayList(currentQueue))
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(intent)
            }
        }

        miniPlayerView?.findViewById<ImageButton>(R.id.pause)?.setOnClickListener {
            val player = PlayerService.sharedPlayer
            val pauseBtn = it as ImageButton

            if (player?.isPlaying == true) {
                player.pause()
                pauseBtn.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player?.play()
                pauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    fun show(track: Track, queue: List<Track> = listOf(track)) {
        currentTrack = track
        currentQueue = queue

        miniPlayerView?.apply {
            findViewById<TextView>(R.id.trackTitle).text = track.title
            findViewById<TextView>(R.id.trackArtist).text = track.artist

            val img = findViewById<ImageView>(R.id.trackThumbnail)
            Glide.with(context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.example)
                .centerCrop()
                .into(img)

            updatePlayPauseIcon()
            visibility = View.VISIBLE
        }
    }

    private fun updatePlayPauseIcon() {
        val pauseBtn = miniPlayerView?.findViewById<ImageButton>(R.id.pause)
        val player = PlayerService.sharedPlayer
        pauseBtn?.setImageResource(
            if (player?.isPlaying == true)
                android.R.drawable.ic_media_pause
            else
                android.R.drawable.ic_media_play
        )
    }

    fun updatePlayState() {
        updatePlayPauseIcon()
    }

    fun hide() {
        miniPlayerView?.visibility = View.GONE
        currentTrack = null
        currentQueue = emptyList()
    }

    fun setTrack(track: Track, queue: List<Track> = listOf(track)) {
        currentTrack = track
        currentQueue = queue
    }

    fun setQueue(queue: List<Track>, startIndex: Int = 0) {
        this.currentQueue = queue
        this.currentIndex = startIndex
        if (queue.isNotEmpty()) {
            setTrack(queue[startIndex], queue)
        }
    }


}