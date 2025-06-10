package com.example.frontend

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class UserInfoActivity : AppCompatActivity() {

    private lateinit var avatarView: ImageView
    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var nameInput: TextInputEditText
    private lateinit var genderInput: TextInputEditText
    private lateinit var birthdateInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var bioInput: TextInputEditText
    private lateinit var btnSave: Button

    private val IMAGE_PICK_CODE = 1001
    private var avatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userinfo)

        // Ánh xạ view
        avatarView = findViewById(R.id.avatarInSidebar)
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        nameInput = findViewById(R.id.nameInput)
        genderInput = findViewById(R.id.genderInput)
        birthdateInput = findViewById(R.id.birthdateInput)
        phoneInput = findViewById(R.id.phoneInput)
        bioInput = findViewById(R.id.bioInput)
        btnSave = findViewById(R.id.btnSave)

        // Chọn ảnh avatar
        avatarView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        // Lưu thông tin
        btnSave.setOnClickListener {
            val username = usernameInput.text.toString()
            val name = nameInput.text.toString()
            val gender = genderInput.text.toString()
            val birthdate = birthdateInput.text.toString()
            val phone = phoneInput.text.toString()
            val bio = bioInput.text.toString()
            val email = emailInput.text.toString()

            Toast.makeText(this, "Đã lưu thông tin!", Toast.LENGTH_SHORT).show()
        }

    }

    // Nhận kết quả chọn ảnh
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            avatarUri = data?.data
            avatarView.setImageURI(avatarUri)
        }
    }
}
