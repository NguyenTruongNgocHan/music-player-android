package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var guestMiniPlayerView: View
    private lateinit var youtubeService: YouTubeService

    private var popularTracks: List<Track> = emptyList()
    private var allTracks: List<Track> = emptyList()
    private var fireTracks: List<Track> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guest_activity)

        searchView = findViewById(R.id.searchView)
        avatarButton = findViewById(R.id.btnGuestAvatar)
        contentFrame = findViewById(R.id.guestMainContent)
        guestMiniPlayerView = findViewById(R.id.miniPlayerGuest)
        GuestMiniPlayerController.bind(guestMiniPlayerView)

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

        youtubeService = YouTubeService(this)
        loadInitialContent()

        // Khi text search thay đổi
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                updateContent(newText.orEmpty())
                return true
            }
        })
    }

    private fun loadInitialContent() {
        val db = Firebase.firestore
        lifecycleScope.launch {
            // Load popular & all từ YouTube
            popularTracks = youtubeService.searchSongs("Popular songs")
            allTracks = youtubeService.searchSongs("All songs")

            // Load Firestore
            db.collection("tracks").limit(30).get()
                .addOnSuccessListener { documents ->
                    fireTracks = documents.map { doc ->
                        Track(
                            doc.id,
                            doc.getString("title") ?: "Unknown",
                            doc.getString("artist") ?: "Unknown",
                            doc.getString("thumbnailUrl") ?: "",
                            "0:00"
                        )
                    }
                    updateContent("")
                }
        }
    }

    private fun updateContent(query: String) {
        val context = this
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (query.isBlank()) {
            // Hiển thị popular & all
            mainLayout.addView(buildSection(" Popular Songs", R.drawable.ic_trending, popularTracks))
            mainLayout.addView(buildSection(" All Songs", R.drawable.ic_music_note, allTracks))
        } else {
            // Tìm kiếm
            val searchResults = (popularTracks + allTracks + fireTracks)
                .filter { it.title.contains(query, true) || it.artist.contains(query, true) }

            mainLayout.addView(buildSection(" Search Results", android.R.drawable.ic_menu_search, searchResults))
        }

        contentFrame.removeAllViews()
        val scrollView = ScrollView(context).apply { addView(mainLayout) }
        contentFrame.addView(scrollView)
    }

    private fun buildSection(title: String, iconRes: Int, tracks: List<Track>): View {
        val context = this
        val sectionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setPadding(16,16,16,8)
            setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
            compoundDrawablePadding = 8
        }
        sectionLayout.addView(titleView)

        tracks.forEach { track ->
            val itemView = layoutInflater.inflate(R.layout.item_queue, sectionLayout, false)
            itemView.findViewById<TextView>(R.id.trackTitle).text = track.title
            itemView.findViewById<TextView>(R.id.trackArtist).text = track.artist
            itemView.findViewById<TextView>(R.id.trackDuration).text = track.duration

            Glide.with(context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.example)
                .centerCrop()
                .into(itemView.findViewById(R.id.trackThumbnail))

            itemView.setOnClickListener {
                GuestMiniPlayerController.show(track, tracks)
            }

            sectionLayout.addView(itemView)
        }

        return sectionLayout
    }
}
