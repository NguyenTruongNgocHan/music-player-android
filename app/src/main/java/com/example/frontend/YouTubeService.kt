package com.example.frontend

import android.content.Context
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeService(private val context: Context) {
    private val youtube: YouTube by lazy {
        YouTube.Builder(
            NetHttpTransport(),
            JacksonFactory(),
            HttpRequestInitializer { /* No credentials */ }
        ).setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val search = youtube.search().list("id,snippet")
                .setQ(query)
                .setType("video")
                .setMaxResults(15)
                .setKey(context.getString(R.string.youtube_api_key))

            search.execute().items.map { item ->
                Song(
                    id = item.id.videoId,
                    title = item.snippet.title,
                    artist = item.snippet.channelTitle,
                    thumbnailUrl = item.snippet.thumbnails.default.url
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAudioStreamUrl(videoId: String): String? {
        // In practice you'd need to extract actual audio stream URL
        // This often requires additional libraries like youtube-extractor
        return "https://example.com/stream/$videoId" // Placeholder
    }
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String
)