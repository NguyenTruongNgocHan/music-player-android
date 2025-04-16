package com.example.frontend

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import okhttp3.*
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
    private val serverBaseUrl = "http://192.168.183.250/music_app_backend"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpw)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        includeOTP = findViewById(R.id.includeOTP)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        btnSendCode.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) sendOTP(email) else toast("Vui lòng nhập email")
        }

        btnResetPassword.setOnClickListener {
            val pass1 = passwordInput.text.toString()
            val pass2 = confirmPasswordInput.text.toString()
            if (pass1 == pass2) resetPassword(pass1) else toast("Mật khẩu không khớp!")
        }
        val btnVerifyCode: Button = includeOTP.findViewById(R.id.btnVerifyCode)
        codeInputs = listOf(
            includeOTP.findViewById(R.id.code1),
            includeOTP.findViewById(R.id.code2),
            includeOTP.findViewById(R.id.code3),
            includeOTP.findViewById(R.id.code4),
            includeOTP.findViewById(R.id.code5),
            includeOTP.findViewById(R.id.code6)
        )
        setupOTPInputs()

        btnVerifyCode.setOnClickListener {
            val otp = codeInputs.joinToString("") { it.text.toString().trim() }
            if (otp.length == 6) verifyOTP(otp) else toast("Nhập đủ 6 số mã OTP!")
        }

    }

    private fun sendOTP(email: String) {
        val requestBody = FormBody.Builder()
            .add("email", email)
            .build()

        val request = Request.Builder()
            .url("$serverBaseUrl/send_otp_forgot.php")
            .post(requestBody)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread { toast("Phản hồi rỗng từ server") }
                    return
                }
                val res = JSONObject(responseBody)

                runOnUiThread {
                    when (res.getString("status")) {
                        "success" -> {
                            toast("Mã xác minh đã gửi về email")
                            btnSendCode.visibility = View.GONE  // ẩn đúng nghĩa
                            includeOTP.visibility = View.VISIBLE


                            val btnVerifyCode: Button = includeOTP.findViewById(R.id.btnVerifyCode)
                            codeInputs = listOf(
                                includeOTP.findViewById(R.id.code1),
                                includeOTP.findViewById(R.id.code2),
                                includeOTP.findViewById(R.id.code3),
                                includeOTP.findViewById(R.id.code4),
                                includeOTP.findViewById(R.id.code5),
                                includeOTP.findViewById(R.id.code6)
                            )

                            setupOTPInputs()
                            btnVerifyCode.visibility = View.VISIBLE

                            btnVerifyCode.setOnClickListener {
                                val otp = codeInputs.joinToString("") { it.text.toString().trim() }
                                if (otp.length == 6) verifyOTP(otp) else toast("Nhập đủ 6 số mã OTP!")
                            }
                        }
                        "not_found" -> {
                            AlertDialog.Builder(this@ForgotPasswordActivity)
                                .setTitle("Email chưa được đăng ký")
                                .setMessage("Email này chưa được dùng để tạo tài khoản. Bạn muốn đăng ký không?")
                                .setPositiveButton("Đăng ký") { _, _ ->
                                    startActivity(Intent(this@ForgotPasswordActivity, SignUpActivity::class.java))
                                    finish()
                                }
                                .setNegativeButton("Hủy") { _, _ ->
                                    startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                                    finish()
                                }
                                .show()
                        }
                        else -> toast("Lỗi không xác định: ${res.getString("message")}")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi server: ${e.message}") }
            }
        })
    }

    private fun verifyOTP(otp: String) {
        val body = FormBody.Builder()
            .add("email", emailInput.text.toString())
            .add("otp", otp)
            .build()

        val request = Request.Builder().url("$serverBaseUrl/verify_otp.php").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread { toast("Phản hồi rỗng từ server") }
                    return
                }
                val res = JSONObject(responseBody)

                runOnUiThread {
                    if (res.getString("status") == "verified") {
                        toast("OTP hợp lệ, đổi mật khẩu nào")
                        verifiedOTP = otp  // ← Lưu lại OTP đã xác minh thành công
                        passwordInput.visibility = View.VISIBLE
                        confirmPasswordInput.visibility = View.VISIBLE
                        btnResetPassword.visibility = View.VISIBLE
                    } else {
                        toast("OTP không đúng")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi xác minh: ${e.message}") }
            }
        })
    }

    private fun resetPassword(newPassword: String) {
        val body = FormBody.Builder()
            .add("email", emailInput.text.toString())
            .add("otp", verifiedOTP)
            .add("password", newPassword)
            .build()

        val request = Request.Builder().url("$serverBaseUrl/reset_password.php").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: return)
                runOnUiThread {
                    if (res.getString("status") == "success") {
                        toast("Mật khẩu đã cập nhật!")
                        finish()
                    } else toast("Cập nhật thất bại!")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi server: ${e.message}") }
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
