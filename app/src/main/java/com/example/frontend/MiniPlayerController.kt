package com.example.frontend

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

object MiniPlayerController {

    private var miniPlayerView: View? = null
    private var currentTrack: Track? = null
    private var currentQueue: List<Track> = emptyList()

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
                }
                context.startActivity(intent)
            }
        }
    }

    fun show(track: Track, queue: List<Track> = listOf(track)) {
        currentTrack = track
        currentQueue = queue

        miniPlayerView?.apply {
            findViewById<TextView>(R.id.trackTitle).text = track.title
            findViewById<TextView>(R.id.trackArtist).text = track.artist
            findViewById<TextView>(R.id.trackDuration).text = track.duration

            val img = findViewById<ImageView>(R.id.trackThumbnail)
            Glide.with(context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.example)
                .centerCrop()
                .into(img)

            visibility = View.VISIBLE
        }
    }

    fun hide() {
        miniPlayerView?.visibility = View.GONE
        currentTrack = null
        currentQueue = emptyList()
    }
}
