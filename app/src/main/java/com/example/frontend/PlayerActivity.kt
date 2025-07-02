package com.example.frontend

import NextListManager
import RelatedListManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ActivityPlayerBinding
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


    private lateinit var lyricsManager: LyricsManager
    private lateinit var lyricsAdapter: LyricsAdapter

    private lateinit var nextListAdapter: QueueAdapter
    private lateinit var nextListManager: NextListManager

    private lateinit var relatedListAdapter: QueueAdapter
    private lateinit var relatedListManager: RelatedListManager

    //private enum class Tab { NEXT, LYRICS, RELATED, CONTENT}
    private enum class Tab { NEXT, RELATED, CONTENT}
    private var currentTab: Tab = Tab.CONTENT

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

        lyricsAdapter = LyricsAdapter(emptyList())

        nextListAdapter = QueueAdapter().apply {
            setOnItemClickListener { position ->
                getTrackAt(position)?.let { track ->
                    playSelectedTrack(track)
                }
            }
        }
        relatedListAdapter = QueueAdapter().apply {
            setOnItemClickListener { position ->
                getTrackAt(position)?.let { track ->
                    playSelectedTrack(track)
                }
            }
        }

        binding.nextTabContainer.visibility = View.GONE
        //binding.lyricsTabContainer.visibility = View.GONE
        binding.relatedTabContainer.visibility = View.GONE
        /*
        binding.rvLyric.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = lyricsAdapter
        }
        */

        binding.rvNextList.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = nextListAdapter
        }

        binding.rvRelatedList.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
            adapter = relatedListAdapter
        }

        lyricsManager = LyricsManager(this)

        nextListManager = NextListManager(this, YouTubeService(this))

        relatedListManager = RelatedListManager(this, YouTubeService(this))

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
                exoPlayer.pause()
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
            setupTabs()
        }
    }


    private fun setupTabs() {
        binding.tabNext.setOnClickListener {
            switchTab(Tab.NEXT)
        }
        /*
        binding.tabLyric.setOnClickListener {
            switchTab(Tab.LYRICS)
        }
        */
        binding.tabRelated.setOnClickListener {
            switchTab(Tab.RELATED)
        }
        switchTab(Tab.CONTENT)
    }

    private fun switchTab(tab: Tab) {
        if (currentTab == tab) return

        /*
        if (currentTab == Tab.LYRICS) {
            handler.removeCallbacks(lyricsSyncRunnable)
        }
        */

        currentTab = tab
        updateTabAppearance()
        showCurrentTabContent()
    }

    private fun updateTabAppearance() {
        // Reset tất cả tab về trạng thái bình thường
        //listOf(binding.tabNext, binding.tabLyric, binding.tabRelated).forEach {
        listOf(binding.tabNext, binding.tabRelated).forEach {

            it.setTypeface(null, Typeface.NORMAL)
            it.alpha = 0.6f
        }

        // Highlight tab hiện tại
        when (currentTab) {
            Tab.CONTENT -> {}
            Tab.NEXT -> {
                binding.tabNext.setTypeface(null, Typeface.BOLD)
                binding.tabNext.alpha = 1f
            }
            /*
            Tab.LYRICS -> {
                binding.tabLyric.setTypeface(null, Typeface.BOLD)
                binding.tabLyric.alpha = 1f
            }
             */
            Tab.RELATED -> {
                binding.tabRelated.setTypeface(null, Typeface.BOLD)
                binding.tabRelated.alpha = 1f
            }
        }
    }

    private fun showCurrentTabContent() {
        // Ẩn tất cả các RecyclerView
        binding.nextTabContainer.visibility = View.GONE
        //binding.lyricsTabContainer.visibility = View.GONE
        binding.relatedTabContainer.visibility = View.GONE

        when (currentTab) {
            Tab.CONTENT -> {}
            Tab.NEXT -> {
                binding.nextTabContainer.visibility = View.VISIBLE
                showNextList()
            }
            /*
            Tab.LYRICS -> {
                binding.lyricsTabContainer.visibility = View.VISIBLE
                showLyric()
            }
            */
            Tab.RELATED -> {
                binding.relatedTabContainer.visibility = View.VISIBLE
                showRelatedList()
            }
        }

    }

    private fun updateCurrentlyPlayingTrack(trackId: String) {
        // Cập nhật cho nextListAdapter
        nextListAdapter.currentlyPlayingIndex = nextListAdapter.currentList.indexOfFirst { it.id == trackId }
        nextListAdapter.notifyDataSetChanged()

        // Cập nhật cho relatedListAdapter
        //relatedListAdapter.currentlyPlayingIndex = relatedListAdapter.currentList.indexOfFirst { it.id == trackId }
        //relatedListAdapter.notifyDataSetChanged()
    }

    private fun playSelectedTrack(track: Track) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                currentTrackIndex = songList.indexOfFirst { it.id == track.id }.takeIf { it != -1 } ?: -1
                // Nếu bài không nằm trong queue của tiếp theo
                if (currentTrackIndex == -1) {
                    loadNewNextList(track)
                    //currentTrackIndex = 0
                    showRelatedList()

                }
                updateCurrentlyPlayingTrack(track.id)
                playCurrentTrack()

            }
        }
    }

    private suspend fun loadNewNextList(track: Track) {
        val nextList = withContext(Dispatchers.IO) {
            nextListManager.loadNextList(track)
        }
        songList = listOf(track) + nextList
        currentTrackIndex = 0
    }

    private fun showNextList() {
        binding.contentProgress.visibility = View.VISIBLE
        binding.rvNextList.visibility = View.GONE
        binding.emptyNextText.visibility = View.GONE


        lifecycleScope.launch {
            try {
                val nextList = withContext(Dispatchers.IO) {
                    nextListManager.loadNextList(songList[currentTrackIndex])
                }
                songList = listOf(songList[currentTrackIndex]) + nextList
                currentTrackIndex = 0
                withContext(Dispatchers.Main) {
                    if (nextList.isEmpty()) {
                        binding.emptyNextText.visibility = View.VISIBLE
                    } else {
                        nextListAdapter.submitList(songList)
                        binding.rvNextList.visibility = View.VISIBLE
                    }
                    binding.contentProgress.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.contentProgress.visibility = View.GONE
                    binding.emptyNextText.visibility = View.VISIBLE
                    Toast.makeText(
                        this@PlayerActivity,
                        "Không thể tải danh sách tiếp theo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun showRelatedList() {
        binding.contentProgress.visibility = View.VISIBLE
        binding.rvRelatedList.visibility = View.GONE
        binding.emptyRelatedText.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val relatedList = withContext(Dispatchers.IO) {
                    relatedListManager.loadRelatedList(songList[currentTrackIndex])
                }

                withContext(Dispatchers.Main) {
                    if (relatedList.isEmpty()) {
                        binding.emptyRelatedText.visibility = View.VISIBLE
                    }
                    else {
                        binding.rvRelatedList.visibility = View.VISIBLE
                        relatedListAdapter.submitList(relatedList)
                    }
                    binding.contentProgress.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.emptyRelatedText.visibility = View.VISIBLE
                    binding.contentProgress.visibility = View.GONE
                    Toast.makeText(
                        this@PlayerActivity,
                        "Không thể tải bài hát liên quan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
/*
    private fun showLyric() {
        binding.contentProgress.visibility = View.VISIBLE
        binding.rvLyric.visibility = View.GONE
        binding.emptyLyricsText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val currentTrack = songList[currentTrackIndex]
                val lyrics = lyricsManager.fetchLyrics(currentTrack.title, currentTrack.artist)

                withContext(Dispatchers.Main) {
                    if (lyrics.isEmpty()) {
                        binding.emptyLyricsText.visibility = View.VISIBLE
                    } else {
                        lyricsAdapter.updateLyrics(lyrics)
                        //binding.rvLyric.adapter = LyricsAdapter(lyrics)
                        binding.rvLyric.visibility = View.VISIBLE
                        if (isPlaying) startLyricsSync()
                    }
                    binding.contentProgress.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.contentProgress.visibility = View.GONE
                    binding.emptyLyricsText.visibility = View.VISIBLE
                    Toast.makeText(
                        this@PlayerActivity,
                        "Không thể tải lời bài hát",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLyricsSync() {
        handler.post(lyricsSyncRunnable)
    }

    private val lyricsSyncRunnable = object : Runnable {
        override fun run() {
            if (binding.rvLyric.visibility == View.VISIBLE  && isPlaying) {
                val currentPos = exoPlayer.currentPosition
                updateHighlightedLyric(currentPos)
                handler.postDelayed(this, 100) // Cập nhật mỗi 100ms
            }
        }
    }

    private fun updateHighlightedLyric(currentTimeMs: Long) {
        val lyrics = (binding.rvLyric.adapter as? LyricsAdapter)?.getLyrics() ?: return

        // Tìm dòng hiện tại dựa trên thời gian
        var currentIndex = -1
        for (i in lyrics.indices) {
            if (currentTimeMs >= lyrics[i].startTimeMs &&
                (i == lyrics.size - 1 || currentTimeMs < lyrics[i + 1].startTimeMs)) {
                currentIndex = i
                break
            }
        }
        if (currentIndex != -1) {
            lyricsAdapter.updateCurrentPosition(currentIndex)

            // Tự động cuộn đến dòng hiện tại
            val layoutManager = binding.rvLyric.layoutManager as LinearLayoutManager
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()

            if (currentIndex <= firstVisible || currentIndex >= lastVisible) {
                layoutManager.scrollToPositionWithOffset(currentIndex, binding.rvLyric.height / 3)
            }
        }
    }
*/
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
