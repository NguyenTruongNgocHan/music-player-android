package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.frontend.LoginActivity

import com.example.frontend.R

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading) // Đảm bảo đúng tên file XML


        // Chờ 3 giây rồi chuyển sang màn hình Login
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // Đóng LoadingActivity để không quay lại được
        }, 3000) // Thời gian delay (3 giây)
    }
}
