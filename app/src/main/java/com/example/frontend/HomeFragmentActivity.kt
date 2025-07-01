package com.example.frontend

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.frontend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class HomeFragmentActivity : Fragment() {

    private lateinit var greetingText: TextView
    private lateinit var subGreeting: TextView
    private lateinit var avatarButton: ImageButton
    private lateinit var trackContainer: LinearLayout
    private lateinit var trackContainerRanking: LinearLayout
    private var queueTopSong: List<Track> = emptyList()
    private lateinit var artistContainer: LinearLayout



    var drawerCallback: (() -> Unit)? = null

    private val greetings = listOf(
        "Chúc bạn một ngày tràn ngập giai điệu 🎵",
        "Hãy để âm nhạc làm ngày mới thêm tuyệt vời!",
        "Sẵn sàng khám phá những bài hit mới chưa? 😉",
        "Âm nhạc sẽ luôn đồng hành cùng bạn ✨",
        "Cùng chill thôi nào 😄",
        "Hy vọng bạn tìm thấy playlist yêu thích hôm nay!",
        "Thêm một ngày để thưởng thức âm nhạc tuyệt đỉnh!",
        "Hãy nhấn play và quẩy hết mình 🎧",
        "Ngày đẹp để nghe nhạc, đúng không bạn?",
        "Chào mừng trở lại, âm nhạc đang chờ bạn!"
    )

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    private lateinit var youtubeService: YouTubeService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        greetingText = view.findViewById(R.id.greetingText)
        subGreeting = view.findViewById(R.id.subGreeting)
        avatarButton = view.findViewById(R.id.btnAvatar)
        trackContainer = view.findViewById(R.id.trackContainer)
        trackContainerRanking = view.findViewById(R.id.trackContainerRanking)

        youtubeService = YouTubeService(requireContext())

        avatarButton.setOnClickListener {
            drawerCallback?.invoke()
        }

        loadUserInfo()
        loadYouTubePopularTracks()
        loadTracks()
        loadTopTrendingTracks()

        val miniPlayerView = view.findViewById<View>(R.id.miniPlayer)
        MiniPlayerController.bind(miniPlayerView)

        artistContainer = view.findViewById(R.id.artistContainer)
        loadSuggestedArtists()

    }
    private fun loadSuggestedArtists() {
        val artists = listOf(
            "Đức Phúc" to R.drawable.artist_ducphuc,
            "Tăng Duy Tân" to R.drawable.artist_tangduytan,
            "Hoàng Thùy Linh" to R.drawable.artist_hoangthuylinh,
            "Bích Phương" to R.drawable.artist_bichphuong,
            "Tiên Tiên" to R.drawable.artist_tientien
        )


        artistContainer.removeAllViews()

        for ((name, avatarUrl) in artists) {
            val itemView = layoutInflater.inflate(R.layout.item_artist, artistContainer, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvArtistName)
            val imgAvatar = itemView.findViewById<ImageView>(R.id.imgArtistAvatar)

            tvName.text = name
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.default_avt)
                .circleCrop()
                .into(imgAvatar)

            itemView.setOnClickListener {
                val intent = Intent(requireContext(), ArtistSongsActivity::class.java)
                intent.putExtra("artistName", name)
                startActivity(intent)
            }

            artistContainer.addView(itemView)
        }
    }


    private fun loadUserInfo() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = Firebase.firestore
        db.collection("users").document(email)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "User"
                val avatarUrl = document.getString("avatarUrl")
                greetingText.text = "Hello, $name 👋"
                subGreeting.text = greetings.random()

                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avt)
                        .circleCrop()
                        .into(avatarButton)
                }
            }
    }

    private fun loadYouTubePopularTracks() {
        lifecycleScope.launch {
            val tracks = youtubeService.searchSongs("Popular songs")
            trackContainer.removeAllViews() // Optional: clear existing items

            for (track in tracks) {
                val itemView = layoutInflater.inflate(R.layout.item_track, trackContainer, false)
                itemView.findViewById<TextView>(R.id.tvTitle).text = track.title
                itemView.findViewById<TextView>(R.id.tvArtist).text = track.artist

                val img = itemView.findViewById<ImageView>(R.id.imgThumbnail)
                Glide.with(this@HomeFragmentActivity)
                    .load(track.thumbnailUrl)
                    .placeholder(R.drawable.example)
                    .centerCrop()
                    .into(img)

                itemView.setOnClickListener {
                    MiniPlayerController.show(track)

                    val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra("track_id", track.id)
                        putExtra("queue", ArrayList(tracks))
                    }
                    startActivity(intent)
                }

                trackContainer.addView(itemView)
            }
        }
    }

    private fun loadTopTrendingTracks() {
        lifecycleScope.launch {
            val trendingTracks = youtubeService.searchSongs("top trending music vietnam")
            val topTracks = trendingTracks.sortedByDescending { it.viewCount }.take(5)
            queueTopSong = topTracks

            trackContainerRanking.removeAllViews()
            var rank = 1

            for (track in topTracks) {
                val itemView = layoutInflater.inflate(R.layout.item_top_song, trackContainerRanking, false)

                val tvRank = itemView.findViewById<TextView>(R.id.tvRank)
                val imgThumbnail = itemView.findViewById<ImageView>(R.id.imgTopThumbnail)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvTopTitle)
                val tvArtist = itemView.findViewById<TextView>(R.id.tvTopArtist)

                val medal = when(rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "⭐"
                }
                tvRank.text = medal
                tvTitle.text = track.title
                tvArtist.text = "${track.artist} • ${track.viewCount} views"

                Glide.with(this@HomeFragmentActivity)
                    .load(track.thumbnailUrl)
                    .placeholder(R.drawable.example)
                    .centerCrop()
                    .into(imgThumbnail)

                itemView.setOnClickListener {
                    MiniPlayerController.show(track)
                    val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra("track_id", track.id)
                        putExtra("queue", ArrayList(queueTopSong))
                    }
                    startActivity(intent)
                }

                trackContainerRanking.addView(itemView)
                rank++
            }
        }
    }


    private fun loadTracks() {
        val db = Firebase.firestore
        db.collection("tracks")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val title = doc.getString("title") ?: "Unknown Title"
                    val artist = doc.getString("artist") ?: "Unknown Artist"
                    val thumbnailUrl = doc.getString("thumbnailUrl")

                    val trackView = layoutInflater.inflate(R.layout.item_track, trackContainer, false)
                    trackView.findViewById<TextView>(R.id.tvTitle).text = title
                    trackView.findViewById<TextView>(R.id.tvArtist).text = artist

                    val img = trackView.findViewById<ImageView>(R.id.imgThumbnail)
                    Glide.with(this)
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.example)
                        .centerCrop()
                        .into(img)

                    trackView.setOnClickListener {
                        Toast.makeText(requireContext(), "Play $title", Toast.LENGTH_SHORT).show()
                    }

                    trackContainer.addView(trackView)
                }
            }
    }
}
