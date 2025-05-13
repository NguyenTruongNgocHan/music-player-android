package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import okhttp3.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnGuest: Button
    private lateinit var btnForgotPassword: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // Gọi trước khi findViewById
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        // Gán giá trị sau khi setContentView
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGuest = findViewById(R.id.btnGuest)

        btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnLogin.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                toast("Vui lòng nhập đầy đủ email và mật khẩu")
            }
        }
        btnGuest.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }


    }

    private fun login(email: String, password: String) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("http://192.168.183.250/music_app_backend/login.php") // hoặc localhost nếu chạy trên máy thật
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi kết nối server: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val rawBody = response.body?.string() ?: ""

                runOnUiThread {
                    try {
                        Log.d("LOGIN_BODY", "Server trả về: $rawBody")

                        val json = JSONObject(rawBody)
                        when (json.getString("status")) {
                            "success" -> {
                                toast("Đăng nhập thành công!")
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            "invalid_password" -> toast("Sai mật khẩu!")
                            "no_user" -> toast("Không tìm thấy tài khoản!")
                            else -> toast("Lỗi không xác định!")
                        }

                    } catch (e: Exception) {
                        Log.e("LOGIN_JSON_ERROR", "Lỗi JSON hoặc context: ${e.message}")
                        toast("Lỗi phản hồi từ server!")
                    }
                }
            }


        })
    }
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }



}
