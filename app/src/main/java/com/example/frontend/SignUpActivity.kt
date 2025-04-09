package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var btnSendCode: Button


    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var btnSignUp: Button

    private lateinit var codeInputs: List<EditText>
    private lateinit var btnVerifyCode: Button
    private lateinit var includeOTP: View
    private var serverCode = "123456" // Gán tạm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        // Gán View sau setContentView
        btnSendCode = findViewById(R.id.btnSendCode)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        btnSignUp = findViewById(R.id.btnSignUp)
        includeOTP = findViewById(R.id.includeOTP)

        codeInputs = listOf(
            includeOTP.findViewById(R.id.code1),
            includeOTP.findViewById(R.id.code2),
            includeOTP.findViewById(R.id.code3),
            includeOTP.findViewById(R.id.code4),
            includeOTP.findViewById(R.id.code5),
            includeOTP.findViewById(R.id.code6)
        )

        btnVerifyCode = includeOTP.findViewById(R.id.btnVerifyCode)

        setupOTPInputs()

        btnVerifyCode.setOnClickListener {
            val verificationCode = codeInputs.joinToString("") { it.text.toString() }

            if (verificationCode.length == 6) {
                if (verificationCode == serverCode) {
                    Toast.makeText(this, "Xác minh thành công!", Toast.LENGTH_SHORT).show()

                    passwordInput.visibility = View.VISIBLE
                    confirmPasswordInput.visibility = View.VISIBLE
                    btnSignUp.visibility = View.VISIBLE

                    includeOTP.visibility = View.GONE
                    btnSendCode.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Sai mã xác nhận!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nhập đủ 6 số xác minh nha!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendCode.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.emailInput)
            val email = emailInput.text.toString()

            if (email.isNotEmpty()) {
                // TODO: gọi API gửi mã xác nhận
                Toast.makeText(this, "Mã xác nhận đã được gửi về email!", Toast.LENGTH_SHORT).show()

                includeOTP.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnSignUp.setOnClickListener {
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                // TODO: gửi dữ liệu lên server
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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


}

