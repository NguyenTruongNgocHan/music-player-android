package com.example.frontend.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.frontend.MiniPlayerController
import com.example.frontend.PlayerActivity
import com.example.frontend.R
import com.example.frontend.Track
import com.example.frontend.YouTubeService
import kotlinx.coroutines.launch

class SearchFragmentActivity : Fragment() {
    private lateinit var searchView: SearchView
    private lateinit var resultContainer: LinearLayout
    private lateinit var youtubeService: YouTubeService
    private lateinit var miniPlayerView: View

    private var popularTracks: List<Track> = emptyList()
    private var allTracks: List<Track> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_fragment, container, false)
        searchView = view.findViewById(R.id.searchView)
        resultContainer = view.findViewById(R.id.resultContainer)
        youtubeService = YouTubeService(requireContext())

        miniPlayerView = requireActivity().findViewById(R.id.miniPlayer)
        MiniPlayerController.bind(miniPlayerView)

        loadInitialContent()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchTracks(query.orEmpty())
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                updateContent(newText.orEmpty())
                return true
            }
        })

        return view
    }

    private fun loadInitialContent() {
        lifecycleScope.launch {
            popularTracks = youtubeService.searchSongs("Popular songs")
            allTracks = youtubeService.searchSongs("All songs")
            updateContent("")
        }
    }

    private fun searchTracks(query: String) {
        resultContainer.removeAllViews()

        lifecycleScope.launch {
            val tracks = youtubeService.searchSongs(query)
            if (tracks.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "Không tìm thấy kết quả."
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                }
                resultContainer.addView(tv)
            } else {
                tracks.forEach { track ->
                    val itemView = layoutInflater.inflate(R.layout.item_queue, resultContainer, false)
                    itemView.findViewById<TextView>(R.id.trackTitle).text = track.title
                    itemView.findViewById<TextView>(R.id.trackArtist).text = track.artist
                    itemView.findViewById<TextView>(R.id.trackDuration).text = track.duration

                    Glide.with(requireContext())
                        .load(track.thumbnailUrl)
                        .placeholder(R.drawable.example)
                        .into(itemView.findViewById(R.id.trackThumbnail))

                    itemView.setOnClickListener {
                        MiniPlayerController.show(track, tracks)
                        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                            putExtra("queue", ArrayList(tracks))
                            putExtra("track_id", track.id)
                        }
                        startActivity(intent)
                    }

                    resultContainer.addView(itemView)
                }
            }
        }
    }

    private fun updateContent(query: String) {
        val context = requireContext()
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (query.isBlank()) {
            mainLayout.addView(buildSection("Popular Songs", R.drawable.ic_trending, popularTracks))
            mainLayout.addView(buildSection("All Songs", R.drawable.ic_music_note, allTracks))
        } else {
            val searchResults = (popularTracks + allTracks)
                .filter { it.title.contains(query, true) || it.artist.contains(query, true) }

            if (searchResults.isEmpty()) {
                val tv = TextView(context).apply {
                    text = "Không tìm thấy kết quả."
                    textSize = 16f
                    setPadding(16,16,16,16)
                }
                mainLayout.addView(tv)
            } else {
                mainLayout.addView(buildSection("Search Results", android.R.drawable.ic_menu_search, searchResults))
            }
        }

        resultContainer.removeAllViews()
        resultContainer.addView(mainLayout)
    }

    private fun buildSection(title: String, iconRes: Int, tracks: List<Track>): View {
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
                MiniPlayerController.show(track, tracks)
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("track_id", track.id)
                    putExtra("queue", ArrayList(tracks))
                }
                startActivity(intent)
            }

            sectionLayout.addView(itemView)
        }

        return sectionLayout
    }
}
