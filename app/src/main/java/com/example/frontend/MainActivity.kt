package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.SearchView
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

        drawerLayout = binding.drawerLayout
        avatarButton = binding.btnAvatar
        navigationView = binding.navView
        miniPlayerView = binding.miniPlayer.root as ConstraintLayout

        // Sidebar toggle
        avatarButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Sidebar nav actions
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
        /*
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    lastSearchQuery = query
                    showQueue(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = false
        })

         */

        // Swipe gesture mini player
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

        loadUserAvatar()
        loadTracks()

    }
    private fun loadTracks() {
        val db = Firebase.firestore
        val trackContainer = findViewById<LinearLayout>(R.id.trackContainer)

        db.collection("tracks")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: "Unknown Title"
                    val artist = document.getString("artist") ?: "Unknown Artist"
                    val duration = document.getString("duration") ?: "0:00"
                    val thumbnailUrl = document.getString("thumbnailUrl")

                    val trackView = layoutInflater.inflate(R.layout.item_track, trackContainer, false)

                    trackView.findViewById<TextView>(R.id.tvTitle).text = title
                    trackView.findViewById<TextView>(R.id.tvArtist).text = artist
                    trackView.findViewById<TextView>(R.id.tvDuration).text = duration

                    val imageView = trackView.findViewById<ImageView>(R.id.imgThumbnail)
                    Glide.with(this)
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.example)
                        .centerCrop()
                        .into(imageView)

                    // Optional: set click để mở Queue hay PlayerActivity
                    trackView.setOnClickListener {
                        val intent = Intent(this, PlayerActivity::class.java)
                        intent.putExtra("title", title)
                        intent.putExtra("artist", artist)
                        intent.putExtra("thumbnailUrl", thumbnailUrl)
                        startActivity(intent)
                    }

                    trackContainer.addView(trackView)
                }
            }
            .addOnFailureListener {
                // fallback nếu cần
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
            miniPlayerView.findViewById<TextView>(R.id.trackTitle).text = track.title
            miniPlayerView.findViewById<TextView>(R.id.trackArtist).text = track.artist
            miniPlayerView.findViewById<TextView>(R.id.trackDuration).text = "3:45"
            Glide.with(this).load(track.thumbnailUrl)
                .placeholder(R.drawable.default_avt)
                .into(miniPlayerView.findViewById(R.id.trackThumbnail))
        } else {
            miniPlayerView.visibility = View.GONE
        }
    }

    private fun showQueue(search: String = lastSearchQuery) {
        val existing = supportFragmentManager.findFragmentByTag("QUEUE_FRAGMENT")
        if (existing == null) {
            val queueFragment = QueueFragment().apply {
                arguments = Bundle().apply {
                    putString("search_query", search)
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

    private fun loadUserAvatar() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = Firebase.firestore
        val userDoc = db.collection("users").document(email)
        userDoc.get().addOnSuccessListener { document ->
            val avatarUrl = document.getString("avatarUrl")
            val username = document.getString("name") ?: "User"
            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.default_avt)
                    .circleCrop()
                    .into(avatarButton)
            }
            // Sidebar avatar + name
            val headerView = navigationView.getHeaderView(0)
            headerView.findViewById<ImageView>(R.id.avatarInSidebar).let {
                Glide.with(this).load(avatarUrl).placeholder(R.drawable.default_avt).circleCrop().into(it)
            }
            headerView.findViewById<TextView>(R.id.usernameInSidebar).text = username
        }
    }
}
