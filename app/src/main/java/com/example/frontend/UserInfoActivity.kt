package com.example.frontend

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class UserInfoActivity : AppCompatActivity() {

    private val REQUEST_GALLERY = 101
    private val REQUEST_CAMERA = 102
    private val REQUEST_EDIT_FIELD = 2

    private var selectedImageUri: Uri? = null
    private lateinit var email: String
    private lateinit var avatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userinfo)

        // Hiển thị email readonly
        val emailInput = findViewById<EditText>(R.id.emailInput)
        email = intent.getStringExtra("email") ?: ""
        emailInput.setText(email)
        emailInput.isEnabled = false

        // Avatar
        avatar = findViewById(R.id.avatarInSidebar)
        avatar.setOnClickListener { showAvatarOptions() }

        // Set label cho từng trường
        setLabel(R.id.usernameField, "Tên người dùng *")
        setLabel(R.id.nameField, "Tên *")
        setLabel(R.id.phoneField, "Số điện thoại *")
        setLabel(R.id.genderField, "Giới tính")
        setLabel(R.id.birthField, "Ngày sinh")
        setLabel(R.id.introField, "Giới thiệu bản thân")

        // Gán sự kiện click cho từng FrameLayout
        setupEditableField(R.id.usernameField, "Tên người dùng", "username")
        setupEditableField(R.id.nameField, "Tên", "name")
        setupEditableField(R.id.phoneField, "Số điện thoại", "phone")
        setupEditableField(R.id.genderField, "Giới tính", "gender")
        setupEditableField(R.id.birthField, "Ngày sinh", "birth")
        setupEditableField(R.id.introField, "Giới thiệu bản thân", "intro")
    }

    private fun showAvatarOptions() {
        val dialogView = layoutInflater.inflate(R.layout.layout_bottomsheet_avatar, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        dialogView.findViewById<TextView>(R.id.option_gallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_GALLERY)
            dialog.dismiss()
        }



        dialogView.findViewById<TextView>(R.id.option_camera).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.option_remove).setOnClickListener {
            avatar.setImageResource(R.drawable.default_avt)
            selectedImageUri = null
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_GALLERY -> {
                selectedImageUri = data?.data
                avatar.setImageURI(selectedImageUri)
            }

            REQUEST_CAMERA -> {
                val bitmap = data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    avatar.setImageBitmap(it)
                }
            }

            REQUEST_EDIT_FIELD -> {
                if (data != null) {
                    val fieldKey = data.getStringExtra("fieldKey")
                    val newValue = data.getStringExtra("newValue") ?: ""

                    when (fieldKey) {
                        "username" -> setInputValue(R.id.usernameField, newValue)
                        "name"     -> setInputValue(R.id.nameField, newValue)
                        "phone"    -> setInputValue(R.id.phoneField, newValue)
                        "gender"   -> setInputValue(R.id.genderField, newValue)
                        "birth"    -> setInputValue(R.id.birthField, newValue)
                        "intro"    -> setInputValue(R.id.introField, newValue)
                    }
                }
            }
        }
    }

    private fun setupEditableField(fieldId: Int, title: String, fieldKey: String) {
        val fieldLayout = findViewById<FrameLayout>(fieldId)
        fieldLayout.setOnClickListener {
            val currentValue = getInputValue(fieldId)
            val intent = Intent(this, EditFieldActivity::class.java).apply {
                putExtra("title", title)
                putExtra("fieldKey", fieldKey)
                putExtra("value", currentValue)
            }
            startActivityForResult(intent, REQUEST_EDIT_FIELD)
        }
    }

    private fun setLabel(fieldId: Int, labelText: String) {
        val view = findViewById<View>(fieldId)
        val label = view.findViewById<TextView>(R.id.label)
        label.text = labelText
    }

    private fun getInputValue(fieldId: Int): String {
        val view = findViewById<View>(fieldId)
        val input = view.findViewById<EditText>(R.id.inputField)
        return input.text.toString()
    }

    private fun setInputValue(fieldId: Int, value: String) {
        val view = findViewById<View>(fieldId)
        val input = view.findViewById<EditText>(R.id.inputField)
        input.setText(value)
    }
}
