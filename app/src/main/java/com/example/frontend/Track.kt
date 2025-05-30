package com.example.frontend

import java.io.Serializable

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnailUrl: String,
    var isPlaying: Boolean = false
) : Serializable {
    companion object {
        // For loading states
        fun createLoadingPlaceholder() = Track(
            id = "loading",
            title = "Loading...",
            artist = "Please wait",
            duration = "0:00",
            thumbnailUrl = ""
        )
    }
}
