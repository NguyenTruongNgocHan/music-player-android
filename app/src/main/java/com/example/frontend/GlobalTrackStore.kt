package com.example.frontend

object GlobalTrackStore {
    private val allTracks: MutableList<Track> = mutableListOf()

    fun getAllTracks(): List<Track> {
        return allTracks
    }

    fun setTracks(tracks: List<Track>) {
        allTracks.clear()
        allTracks.addAll(tracks)
    }
}
