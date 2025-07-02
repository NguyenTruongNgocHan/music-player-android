package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.frontend.databinding.ActivityMainBinding
import com.example.frontend.LibraryFragmentActivity
import com.example.frontend.ui.search.SearchFragmentActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var miniPlayerView: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        navigationView = binding.navView
        miniPlayerView = binding.miniPlayer.root as ConstraintLayout

        setupBottomNavigation()
        setupSidebar()
        setupMiniPlayerSwipe()

        loadUserAvatarInSidebar()

        // Mặc định mở HomeFragmentActivity
        loadHomeFragmentActivity()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadHomeFragmentActivity()
                    true
                }
                R.id.nav_search -> loadFragment(SearchFragmentActivity())
                R.id.nav_library -> loadFragment(LibraryFragmentActivity())
                else -> false
            }
        }
    }

    private fun loadHomeFragmentActivity() {
        val fragment = HomeFragmentActivity()
        fragment.drawerCallback = {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    private fun setupSidebar() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_account_info -> {
                    val email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
                    startActivity(Intent(this, UserInfoActivity::class.java).apply {
                        putExtra("email", email)
                    })
                }
                R.id.nav_logout -> showLogoutDialog()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupMiniPlayerSwipe() {
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

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    private fun showQueue() {
        val existing = supportFragmentManager.findFragmentByTag("QUEUE_FRAGMENT")
        if (existing == null) {
            val queueFragment = QueueFragment()
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

    private fun loadUserAvatarInSidebar() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = Firebase.firestore
        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                val avatarUrl = document.getString("avatarUrl")
                val username = document.getString("name") ?: "User"
                val headerView = navigationView.getHeaderView(0)

                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avt)
                        .circleCrop()
                        .into(headerView.findViewById(R.id.avatarInSidebar))
                }

                headerView.findViewById<TextView>(R.id.usernameInSidebar).text = username
            }
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayerUI()
    }


    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }


    private fun updateMiniPlayerUI() {
        val track = MiniPlayerManager.getCurrentTrack()
        if (track != null) {
            miniPlayerView.visibility = View.VISIBLE
            miniPlayerView.findViewById<TextView>(R.id.trackTitle).text = track.title
            miniPlayerView.findViewById<TextView>(R.id.trackArtist).text = track.artist
            miniPlayerView.findViewById<TextView>(R.id.trackDuration).text = "3:45"
            Glide.with(this)
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.default_avt)
                .into(miniPlayerView.findViewById(R.id.trackThumbnail))
        } else {
            miniPlayerView.visibility = View.GONE
        }
    }
}
