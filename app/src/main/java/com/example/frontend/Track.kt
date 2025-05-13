package com.example.frontend

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String, // formatted as "3:45"
    val thumbnailUrl: String,
    var isPlaying: Boolean = false
) {
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
