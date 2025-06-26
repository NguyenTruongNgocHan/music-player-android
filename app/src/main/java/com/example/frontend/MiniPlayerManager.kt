package com.example.frontend

object MiniPlayerManager {
    private var currentTrack: Track? = null

    fun setTrack(track: Track) {
        currentTrack = track
    }

    fun getCurrentTrack(): Track? = currentTrack
}