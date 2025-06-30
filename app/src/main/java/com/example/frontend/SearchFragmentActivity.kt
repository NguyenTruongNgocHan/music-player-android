package com.example.frontend.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.frontend.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragmentActivity : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var resultContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.search_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchView = view.findViewById(R.id.searchView)
        resultContainer = view.findViewById(R.id.resultContainer)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchTracks(query.trim())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = false
        })
    }

    private fun searchTracks(query: String) {
        resultContainer.removeAllViews()
        val db = Firebase.firestore

        // Sử dụng whereEqualTo, nếu muốn flexible hơn có thể dùng whereArrayContains hoặc custom index
        db.collection("tracks")
            .whereEqualTo("title", query)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val tv = TextView(requireContext()).apply {
                        text = "Không tìm thấy kết quả."
                        setTextColor(resources.getColor(android.R.color.white))
                        textSize = 16f
                        setPadding(12, 12, 12, 12)
                    }
                    resultContainer.addView(tv)
                } else {
                    for (doc in documents) {
                        val title = doc.getString("title") ?: "Unknown Title"
                        val artist = doc.getString("artist") ?: "Unknown Artist"
                        val thumbnailUrl = doc.getString("thumbnailUrl")

                        val cardView = layoutInflater.inflate(R.layout.item_search_result, resultContainer, false)
                        cardView.findViewById<TextView>(R.id.tvTitle).text = title
                        cardView.findViewById<TextView>(R.id.tvArtist).text = artist

                        val imgThumb = cardView.findViewById<ImageView>(R.id.imgThumbnail)
                        Glide.with(this)
                            .load(thumbnailUrl)
                            .placeholder(R.drawable.example)
                            .centerCrop()
                            .into(imgThumb)

                        cardView.setOnClickListener {
                            Toast.makeText(requireContext(), "Play: $title", Toast.LENGTH_SHORT).show()
                        }

                        resultContainer.addView(cardView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi khi tìm kiếm", Toast.LENGTH_SHORT).show()
            }
    }
}
