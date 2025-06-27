package com.example.frontend

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader.getFieldId
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class UserInfoActivity : AppCompatActivity() {

    private val REQUEST_GALLERY = 101
    private val REQUEST_CAMERA = 102
    private val REQUEST_EDIT_FIELD = 2

    private var selectedImageUri: Uri? = null
    private lateinit var email: String
    private lateinit var avatar: ImageView

    private lateinit var originalUserInfo: Map<String, String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userinfo)

        // Hiển thị email readonly
        val emailInput = findViewById<EditText>(R.id.emailInput)
        email = intent.getStringExtra("email") ?: ""
        emailInput.setText(email)
        emailInput.isEnabled = false
        loadUserInfo()

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


        val topAppBar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            handleBackPressed()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val requiredFields = listOf("username", "name", "phone")
            val missing = requiredFields.filter {
                getInputValue(getFieldId(it)).isBlank()
            }

            if (missing.isNotEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
            } else {
                saveUserInfo()
            }
        }


    }

    private fun getFieldId(key: String): Int {
        return when (key) {
            "username" -> R.id.usernameField
            "name" -> R.id.nameField
            "phone" -> R.id.phoneField
            else -> View.NO_ID
        }
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
        val inputView = fieldLayout.findViewById<TextView>(R.id.displayValue)
        inputView?.setOnClickListener {
            val currentValue = inputView.text.toString()
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
        val input = view.findViewById<TextView>(R.id.displayValue)

        if (input == null) {
            val fieldKey = when (fieldId) {
                R.id.usernameField -> "username"
                R.id.nameField     -> "name"
                R.id.phoneField    -> "phone"
                else               -> return ""
            }
            val title = when (fieldKey) {
                "username" -> "Tên người dùng"
                "name"     -> "Tên"
                "phone"    -> "Số điện thoại"
                else       -> "Chỉnh sửa"
            }

            val intent = Intent(this, EditFieldActivity::class.java).apply {
                putExtra("title", title)
                putExtra("fieldKey", fieldKey)
                putExtra("value", "")
            }
            startActivityForResult(intent, REQUEST_EDIT_FIELD)

            return "" // Trả về rỗng để không crash
        }

        return input.text.toString()
    }


    private fun setInputValue(fieldId: Int, value: String) {
        val view = findViewById<View>(fieldId)
        val input = view.findViewById<TextView>(R.id.displayValue)
        input.text = value
    }

    private fun handleBackPressed() {
        val requiredFields = listOf("username", "name", "phone")
        val missing = requiredFields.filter {
            getInputValue(getFieldId(it)).isBlank()
        }

        val isChanged = isUserInfoChanged()

        when {
            missing.isNotEmpty() -> {
                AlertDialog.Builder(this)
                    .setTitle("Thông báo")
                    .setMessage("Bạn chưa nhập đủ thông tin bắt buộc.\nBạn muốn tiếp tục nhập hay quay lại?")
                    .setNegativeButton("Tiếp tục", null)
                    .setPositiveButton("Quay lại") { _, _ ->
                        // Trường hợp từ MainActivity → finish()
                        // Trường hợp sau đăng ký → về LoginActivity hoặc finish()
                        navigateBack()
                    }
                    .show()
            }

            isChanged -> {
                AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn đã chỉnh sửa thông tin. Bạn muốn lưu lại trước khi quay lại?")
                    .setNegativeButton("Quay lại") { _, _ -> navigateBack() }
                    .setPositiveButton("Lưu") { _, _ ->
                        findViewById<Button>(R.id.btnSave).performClick()
                    }
                    .show()
            }

            else -> {
                navigateBack()
            }
        }
    }

    private fun navigateBack() {
        val isFromRegister = intent.getBooleanExtra("fromRegister", false)
        if (isFromRegister) {
            // Nếu là từ SignUpActivity → về lại Login (yêu cầu đăng nhập lại)
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Nếu là từ MainActivity → chỉ cần finish
            finish()
        }
    }

    private fun isUserInfoChanged(): Boolean {
        // Lấy các trường hiện tại
        val currentData = mapOf(
            "username" to getInputValue(R.id.usernameField),
            "name"     to getInputValue(R.id.nameField),
            "phone"    to getInputValue(R.id.phoneField),
            "gender"   to getInputValue(R.id.genderField),
            "birth"    to getInputValue(R.id.birthField),
            "intro"    to getInputValue(R.id.introField)
        )

        // So sánh với dữ liệu đã load từ Firestore
        val db = FirebaseFirestore.getInstance()
        var changed = false

        // ⚠️ NOTE: Hàm này async không thể dùng trực tiếp, bạn nên tạo biến `originalUserInfo` lúc load xong, rồi so sánh.
        // Giải pháp đơn giản hơn là lưu lại snapshot gốc như sau:

        return currentData != originalUserInfo // originalUserInfo là HashMap bạn lưu lại khi load
    }


    private fun saveUserInfo() {
        val username = getInputValue(R.id.usernameField)
        val name = getInputValue(R.id.nameField)
        val phone = getInputValue(R.id.phoneField)
        val gender = getInputValue(R.id.genderField)
        val birth = getInputValue(R.id.birthField)
        val intro = getInputValue(R.id.introField)

        val userMap = hashMapOf<String, Any>(
            "username" to username,
            "name" to name,
            "phone" to phone,
            "gender" to gender,
            "birth" to birth,
            "intro" to intro,
            "updatedAt" to System.currentTimeMillis()
        )

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("avatars/${email}.jpg")

            storageRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    userMap["avatarUrl"] = uri.toString()
                    saveToFirestore(userMap)
                }.addOnFailureListener {
                    Toast.makeText(this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Không có ảnh mới: vẫn lưu thông tin khác
            saveToFirestore(userMap)
        }
    }


    private fun saveToFirestore(userMap: HashMap<String, Any>) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(email)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_SHORT).show()

                AlertDialog.Builder(this)
                    .setTitle("Hoàn tất")
                    .setMessage("Bạn muốn đăng nhập tiếp hay thoát ứng dụng?")
                    .setPositiveButton("Đăng nhập") { _, _ ->
                        startActivity(Intent(this, MainActivity::class.java))
                        finishAffinity()
                    }
                    .setNegativeButton("Thoát") { _, _ -> finishAffinity() }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi lưu thông tin: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        val db = FirebaseFirestore.getInstance()


        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    setInputValue(R.id.usernameField, document.getString("username") ?: "")
                    setInputValue(R.id.nameField,     document.getString("name") ?: "")
                    setInputValue(R.id.phoneField,    document.getString("phone") ?: "")
                    setInputValue(R.id.genderField,   document.getString("gender") ?: "")
                    setInputValue(R.id.birthField,    document.getString("birth") ?: "")
                    setInputValue(R.id.introField,    document.getString("intro") ?: "")

                    val avatarUrl = document.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this).load(avatarUrl).into(avatar)
                    } else {
                        avatar.setImageResource(R.drawable.default_avt)
                    }

                    // ✅ Đặt trong đây mới access được biến `document`
                    originalUserInfo = mapOf(
                        "username" to (document.getString("username") ?: ""),
                        "name"     to (document.getString("name") ?: ""),
                        "phone"    to (document.getString("phone") ?: ""),
                        "gender"   to (document.getString("gender") ?: ""),
                        "birth"    to (document.getString("birth") ?: ""),
                        "intro"    to (document.getString("intro") ?: "")
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show()
            }

    }

}
