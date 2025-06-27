package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.frontend.LoginActivity
import com.example.frontend.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.frontend.R


class LoadingActivity : AppCompatActivity() {

    private val SESSION_TIMEOUT = 7 * 24 * 60 * 60 * 1000L // 7 ngày in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading)

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 2000) // Giảm còn 2s cho đỡ chờ lâu
    }

    private fun checkLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            // 1. Chưa đăng nhập
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // 2. Đã đăng nhập, kiểm tra thời gian phiên
            val email = user.email ?: return goToLogin()

            Firebase.firestore.collection("users").document(email).get()
                .addOnSuccessListener { doc ->
                    val createdAt = doc.getLong("createdAt") ?: return@addOnSuccessListener goToLogin()
                    val now = System.currentTimeMillis()
                    val lastLogin = doc.getLong("lastLogin") ?: return@addOnSuccessListener goToLogin()


                    if (now - lastLogin < SESSION_TIMEOUT) {
                        // 2.1 Phiên còn hạn
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // 2.2 Hết hạn phiên
                        FirebaseAuth.getInstance().signOut()
                        goToLogin()
                    }
                    finish()
                }
                .addOnFailureListener {
                    goToLogin()
                }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
