package com.example.frontend

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ItemQueueBinding

class QueueAdapter : ListAdapter<Track, QueueAdapter.ViewHolder>(TrackDiffCallback()) {
    var currentlyPlayingIndex = -1
    private var itemClickListener: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        itemClickListener = listener
    }

    inner class ViewHolder(val binding: ItemQueueBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener?.invoke(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQueueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = getItem(position)
        holder.binding.apply {
            trackTitle.text = track.title
            trackArtist.text = track.artist
            trackDuration.text = track.duration

            Glide.with(root.context)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.ic_music_note)
                .into(trackThumbnail)

            // Highlight currently playing track
            root.setBackgroundColor(
                if (position == currentlyPlayingIndex) {
                    ContextCompat.getColor(root.context, R.color.colorPrimaryLight)
                } else {
                    Color.TRANSPARENT
                }
            )
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
}