package com.example.frontend

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ActivityPlayerBinding
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.random.Random

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentTrackIndex = 0
    private var isShuffle = false
    private var isRepeat = false
    private var isPlaying = false
    private var isChangingTrack = false

    private var songList: List<Track> = emptyList()

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.duration > 0) {
                binding.progressBar.max = exoPlayer.duration.toInt()
                binding.progressBar.progress = exoPlayer.currentPosition.toInt()
                binding.currentTime.text = formatTime(exoPlayer.currentPosition)
                binding.totalTime.text = formatTime(exoPlayer.duration)
            }
            handler.postDelayed(this, 500)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start PlayerService to ensure background playback
        val serviceIntent = Intent(this, PlayerService::class.java)
        startService(serviceIntent)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create local player for UI control
        exoPlayer = PlayerService.sharedPlayer ?: ExoPlayer.Builder(this).build().also {
            PlayerService.sharedPlayer = it
        }

        val receivedQueue = intent.getSerializableExtra("queue") as? ArrayList<Track>
        val selectedTrackId = intent.getStringExtra("track_id")

        val positionFromVideo = intent.getLongExtra("position", 0L)

        if (!receivedQueue.isNullOrEmpty()) {
            songList = receivedQueue
            currentTrackIndex = songList.indexOfFirst { it.id == selectedTrackId }.takeIf { it != -1 } ?: 0

            setupUI()
            binding.btnSwitchToVideo.setOnClickListener {
                val currentTrack = songList[currentTrackIndex]
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra("videoUrl", cachedVideoUrl)
                    putExtra("videoId", currentTrack.id)
                    putExtra("title", currentTrack.title)
                    putExtra("artist", currentTrack.artist)
                    putExtra("thumbnail", currentTrack.thumbnailUrl)
                    putExtra("position", exoPlayer.currentPosition)
                }
                Log.d("LaunchVideo", "Launching with URL = $cachedVideoUrl")
                startActivityForResult(intent, 1001)
            }
            playCurrentTrack(positionFromVideo)
        }
    }

    private var cachedVideoUrl: String? = null

    @UnstableApi
    private fun preloadVideoUrl(track: Track) {
        CoroutineScope(Dispatchers.IO).launch {
            val service = YouTubeService(this@PlayerActivity)
            cachedVideoUrl = service.getVideoStreamUrl(this@PlayerActivity, track.id)
            Log.d("PreloadVideo", "Video URL: $cachedVideoUrl")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val returnedPosition = data?.getLongExtra("position", 0L) ?: 0L
            exoPlayer.seekTo(returnedPosition)
        }
    }

    private fun setupUI() {
        updateTrack(songList[currentTrackIndex])
        updateShuffleButton()
        updateRepeatButton()
        updatePlayButtonIcon()
        setupListeners()
        updateHeartIconState()
    }

    private fun setupListeners() {
        binding.playButton.setOnClickListener {
            if (isPlaying) {
                exoPlayer.pause()
                handler.removeCallbacks(updateSeekBarRunnable)
            } else {
                exoPlayer.play()
                handler.post(updateSeekBarRunnable)
            }
            isPlaying = !isPlaying
            updatePlayButtonIcon()
            MiniPlayerController.updatePlayState()

            // Notify service about playback state
            notifyServicePlaybackState()
        }

        binding.nextButton.setOnClickListener { playNextTrack() }
        binding.previousButton.setOnClickListener { playPreviousTrack() }

        binding.shuffleButton.setOnClickListener {
            isShuffle = !isShuffle
            updateShuffleButton()
        }

        binding.repeatButton.setOnClickListener {
            isRepeat = !isRepeat
            exoPlayer.repeatMode = if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            updateRepeatButton()
        }

        binding.favoriteButton.setOnClickListener {
            val song = songList[currentTrackIndex]
            song.isLiked = !song.isLiked
            updateHeartIconState()
        }

        binding.progressBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) exoPlayer.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                handler.removeCallbacks(updateSeekBarRunnable)
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                handler.post(updateSeekBarRunnable)
            }
        })

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> playNextTrack()
                    Player.STATE_READY -> {
                        binding.totalTime.text = formatTime(exoPlayer.duration)
                        MiniPlayerController.updatePlayState()
                    }
                }
            }
        })
    }

    @OptIn(UnstableApi::class)
    private fun playCurrentTrack(startPosition: Long = 0L) {
        val currentTrack = songList[currentTrackIndex]
        updateTrack(currentTrack)
        preloadVideoUrl(currentTrack)
        exoPlayer.repeatMode = if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

        coroutineScope.launch {
            try {
                val youTubeService = YouTubeService(this@PlayerActivity)
                val streamUrl =
                    youTubeService.getAudioStreamUrl(this@PlayerActivity, currentTrack.id)

                if (streamUrl != null) {
                   // exoPlayer.stop() // stop any previous song
                    exoPlayer.clearMediaItems()
                    val mediaItem = MediaItem.fromUri(streamUrl)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(startPosition)
                    exoPlayer.play()
                    isPlaying = true
                    updatePlayButtonIcon()
                    handler.post(updateSeekBarRunnable)

                    // Notify service about the current track
                    notifyServiceCurrentTrack()
                } else {
                    Toast.makeText(this@PlayerActivity, "Unable to load audio", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isChangingTrack = false
            }
        }
    }

    private fun notifyServicePlaybackState() {
        val intent = Intent(this, PlayerService::class.java).apply {
            action = if (isPlaying) "PLAY" else "PAUSE"
        }
        startService(intent)
    }

    private fun notifyServiceCurrentTrack() {
        val intent = Intent(this, PlayerService::class.java).apply {
            action = "SET_TRACK"
            putExtra("track", songList[currentTrackIndex])
            putExtra("position", exoPlayer.currentPosition)
        }
        startService(intent)
    }

    private fun formatTime(timeMs: Long): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun playNextTrack() {
        if (isChangingTrack) return
        isChangingTrack = true

        currentTrackIndex = if (isShuffle) {
            Random.nextInt(songList.size)
        } else {
            (currentTrackIndex + 1) % songList.size
        }
        updateTrack(songList[currentTrackIndex])
        playCurrentTrack()
        isPlaying = true
        updatePlayButtonIcon()
        updateHeartIconState()
    }

    private fun playPreviousTrack() {
        if (isChangingTrack) return
        isChangingTrack = true

        currentTrackIndex = if (currentTrackIndex - 1 < 0) songList.size - 1 else currentTrackIndex - 1
        updateTrack(songList[currentTrackIndex])
        playCurrentTrack()
        isPlaying = true
        updatePlayButtonIcon()
        updateHeartIconState()
    }

    private fun updateTrack(track: Track) {
        Glide.with(this)
            .load(track.thumbnailUrl)
            .centerCrop()
            .placeholder(R.drawable.example)
            .error(R.drawable.example)
            .into(binding.albumArt)

        binding.songTitle.text = track.title
        binding.artist.text = track.artist
        binding.progressBar.progress = 0
    }

    private fun updateHeartIconState() {
        val iconRes = if (songList[currentTrackIndex].isLiked) {
            R.drawable.heart_filled
        } else {
            R.drawable.heart_empty
        }
        binding.favoriteButton.setImageResource(iconRes)
    }

    private fun updateShuffleButton() {
        binding.shuffleButton.setColorFilter(
            getColor(if (isShuffle) android.R.color.white else android.R.color.darker_gray)
        )
        binding.shuffleButton.alpha = if (isShuffle) 1.0f else 0.4f
    }

    private fun updateRepeatButton() {
        binding.repeatButton.setColorFilter(
            getColor(if (isRepeat) android.R.color.white else android.R.color.darker_gray)
        )
        binding.repeatButton.alpha = if (isRepeat) 1.0f else 0.4f
    }

    private fun updatePlayButtonIcon() {
        val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        binding.playButton.setImageResource(icon)
    }

    override fun onDestroy() {
        MiniPlayerManager.setTrack(songList[currentTrackIndex])
        super.onDestroy()
        //exoPlayer.release()
        handler.removeCallbacks(updateSeekBarRunnable)
        coroutineScope.cancel()
    }
}