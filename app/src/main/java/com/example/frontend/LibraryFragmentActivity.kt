package com.example.frontend

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.frontend.databinding.LibraryFragmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LibraryFragmentActivity : Fragment() {
    private lateinit var binding: LibraryFragmentBinding
    private lateinit var adapter: LibraryAdapter
    private val playlists = mutableListOf<Playlist>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LibraryFragmentBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupClickListeners()
        loadPlaylistsFromFirestore()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = LibraryAdapter(playlists) { playlist ->
            if (playlist.name == "Bài hát yêu thích") {
                val email = FirebaseAuth.getInstance().currentUser?.email ?: return@LibraryAdapter
                Firebase.firestore.collection("users").document(email).get()
                    .addOnSuccessListener { doc ->
                        val liked = doc.get("likedSongs") as? List<Map<String, Any>> ?: emptyList()
                        val likedTracks = liked.map {
                            Track(
                                id = it["id"] as String,
                                title = it["title"] as String,
                                artist = it["artist"] as String,
                                thumbnailUrl = it["thumbnailUrl"] as String,
                                duration = it["duration"] as String,
                                isLiked = true
                            )
                        }

                        val intent =
                            Intent(requireContext(), LikedSongsActivity::class.java).apply {
                                putExtra("tracks", ArrayList(likedTracks))
                            }
                        startActivity(intent)
                    }
            } else {
                if (playlist.name == "Gần đây") {
                    startActivity(Intent(requireContext(), RecentlyPlayedActivity::class.java))
                } else {
                    val intent =
                        Intent(requireContext(), PlaylistDetailActivity::class.java).apply {
                            putExtra("playlist", playlist)
                        }
                    startActivity(intent)
                }
            }
        }
        binding.libraryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.libraryRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnAvatar.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        binding.btnAddPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun loadPlaylistsFromFirestore() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        Firebase.firestore.collection("users").document(email).get()
            .addOnSuccessListener { doc ->
                val data = doc.get("playlists") as? List<String> ?: emptyList()
                playlists.clear()
                playlists.add(Playlist("Bài hát yêu thích"))
                playlists.add(Playlist("Gần đây"))
                data.forEach { name ->
                    if (name != "Bài hát yêu thích" && name != "Gần đây") {
                        playlists.add(Playlist(name))
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showCreatePlaylistDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Tên playlist"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Tạo playlist mới")
            .setView(editText)
            .setPositiveButton("Tạo") { dialog, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    playlists.add(Playlist(name))
                    adapter.notifyDataSetChanged()
                    savePlaylistsToFirestore()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun savePlaylistsToFirestore() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val playlistNames = playlists.map { it.name }
        Firebase.firestore.collection("users").document(email)
            .update("playlists", playlistNames)
    }
}
