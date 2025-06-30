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

        youtubeService = YouTubeService(requireContext())

        avatarButton.setOnClickListener {
            drawerCallback?.invoke()
        }

        loadUserInfo()
        loadYouTubePopularTracks()
        loadTracks()

        val miniPlayerView = view.findViewById<View>(R.id.miniPlayer)
        MiniPlayerController.bind(miniPlayerView)
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
