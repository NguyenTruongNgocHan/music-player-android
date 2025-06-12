package com.example.frontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.widget.AppCompatEditText


class UserInfoActivity : AppCompatActivity() {

    private lateinit var emailInput: AppCompatEditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userinfo)


        // Get view reference
        emailInput = findViewById(R.id.emailInput)

        // Get email from Intent
        val email = intent.getStringExtra("email") ?: ""

        // Display and lock field
        emailInput.setText(email)
        emailInput.isEnabled = false
    }
}
