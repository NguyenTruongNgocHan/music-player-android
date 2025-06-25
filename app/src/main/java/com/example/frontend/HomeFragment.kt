package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontend.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var trackAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        fetchFeaturedTracks()
    }

    private fun setupRecyclerViews() {
        trackAdapter = TrackAdapter().apply {
            onTrackClick = { selectedTrack ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("track_id", selectedTrack.id)
                    putExtra("queue", ArrayList(currentList))
                }
                startActivity(intent)
            }
        }
        binding.recyclerFeatured.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = trackAdapter
        }
    }

    private fun fetchFeaturedTracks() {
        lifecycleScope.launch {
            val query = activity?.intent?.getStringExtra("search_query")
            val youTubeService = YouTubeService(requireContext())
            val tracks = if (!query.isNullOrBlank()) {
                youTubeService.searchSongs(query)
            } else {
                youTubeService.searchSongs("popular music")
            }
            trackAdapter.submitList(tracks)
        }
    }
}
