package com.example.frontend

import java.io.Serializable

data class Playlist(
    val name: String,
    val coverUrl: String? = null,
    val tracks: List<Track> = emptyList()
) : Serializable
