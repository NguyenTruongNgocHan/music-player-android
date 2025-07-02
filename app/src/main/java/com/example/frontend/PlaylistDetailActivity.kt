package com.example.frontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontend.databinding.ActivityPlaylistDetailBinding

class PlaylistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var adapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlist = intent.getSerializableExtra("playlist") as? Playlist
        binding.tvPlaylistName.text = playlist?.name ?: "Playlist"

        adapter = QueueAdapter()
        binding.recyclerViewQueue.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewQueue.adapter = adapter

        adapter.submitList(playlist?.tracks ?: emptyList())

        binding.btnBack.setOnClickListener { finish() }
    }
}
