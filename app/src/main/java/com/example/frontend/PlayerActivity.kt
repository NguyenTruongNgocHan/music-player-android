package com.example.frontend

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import com.example.frontend.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    private val handler = Handler(Looper.getMainLooper())

    private var currentSongIndex = 0
    private var mediaPlayer: MediaPlayer? = null
    private var isShuffle = false
    private var isRepeat = false
    private var isPlaying = false

    // Danh sách các bài hát
    private val songList = mutableListOf(
        Song("1","Bài hát 1", "Nghệ sĩ 1",  R.drawable.example, R.raw.song1),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        updateSong(songList[currentSongIndex])
        updateShuffleButton()
        updateRepeatButton()
        updateButtonListeners()
        updateSeekBarListener()
        setupHeartIcon()
        updateHeartIconState()
    }

    private fun updateButtonListeners() {
        binding.playButton.setOnClickListener {
            if (isPlaying) pauseSong() else playSong()
            isPlaying = !isPlaying
            updatePlayButtonIcon()
        }

        binding.nextButton.setOnClickListener { playNextSong() }
        binding.previousButton.setOnClickListener { playPreviousSong() }

        binding.shuffleButton.setOnClickListener {
            isShuffle = !isShuffle
            updateShuffleButton()
        }

        binding.repeatButton.setOnClickListener {
            isRepeat = !isRepeat
            updateRepeatButton()
        }
    }

    private fun updateHeartIconState() {
        binding.favoriteButton.setOnClickListener {
            val song = songList[currentSongIndex]
            song.isLiked = !song.isLiked
            setupHeartIcon()
        }
    }

    private fun setupHeartIcon() {
        val song = songList[currentSongIndex]
        if (song.isLiked) {
            binding.favoriteButton.setImageResource(R.drawable.heart_filled)
        } else {
            binding.favoriteButton.setImageResource(R.drawable.heart_empty)
        }
    }

    //cập nhật bài hát
    private fun updateSong(song: Song) {
        binding.albumArt.setImageResource(song.image)
        binding.songTitle.text = song.title
        binding.artist.text = song.artist
        binding.progressBar.progress = 0
    }

    // Xử lý thao tác với thanh seekbar
    private fun updateSeekBarListener() {
        binding.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(updateSeekBarRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                handler.post(updateSeekBarRunnable)
            }
        })
    }

    // cập nhật thanh tiến trinh và thời gian
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                binding.progressBar.max = it.duration
                binding.progressBar.progress = it.currentPosition
                binding.currentTime.text = formatTime(it.currentPosition) // thêm dòng này
                handler.postDelayed(this, 500)
            }
        }
    }

    //Cập nhật trạng thái nút shuffle
    private fun updateShuffleButton() {
        if (isShuffle) {
            binding.shuffleButton.setColorFilter(getColor(android.R.color.white))
            binding.shuffleButton.alpha = 1.0f
        } else {
            binding.shuffleButton.setColorFilter(getColor(android.R.color.darker_gray))
            binding.shuffleButton.alpha = 0.4f
        }
    }
    // Cập nhật trạng thái nút repeat
    private fun updateRepeatButton() {
        if (isRepeat) {
            binding.repeatButton.setColorFilter(getColor(android.R.color.white))
            binding.repeatButton.alpha = 1.0f
        } else {
            binding.repeatButton.setColorFilter(getColor(android.R.color.darker_gray))
            binding.repeatButton.alpha = 0.4f
        }
    }

    // định dạng thời gian
    private fun formatTime(ms: Int): String {
        val minutes = ms / 1000 / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // cập nhật nút phát nhạc
    private fun updatePlayButtonIcon() {
        val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        binding.playButton.setImageResource(icon)
    }

    private fun playSong() {
        if (mediaPlayer == null) {
            val song = songList[currentSongIndex]
            mediaPlayer = MediaPlayer.create(this, song.audio)
            val duration = mediaPlayer?.duration ?: 0
            binding.totalTime.text = formatTime(duration)

            mediaPlayer?.setOnCompletionListener {
                if (isRepeat) {
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                } else {
                    playNextSong()
                }
            }
        }
        mediaPlayer?.start()
        handler.post(updateSeekBarRunnable)
    }
    // tạm dừng bài hát
    private fun pauseSong() {
        mediaPlayer?.pause()
        handler.removeCallbacks(updateSeekBarRunnable)

    }
    // nghe bài tiếp theo
    private fun playNextSong() {
        mediaPlayer?.release()
        mediaPlayer = null

        currentSongIndex = if (isShuffle) {
            Random.nextInt(songList.size)
        } else {
            (currentSongIndex + 1) % songList.size
        }

        updateSong(songList[currentSongIndex])
        playSong()
        isPlaying = true
        updatePlayButtonIcon()
        setupHeartIcon()
    }
    // Nghe bài trước
    private fun playPreviousSong() {
        mediaPlayer?.release()
        mediaPlayer = null

        currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
        updateSong(songList[currentSongIndex])
        playSong()
        isPlaying = true
        updatePlayButtonIcon()
        setupHeartIcon()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(updateSeekBarRunnable)
    }
}