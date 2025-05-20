package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnGuest: Button
    private lateinit var btnForgotPassword: Button

    private val client = OkHttpClient()
    private val baseUrl = "https://us-central1-musicplayer-otp.cloudfunctions.net/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGuest = findViewById(R.id.btnGuest)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Vui lòng nhập đầy đủ email và mật khẩu")
            } else {
                login(email, password)
            }
        }

        btnGuest.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun login(email: String, password: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val request = Request.Builder()
            .url("$baseUrl/loginUser")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi kết nối server: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""

                Log.d("LOGIN_RESPONSE", body)

                runOnUiThread {
                    if (!response.isSuccessful) {
                        toast("Lỗi server: ${response.code}")
                        return@runOnUiThread
                    }

                    try {
                        val json = JSONObject(body)
                        when (json.optString("status")) {
                            "success" -> {
                                toast("Đăng nhập thành công!")
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                            "invalid_password" -> toast("Sai mật khẩu!")
                            "no_user" -> toast("Không tìm thấy tài khoản!")
                            else -> toast("Lỗi không xác định: ${json.optString("message")}")
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_JSON_ERROR", "Lỗi JSON: ${e.message}")
                        toast("Phản hồi không hợp lệ từ server!")
                    }
                }
            }
        })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
