package com.example.frontend

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontend.databinding.ActivityLikedSongsBinding

class LikedSongsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLikedSongsBinding
    private val adapter = QueueAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikedSongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tracks = intent.getSerializableExtra("tracks") as? ArrayList<Track> ?: return

        binding.recyclerViewLikedSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLikedSongs.adapter = adapter
        adapter.submitList(tracks)

        adapter.setOnItemClickListener { position ->
            val selected = adapter.getTrackAt(position)
            if (selected != null) {
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("queue", ArrayList(tracks))
                    putExtra("track_id", selected.id)
                }
                startActivity(intent)
            }
        }
    }
}
