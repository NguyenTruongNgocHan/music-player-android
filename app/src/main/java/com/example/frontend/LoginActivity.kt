package com.example.frontend

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnGuest: Button
    private lateinit var btnForgotPassword: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)


        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGuest = findViewById(R.id.btnGuest)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Nhập email và mật khẩu")
                return@setOnClickListener
            }

            loginWithFirebase(email, password)
        }


        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnGuest.setOnClickListener {
            startActivity(Intent(this, GuestActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun loginWithFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // ✅ Update thời gian đăng nhập
                val now = System.currentTimeMillis()
                db.collection("users").document(email)
                    .update("lastLogin", now)
                    .addOnSuccessListener {
                        // ✅ Sau khi update xong → kiểm tra thông tin để điều hướng
                        checkUserInfo(email)
                    }
                    .addOnFailureListener {
                        toast("Không cập nhật được thời gian đăng nhập")
                    }
            }
            .addOnFailureListener {
                toast("Sai email hoặc mật khẩu")
            }
    }




    private fun checkUserInfo(email: String) {
        val db = Firebase.firestore
        db.collection("users").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val username = doc.getString("username") ?: ""
                    val name = doc.getString("name") ?: ""
                    val phone = doc.getString("phone") ?: ""

                    val hasAllInfo = username.isNotBlank() && name.isNotBlank() && phone.isNotBlank()

                    val next = if (hasAllInfo) {
                        Intent(this, MainActivity::class.java)
                    } else {
                        Intent(this, UserInfoActivity::class.java).apply {
                            putExtra("email", email)
                        }
                    }

                    startActivity(next)
                    finish()
                } else {
                    toast("Không tìm thấy thông tin người dùng")
                }
            }
            .addOnFailureListener {
                toast("Lỗi khi lấy thông tin người dùng")
            }
    }

    private fun loadUserInfo(email: String) {
        val db = Firebase.firestore
        db.collection("users").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Lấy thêm role, avatar, playlist nếu cần
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    toast("Không tìm thấy thông tin người dùng")
                }
            }
            .addOnFailureListener {
                toast("Lỗi khi lấy thông tin người dùng")
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
