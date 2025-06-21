package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.frontend.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var avatarButton: ImageButton
    private lateinit var navigationView: NavigationView
    private lateinit var miniPlayerContainer: ConstraintLayout
    private lateinit var binding: ActivityMainBinding
    private var lastSearchQuery: String = "popular song"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // View bindings
        drawerLayout = binding.drawerLayout
        avatarButton = binding.btnAvatar
        navigationView = binding.navView
        miniPlayerContainer = binding.miniPlayer.root as ConstraintLayout // ✅ FIXED

        // Open sidebar when clicking avatar
        avatarButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //load recommendation songs
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()

        // Get current logged-in email
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

        // Sidebar menu item click
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_account_info -> {
                    val intent = Intent(this, UserInfoActivity::class.java)
                    intent.putExtra("email", currentUserEmail)
                    startActivity(intent)
                }

                R.id.nav_logout -> {
                    showLogoutDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        // Detect swipe up gesture on mini player
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

        // Attach swipe + click to mini player container
        miniPlayerContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        miniPlayerContainer.setOnClickListener {
            showQueue()
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
