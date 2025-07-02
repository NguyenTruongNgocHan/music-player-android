package com.example.frontend.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.frontend.MiniPlayerController
import com.example.frontend.R
import com.example.frontend.Track

class LibraryFragmentActivity : Fragment() {

    private lateinit var libraryContainer: LinearLayout
    private lateinit var miniPlayerView: View

    private var likedSongs: List<Track> = emptyList()
    private var recentSongs: List<Track> = emptyList()
    private var playlists: List<String> = emptyList() // hoặc List<Playlist>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.library_fragment, container, false)
        libraryContainer = view.findViewById(R.id.libraryContainer)

        miniPlayerView = requireActivity().findViewById(R.id.miniPlayer)
        MiniPlayerController.bind(miniPlayerView)

        loadLibraryContent()

        return view
    }

    private fun loadLibraryContent() {
        // giả lập dữ liệu
        likedSongs = listOf(
            Track("1", "Song A", "Artist A", "3:30", "https://...", isLiked = true),
            Track("2", "Song B", "Artist B", "4:00", "https://...", isLiked = true)
        )
        recentSongs = listOf(
            Track("3", "Recent X", "Artist X", "3:00", "https://..."),
            Track("4", "Recent Y", "Artist Y", "2:45", "https://...")
        )
        playlists = listOf("EDM", "Nhạc Việt", "Ballad buồn")

        libraryContainer.removeAllViews()
        libraryContainer.addView(buildSection("Bài hát đã thích", likedSongs))
        libraryContainer.addView(buildSection("Nghe gần đây", recentSongs))
        libraryContainer.addView(buildPlaylistSection())
    }

    private fun buildSection(title: String, tracks: List<Track>): View {
        val context = requireContext()
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
        }
        sectionLayout.addView(titleView)

        tracks.forEach { track ->
            val itemView = layoutInflater.inflate(R.layout.item_queue, sectionLayout, false)
            itemView.findViewById<TextView>(R.id.trackTitle).text = track.title
            itemView.findViewById<TextView>(R.id.trackArtist).text = track.artist
            Glide.with(context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.default_avt)
                .centerCrop()
                .into(itemView.findViewById(R.id.trackThumbnail))

            itemView.setOnClickListener {
                // handle play
            }

            sectionLayout.addView(itemView)
        }

        return sectionLayout
    }

    private fun buildPlaylistSection(): View {
        val context = requireContext()
        val sectionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(context).apply {
            text = "Playlists"
            textSize = 18f
            setPadding(16,16,16,8)
        }
        sectionLayout.addView(titleView)

        playlists.forEach { name ->
            val item = TextView(context).apply {
                text = name
                textSize = 16f
                setPadding(32,8,16,8)
            }
            item.setOnClickListener {
                // mở playlist detail
            }
            sectionLayout.addView(item)
        }

        return sectionLayout
    }
}
