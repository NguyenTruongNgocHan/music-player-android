package com.example.frontend.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.frontend.PlayerActivity
import com.example.frontend.R
import com.example.frontend.Track
import com.example.frontend.YouTubeService
import com.example.frontend.databinding.ItemSearchResultBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragmentActivity : Fragment() {
    private lateinit var searchView: SearchView
    private lateinit var resultContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.search_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchView = view.findViewById(R.id.searchView)
        resultContainer = view.findViewById(R.id.resultContainer)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) searchTracks(query.trim())
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
    }

    private fun searchTracks(query: String) {
        resultContainer.removeAllViews()
        CoroutineScope(Dispatchers.Main).launch {
            val tracks = YouTubeService(requireContext()).searchSongs(query)
            if (tracks.isEmpty()) {
                val tv = TextView(requireContext()).apply { text = "Không tìm thấy kết quả." }
                resultContainer.addView(tv)
            } else {
                for (track in tracks) {
                    val binding = ItemSearchResultBinding.inflate(layoutInflater, resultContainer, false)
                    binding.tvTitle.text = track.title
                    binding.tvArtist.text = track.artist
                    Glide.with(this@SearchFragmentActivity)
                        .load(track.thumbnailUrl)
                        .into(binding.imgThumbnail)

                    binding.root.setOnClickListener {
                        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                            putExtra("queue", ArrayList(tracks))
                            putExtra("track_id", track.id)
                        }
                        startActivity(intent)
                    }
                    resultContainer.addView(binding.root)
                }
            }
        }
    }
}
