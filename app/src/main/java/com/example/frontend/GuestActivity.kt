package com.example.frontend

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class GuestActivity : AppCompatActivity() {

    private lateinit var miniPlayer: View
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guest_activity)

        // Avatar click → hiển thị dialog
        findViewById<View>(R.id.btnGuestAvatar).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Bạn đang ở chế độ khách")
                .setMessage("Đăng nhập để có trải nghiệm tốt hơn.")
                .setPositiveButton("Đăng nhập") { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton("Đăng ký") { _, _ ->
                    startActivity(Intent(this, SignUpActivity::class.java))
                }
                .setNeutralButton("Để sau", null)
                .show()
        }

        // Gán và xử lý mini player
        miniPlayer = findViewById(R.id.miniPlayer)
        miniPlayer.setOnClickListener {
            showQueueFragment()
        }

        // Hiển thị queue thẳng trong content luôn
        showQueueFragment()
    }

    private fun showQueueFragment() {
        val fragment = QueueFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.guestMainContent, fragment)
            .commit()
    }

    fun playTrack(track: Track) {
        miniPlayer.visibility = View.VISIBLE

        val title = miniPlayer.findViewById<TextView>(R.id.trackTitle)
        val artist = miniPlayer.findViewById<TextView>(R.id.trackArtist)
        val duration = miniPlayer.findViewById<TextView>(R.id.trackDuration)
        val thumb = miniPlayer.findViewById<ImageView>(R.id.trackThumbnail)

        title.text = track.title
        artist.text = track.artist
        duration.text = track.duration

        Glide.with(this)
            .load(track.thumbnailUrl)
            .placeholder(R.drawable.ic_music_note)
            .into(thumb)

        // Phát nhạc
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(track.audioUrl)
            prepareAsync()
            setOnPreparedListener {
                start()
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@GuestActivity, "Không phát được nhạc!", Toast.LENGTH_SHORT).show()
                true
            }
        }

        Toast.makeText(this, "Đang phát: ${track.title}", Toast.LENGTH_SHORT).show()
    }

}
