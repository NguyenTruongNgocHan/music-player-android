package com.example.frontend

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var includeOTP: View
    private lateinit var codeInputs: List<EditText>
    private lateinit var btnSendCode: Button
    private lateinit var btnResetPassword: Button
    private var verifiedOTP: String = ""

    private val client = OkHttpClient()
    private val baseUrl = "https://us-central1-musicplayer-otp.cloudfunctions.net"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpw)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        includeOTP = findViewById(R.id.includeOTP)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnResetPassword = findViewById(R.id.btnResetPassword)

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
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) sendOTP(email) else toast("Vui lòng nhập email")
        }

        includeOTP.findViewById<Button>(R.id.btnVerifyCode).setOnClickListener {
            val otp = codeInputs.joinToString("") { it.text.toString().trim() }
            if (otp.length == 6) verifyOTP(otp) else toast("Nhập đủ 6 số mã OTP!")
        }

        btnResetPassword.setOnClickListener {
            val pass1 = passwordInput.text.toString()
            val pass2 = confirmPasswordInput.text.toString()
            if (pass1 == pass2) resetPassword(pass1) else toast("Mật khẩu không khớp!")
        }
    }

    private fun sendOTP(email: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("mode", "forgot")
        }

        val request = Request.Builder()
            .url("$baseUrl/sendOtpEmail")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi gửi OTP: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    if (res.optString("status") == "success") {
                        toast("Mã OTP đã được gửi")
                        btnSendCode.visibility = View.GONE
                        includeOTP.visibility = View.VISIBLE
                    } else {
                        toast("Không gửi được OTP: ${res.optString("message")}")
                    }
                }
            }
        })
    }

    private fun verifyOTP(otp: String) {
        val json = JSONObject().apply {
            put("email", emailInput.text.toString())
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
                val res = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    if (res.optString("status") == "verified") {
                        toast("OTP hợp lệ, đổi mật khẩu nào")
                        verifiedOTP = otp
                        passwordInput.visibility = View.VISIBLE
                        confirmPasswordInput.visibility = View.VISIBLE
                        btnResetPassword.visibility = View.VISIBLE
                    } else {
                        toast("OTP không đúng hoặc đã hết hạn")
                    }
                }
            }
        })
    }

    private fun resetPassword(newPassword: String) {
        val json = JSONObject().apply {
            put("email", emailInput.text.toString())
            put("otp", verifiedOTP)
            put("password", newPassword)
        }

        val request = Request.Builder()
            .url("$baseUrl/resetPassword")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi cập nhật mật khẩu: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    if (res.optString("status") == "success") {
                        toast("Đổi mật khẩu thành công")
                        finish()
                    } else {
                        toast("Lỗi đổi mật khẩu: ${res.optString("message")}")
                    }
                }
            }
        })
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
}
