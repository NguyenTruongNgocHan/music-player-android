package com.example.frontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChannelAdapter(
    private var channels: List<Channel>,
    private val onClick: ((Channel) -> Unit)? = null
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_circle, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvName.text = channel.name

        Glide.with(holder.itemView)
            .load(channel.avatarUrl)
            .placeholder(R.drawable.default_avt)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener {
            onClick?.invoke(channel)
        }
    }

    override fun getItemCount(): Int = channels.size

    fun updateData(newChannels: List<Channel>) {
        channels = newChannels
        notifyDataSetChanged()
    }
}