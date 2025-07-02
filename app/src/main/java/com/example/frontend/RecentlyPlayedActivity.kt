package com.example.frontend

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentlyPlayedActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QueueAdapter
    private val trackList = mutableListOf<Track>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_played)

        recyclerView = findViewById(R.id.recentRecyclerView)
        adapter = QueueAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { index ->
            val track = adapter.getTrackAt(index)
            if (track != null) {
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("queue", ArrayList(adapter.currentList))
                    putExtra("track_id", track.id)
                }
                startActivity(intent)
            }
        }

        loadRecentlyPlayed()
    }

    private fun loadRecentlyPlayed() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        Firebase.firestore.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                val ids = document.get("recentlyPlayed") as? List<String> ?: return@addOnSuccessListener
                val service = YouTubeService(this)
                CoroutineScope(Dispatchers.Main).launch {
                    val tracks = ids.mapNotNull { id -> service.getTrackInfoById(id) }
                    adapter.submitList(tracks)
                }
            }
    }
}