package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var avatarButton: ImageButton
    private lateinit var navigationView: NavigationView
    private lateinit var miniPlayerView: ConstraintLayout
    private var lastSearchQuery: String = "popular song"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind views
        drawerLayout = binding.drawerLayout
        avatarButton = binding.btnAvatar
        navigationView = binding.navView
        miniPlayerView = binding.miniPlayer.root as ConstraintLayout

        // Set up sidebar toggle
        avatarButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Load initial HomeFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()

        // Sidebar actions
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_account_info -> {
                    startActivity(Intent(this, UserInfoActivity::class.java).apply {
                        putExtra("email", currentUserEmail)
                    })
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Search actions
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    lastSearchQuery = query
                    val queueFragment = QueueFragment().apply {
                        arguments = Bundle().apply {
                            putString("search_query", query)
                        }
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, queueFragment)
                        .addToBackStack(null)
                        .commit()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = false
        })

        // Swipe gesture on mini player
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false
                val diffY = e1.y - e2.y
                val diffX = e1.x - e2.x
                if (abs(diffY) > abs(diffX) && abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        showQueue()
                        return true
                    }
                }
                return false
            }
        })

        miniPlayerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        miniPlayerView.setOnClickListener {
            showQueue()
        }
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayerUI()
    }

    private fun updateMiniPlayerUI() {
        val track = MiniPlayerManager.getCurrentTrack()
        if (track != null) {
            miniPlayerView.visibility = View.VISIBLE

            val title = miniPlayerView.findViewById<TextView>(R.id.trackTitle)
            val artist = miniPlayerView.findViewById<TextView>(R.id.trackArtist)
            val duration = miniPlayerView.findViewById<TextView>(R.id.trackDuration)
            val thumbnail = miniPlayerView.findViewById<ImageView>(R.id.trackThumbnail)

            title.text = track.title
            artist.text = track.artist
            duration.text = "3:45" // Optional: set actual duration later
            Glide.with(this).load(track.thumbnailUrl).into(thumbnail)
        } else {
            //miniPlayerView.visibility = View.GONE
        }
    }

    private fun showQueue() {
        val existing = supportFragmentManager.findFragmentByTag("QUEUE_FRAGMENT")
        if (existing == null) {
            val queueFragment = QueueFragment().apply {
                arguments = Bundle().apply {
                    putString("search_query", lastSearchQuery)
                }
            }
            queueFragment.show(supportFragmentManager, "QUEUE_FRAGMENT")
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
