package com.example.frontend

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val image: Int,
    val audio: String,
    var isLiked: Boolean = false
)
