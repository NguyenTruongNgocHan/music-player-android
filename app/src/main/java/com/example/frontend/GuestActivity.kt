package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class GuestActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var avatarButton: ImageButton
    private lateinit var contentFrame: FrameLayout

    private lateinit var youtubeService: YouTubeService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guest_activity)

        searchView = findViewById(R.id.searchView)
        avatarButton = findViewById(R.id.btnGuestAvatar)
        contentFrame = findViewById(R.id.guestMainContent)

        // Bind miniPlayer
        val miniPlayerView = findViewById<FrameLayout>(R.id.miniPlayer)
        layoutInflater.inflate(R.layout.view_mini_player, miniPlayerView, true)
        MiniPlayerController.bind(miniPlayerView)

        // Handle avatar click
        avatarButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Bạn đang ở chế độ khách")
                .setMessage("Đăng nhập để có trải nghiệm tốt hơn.")
                .setPositiveButton("Đăng nhập") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Đăng ký") { _, _ ->
                    startActivity(Intent(this, SignUpActivity::class.java))
                }
                .setNeutralButton("Để sau", null)
                .show()
        }

        // Search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@GuestActivity, "Tìm: $query", Toast.LENGTH_SHORT).show()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        youtubeService = YouTubeService(this)
        loadGuestContent()
    }

    private fun loadGuestContent() {
        val context = this
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        lifecycleScope.launch {
            // Popular songs
            val popularTitle = TextView(context).apply {
                text = "Popular Songs"
                textSize = 18f
                setPadding(16,16,16,8)
            }
            mainLayout.addView(popularTitle)

            val popularTracks = youtubeService.searchSongs("Popular songs")
            val popularContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            for (track in popularTracks) {
                val itemView = layoutInflater.inflate(R.layout.item_queue, popularContainer, false)
                itemView.findViewById<TextView>(R.id.trackTitle).text = track.title
                itemView.findViewById<TextView>(R.id.trackArtist).text = track.artist
                itemView.findViewById<TextView>(R.id.trackDuration).text = track.duration

                Glide.with(context)
                    .load(track.thumbnailUrl)
                    .placeholder(R.drawable.example)
                    .centerCrop()
                    .into(itemView.findViewById(R.id.trackThumbnail))

                itemView.setOnClickListener {
                    MiniPlayerController.show(track, popularTracks)
                }

                popularContainer.addView(itemView)
            }
            mainLayout.addView(popularContainer)

            // Firestore
            val dbTitle = TextView(context).apply {
                text = "All Songs"
                textSize = 18f
                setPadding(16,24,16,8)
            }
            mainLayout.addView(dbTitle)

            val dbContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val db = Firebase.firestore
            db.collection("tracks").limit(30).get()
                .addOnSuccessListener { documents ->
                    val fireTracks = mutableListOf<Track>()
                    for (doc in documents) {
                        val title = doc.getString("title") ?: "Unknown"
                        val artist = doc.getString("artist") ?: "Unknown"
                        val thumbnailUrl = doc.getString("thumbnailUrl")
                        val track = Track(doc.id, title, artist, thumbnailUrl ?: "", "0:00")
                        fireTracks.add(track)

                        val itemView = layoutInflater.inflate(R.layout.item_queue, dbContainer, false)
                        itemView.findViewById<TextView>(R.id.trackTitle).text = title
                        itemView.findViewById<TextView>(R.id.trackArtist).text = artist
                        itemView.findViewById<TextView>(R.id.trackDuration).text = "0:00"

                        Glide.with(context)
                            .load(thumbnailUrl)
                            .placeholder(R.drawable.example)
                            .centerCrop()
                            .into(itemView.findViewById(R.id.trackThumbnail))

                        itemView.setOnClickListener {
                            MiniPlayerController.show(track, fireTracks)
                        }

                        dbContainer.addView(itemView)
                    }

                    mainLayout.addView(dbContainer)

                    contentFrame.removeAllViews()
                    val scrollView = ScrollView(context).apply { addView(mainLayout) }
                    contentFrame.addView(scrollView)
                }
        }
    }
}
