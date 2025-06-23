package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SignUpActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var btnSendCode: Button
    private lateinit var btnVerifyCode: Button
    private lateinit var btnSignUp: Button
    private lateinit var includeOTP: View
    private lateinit var codeInputs: List<EditText>
    private val client = OkHttpClient()

    private val baseUrl = "https://us-central1-musicplayer-otp.cloudfunctions.net/api"
    private var isOtpVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnSignUp = findViewById(R.id.btnSignUp)
        includeOTP = findViewById(R.id.includeOTP)
        btnVerifyCode = includeOTP.findViewById(R.id.btnVerifyCode)

        codeInputs = listOf(
            includeOTP.findViewById(R.id.code1),
            includeOTP.findViewById(R.id.code2),
            includeOTP.findViewById(R.id.code3),
            includeOTP.findViewById(R.id.code4),
            includeOTP.findViewById(R.id.code5),
            includeOTP.findViewById(R.id.code6)
        )

        setupOTPInputs()

        btnSendCode.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) sendOTP(email) else toast("Vui lòng nhập email")
        }

        btnVerifyCode.setOnClickListener {
            val email = emailInput.text.toString()
            val otp = codeInputs.joinToString("") { it.text.toString().trim() }
            if (otp.length == 6) verifyOTP(email, otp) else toast("Nhập đủ 6 số xác minh nha!")
        }

        btnSignUp.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (!isOtpVerified) {
                toast("Xác minh OTP trước đã!")
                return@setOnClickListener
            }

            if (password != confirmPassword) toast("Mật khẩu không khớp!")
            else if (password.length < 6) toast("Mật khẩu phải ít nhất 6 ký tự")
            else signUp(email, password)
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun sendOTP(email: String) {
        val json = JSONObject().apply { put("email", email) }

        val request = Request.Builder()
            .url("$baseUrl/sendOtpEmail")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi gửi OTP: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                runOnUiThread {
                    try {
                        if (!response.isSuccessful) {
                            toast("Server lỗi: ${response.code}")
                            return@runOnUiThread
                        }
                        val json = JSONObject(body)
                        when (json.getString("status")) {
                            "success" -> {
                                toast("Mã OTP đã gửi về email!")
                                includeOTP.visibility = View.VISIBLE
                            }
                            else -> toast("Không gửi được OTP: ${json.optString("message")}")
                        }
                    } catch (e: Exception) {
                        toast("Phản hồi lỗi JSON: ${e.message}")
                        Log.e("SEND_OTP_ERROR", e.message ?: "Unknown")
                    }
                }
            }
        })
    }

    private fun verifyOTP(email: String, otp: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("otp", otp)
        }

        val request = Request.Builder()
            .url("$baseUrl/verifyOtp")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi xác minh OTP: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                runOnUiThread {
                    try {
                        if (!response.isSuccessful) {
                            toast("Lỗi từ server: ${response.code}")
                            return@runOnUiThread
                        }

                        val json = JSONObject(body)
                        when (json.getString("status")) {
                            "verified" -> {
                                isOtpVerified = true
                                toast("Xác minh thành công!")
                                showPasswordFields()
                            }
                            "invalid" -> toast("Sai mã xác minh!")
                            "expired" -> toast("Mã xác minh đã hết hạn!")
                            else -> toast("Phản hồi không hợp lệ!")
                        }
                    } catch (e: Exception) {
                        toast("Lỗi xử lý JSON: ${e.message}")
                        Log.e("VERIFY_OTP_ERROR", e.message ?: "Unknown")
                    }
                }
            }
        })
    }

    private fun signUp(email: String, password: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val request = Request.Builder()
            .url("$baseUrl/registerUser")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi đăng ký: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                runOnUiThread {
                    try {
                        if (!response.isSuccessful) {
                            toast("Lỗi server: ${response.code}")
                            return@runOnUiThread
                        }

                        val json = JSONObject(body)
                        when (json.getString("status")) {
                            "exists" -> {
                                AlertDialog.Builder(this@SignUpActivity)
                                    .setTitle("Email đã tồn tại")
                                    .setMessage("Email này đã có tài khoản. Bạn muốn đăng nhập?")
                                    .setPositiveButton("Đăng nhập") { _, _ ->
                                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                                        finish()
                                    }
                                    .setNegativeButton("Thoát") { _, _ -> finishAffinity() }
                                    .show()
                            }
                            "success" -> {
                                val intent = Intent(this@SignUpActivity, UserInfoActivity::class.java)
                                intent.putExtra("email", email)
                                startActivity(intent)
                                finish()
                            }
                            else -> toast("Đăng ký thất bại: ${json.optString("message")}")
                        }
                    } catch (e: Exception) {
                        toast("Lỗi xử lý phản hồi: ${e.message}")
                    }
                }
            }
        })
    }

    private fun showPasswordFields() {
        passwordInput.visibility = View.VISIBLE
        confirmPasswordInput.visibility = View.VISIBLE
        btnSignUp.visibility = View.VISIBLE
        includeOTP.visibility = View.GONE
        btnSendCode.visibility = View.GONE
    }

    private fun setupOTPInputs() {
        for (i in codeInputs.indices) {
            codeInputs[i].addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < codeInputs.size - 1) {
                        codeInputs[i + 1].requestFocus()
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
