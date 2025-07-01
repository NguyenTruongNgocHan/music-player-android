package com.example.frontend

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import android.util.Log
import android.util.SparseArray
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.kotlinyoutubeextractor.getVideoOnly
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.YtFile
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import com.maxrave.kotlinyoutubeextractor.State
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class YouTubeService(private val context: Context) {

    private val TAG = "YouTubeService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            chain.proceed(request)
        }
        .build()

    private val youtube: YouTube by lazy {
        YouTube.Builder(
            NetHttpTransport(),
            JacksonFactory(),
            HttpRequestInitializer { /* No auth needed */ }
        ).setApplicationName(context.getString(R.string.app_name)).build()
    }

    suspend fun searchSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val searchResponse = youtube.search().list("id,snippet").apply {
                q = query
                type = "video"
                videoCategoryId = "10" // Music
                maxResults = 10
                key = context.getString(R.string.youtube_api_key)
            }.execute()

            val videoIds = searchResponse.items.mapNotNull { it.id?.videoId }


            val videoDetails = youtube.videos().list("snippet,contentDetails,statistics").apply {
            id = videoIds.joinToString(",")
                key = context.getString(R.string.youtube_api_key)
            }.execute()

            videoDetails.items.map { video ->
                val viewCount = video.statistics?.viewCount?.toLong() ?: 0L
                Track(
                    id = video.id,
                    title = video.snippet.title ?: "Unknown",
                    artist = video.snippet.channelTitle ?: "Unknown",
                    duration = parseDuration(video.contentDetails.duration),
                    thumbnailUrl = video.snippet.thumbnails?.default?.url ?: "",
                    viewCount = viewCount
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during search: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAudioStreamUrl(context: Context, videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://musicextractor-production.up.railway.app/extract-audio")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = """{"videoId": "$videoId"}"""
            conn.outputStream.use { it.write(jsonBody.toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return@withContext json.optString("audioUrl", null)

        } catch (e: Exception) {
            Log.e("AudioFetch", "Failed to get audio URL", e)
            return@withContext null
        }
    }

    @SuppressLint("DefaultLocale")
    private fun parseDuration(isoDuration: String): String {
        val pattern = Pattern.compile("PT(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?")
        val matcher = pattern.matcher(isoDuration)

        if (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            val seconds = matcher.group(3)?.toIntOrNull() ?: 0

            return if (hours > 0)
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            else
                String.format("%d:%02d", minutes, seconds)
        }
        return "0:00"
    }

    suspend fun getVideoStreamUrl(context: Context, videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://musicextractor-production.up.railway.app/extract-video")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = """{"videoId": "$videoId"}"""
            conn.outputStream.use { it.write(jsonBody.toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return@withContext json.optString("videoUrl", null)

        } catch (e: Exception) {
            Log.e("VideoFetch", "Failed to get video URL", e)
            return@withContext null
        }
    }
}
