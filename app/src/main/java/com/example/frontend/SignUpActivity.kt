package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
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

    private val serverBaseUrl = "http://192.168.183.250/music_app_backend"

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
            if (email.isNotEmpty()) {
                sendOTP(email)
            } else {
                toast("Vui lòng nhập email")
            }
        }

        btnVerifyCode.setOnClickListener {
            val email = emailInput.text.toString()
            val otp = codeInputs.joinToString("") { it.text.toString().trim() }

            Log.d("OTP_ENTERED", "OTP: $otp")
            if (otp.length == 6) {
                verifyOTP(email, otp)
            } else {
                toast("Nhập đủ 6 số xác minh nha!")
            }
        }

        btnSignUp.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (password != confirmPassword) {
                toast("Mật khẩu không khớp!")
            } else {
                signUp(email, password)
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun sendOTP(email: String) {
        val requestBody = FormBody.Builder().add("email", email).build()
        val request = Request.Builder()
            .url("$serverBaseUrl/send_otp.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi gửi OTP: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                Log.d("SEND_OTP_RESPONSE", body)
                runOnUiThread {
                    try {
                        val json = JSONObject(body)
                        if (json.getString("status") == "success") {
                            toast("Mã OTP đã gửi về email!")
                            includeOTP.visibility = View.VISIBLE
                        } else {
                            toast("Không gửi được mã OTP")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("Lỗi phản hồi từ server khi gửi OTP")
                    }
                }
            }
        })
    }

    private fun verifyOTP(email: String, otp: String) {
        val requestBody = FormBody.Builder()
            .add("email", email)
            .add("otp", otp)
            .build()

        val request = Request.Builder()
            .url("$serverBaseUrl/verify_otp.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi xác minh OTP: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                Log.d("VERIFY_OTP_RESPONSE", body)
                runOnUiThread {
                    try {
                        val json = JSONObject(body)
                        when (json.getString("status")) {
                            "verified" -> {
                                toast("Xác minh thành công!")
                                showPasswordFields()
                            }
                            "invalid" -> toast("Sai mã xác minh!")
                            else -> toast("Phản hồi không hợp lệ!")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("Lỗi JSON: ${e.message}")
                    }
                }
            }
        })
    }

    private fun signUp(email: String, password: String) {
        val requestBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("$serverBaseUrl/signup.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Lỗi đăng ký: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                Log.d("SIGN_UP_RESPONSE", body)
                runOnUiThread {
                    try {
                        val json = JSONObject(body)
                        when (json.getString("status")) {
                            "success" -> {
                                toast("Đăng ký thành công!")
                                startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                                finish()
                            }
                            "exists" -> toast("Email đã tồn tại!")
                            else -> toast("Đăng ký thất bại!")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("Lỗi JSON khi đăng ký")
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
