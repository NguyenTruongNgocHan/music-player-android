package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ArtistSongsActivity : AppCompatActivity() {

    private lateinit var tvArtistTitle: TextView
    private lateinit var tvSongCount: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnPlayAll: ImageView
    private lateinit var songContainer: LinearLayout

    private lateinit var youtubeService: YouTubeService
    private var artistQueue: List<Track> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artist_songs)

        tvArtistTitle = findViewById(R.id.tvArtistTitle)
        tvSongCount = findViewById(R.id.tvSongCount)
        btnBack = findViewById(R.id.btnBack)
        btnPlayAll = findViewById(R.id.btnPlayAll)
        songContainer = findViewById(R.id.songContainer)

        youtubeService = YouTubeService(this)

        val artistName = intent.getStringExtra("artistName") ?: "Unknown Artist"
        tvArtistTitle.text = artistName

        btnBack.setOnClickListener { finish() }

        btnPlayAll.setOnClickListener {
            if (artistQueue.isNotEmpty()) {
                Toast.makeText(this, "Playing all songs of $artistName", Toast.LENGTH_SHORT).show()
                MiniPlayerController.show(artistQueue.first())
                MiniPlayerController.setQueue(artistQueue)

                // mở PlayerActivity với queue
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("track_id", artistQueue.first().id)
                    putExtra("queue", ArrayList(artistQueue))
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Không tìm thấy bài hát nào.", Toast.LENGTH_SHORT).show()
            }
        }

        loadSongsForArtist(artistName)
    }

    private fun loadSongsForArtist(artistName: String) {
        lifecycleScope.launch {
            artistQueue = youtubeService.createPlaylistByArtist(artistName)
            tvSongCount.text = "${artistQueue.size} bài hát"

            songContainer.removeAllViews()
            for (song in artistQueue) {
                val itemView = layoutInflater.inflate(R.layout.item_song, songContainer, false)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvSongTitle)
                val tvArtist = itemView.findViewById<TextView>(R.id.tvSongArtist)
                val imgThumb = itemView.findViewById<ImageView>(R.id.imgSongThumbnail)

                tvTitle.text = song.title
                tvArtist.text = "${song.artist} • ${song.duration}"

                Glide.with(this@ArtistSongsActivity)
                    .load(song.thumbnailUrl)
                    .placeholder(R.drawable.example)
                    .centerCrop()
                    .into(imgThumb)

                itemView.setOnClickListener {
                    Toast.makeText(this@ArtistSongsActivity, "Playing ${song.title}", Toast.LENGTH_SHORT).show()
                    MiniPlayerController.show(song)
                    MiniPlayerController.setQueue(artistQueue)

                    val intent = Intent(this@ArtistSongsActivity, PlayerActivity::class.java).apply {
                        putExtra("track_id", song.id)
                        putExtra("queue", ArrayList(artistQueue))
                    }
                    startActivity(intent)
                }

                songContainer.addView(itemView)
            }
        }
    }
}
