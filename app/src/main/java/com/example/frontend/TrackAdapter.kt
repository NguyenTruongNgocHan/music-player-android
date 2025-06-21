package com.example.frontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TrackAdapter : ListAdapter<Track, TrackAdapter.TrackViewHolder>(DiffCallback()) {

    var onTrackClick: ((Track) -> Unit)? = null

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val artist: TextView = itemView.findViewById(R.id.tvArtist)
        val duration: TextView = itemView.findViewById(R.id.tvDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)

        holder.title.text = track.title
        holder.artist.text = track.artist
        holder.duration.text = track.duration

        Glide.with(holder.itemView)
            .load(track.thumbnailUrl)
            .placeholder(R.drawable.example)
            .centerCrop()
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            onTrackClick?.invoke(track)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem == newItem
    }
}