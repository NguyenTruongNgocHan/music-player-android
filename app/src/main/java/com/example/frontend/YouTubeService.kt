package com.example.frontend

import android.content.Context
import android.util.Log
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

    suspend fun searchSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val search = youtube.search().list("id,snippet").apply {
                q = query
                type = "video"
                videoCategoryId = "10" // Music category
                maxResults = 10
                key = context.getString(R.string.youtube_api_key)
            }.execute()

            Log.d("YouTubeService", "Search results: ${search.items.map { it.snippet?.title }}")

            search.items.mapNotNull { item ->
                item.id?.videoId?.let { videoId ->
                    val audioUrl = getAudioStreamUrl(videoId) ?: return@mapNotNull null

                    Track(
                        id = videoId,
                        title = item.snippet.title ?: "Unknown",
                        artist = item.snippet.channelTitle ?: "Unknown",
                        duration = "0:00", // We'll implement duration later
                        thumbnailUrl = item.snippet.thumbnails?.default?.url ?: "",
                        audioUrl = audioUrl

                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("YouTubeService", "Error fetching songs: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAudioStreamUrl(videoId: String): String? {
        val sampleUrls = listOf(
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        )
        return sampleUrls.random()
    }

}