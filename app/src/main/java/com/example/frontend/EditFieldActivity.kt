package com.example.frontend

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.frontend.databinding.EditFieldBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class EditFieldActivity : AppCompatActivity() {

    private lateinit var binding: EditFieldBinding
    private var fieldKey: String? = null
    private var selectedDate: String? = null
    private var checkJob: Job? = null
    private val email: String by lazy { FirebaseAuth.getInstance().currentUser?.email.orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fieldKey = intent.getStringExtra("fieldKey")
        val title = intent.getStringExtra("title") ?: "Chỉnh sửa"
        val value = intent.getStringExtra("value") ?: ""

        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menu_done) {
                saveChanges(value)
                true
            } else false
        }

        setupFieldView(fieldKey, value)
        setupFieldDescription(fieldKey)
    }

    private fun saveChanges(originalValue: String) {
        if (fieldKey == "username" && binding.editTextLayout.error != null) {
            toast("Tên người dùng không hợp lệ")
            return
        }

        lifecycleScope.launch {
            val newValue = when (fieldKey) {
                "gender" -> binding.spinnerGender.selectedItem.toString()
                "birth" -> selectedDate ?: originalValue
                else -> binding.editField.text.toString().trim()
            }

            try {
                if (fieldKey == "username" && !canChangeUsername(email)) {
                    toast("Bạn chỉ được đổi username tối đa 5 lần trong 30 phút.")
                    return@launch
                }
                if (fieldKey == "name" && !canChangeName(email)) {
                    toast("Bạn chỉ được đổi tên tối đa 2 lần trong 14 ngày.")
                    return@launch
                }

                updateChangeLog(email, fieldKey)
                val resultIntent = Intent().apply {
                    putExtra("fieldKey", fieldKey)
                    putExtra("newValue", newValue)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                toast("Lỗi lưu dữ liệu: ${e.message}")
            }
        }
    }

    private fun setupFieldView(fieldKey: String?, value: String) {
        when (fieldKey) {
            "gender" -> {
                showOnly(binding.spinnerGender)
                val options = listOf("Nam", "Nữ", "Khác")
                val adapter = ArrayAdapter(this, R.layout.spinner_gender, options)
                adapter.setDropDownViewResource(R.layout.spinner_gender)
                binding.spinnerGender.adapter = adapter
                val index = options.indexOfFirst { it.equals(value, ignoreCase = true) }
                if (index != -1) binding.spinnerGender.setSelection(index)
            }
            "birth" -> {
                showOnly(binding.datePickerField)
                binding.displayValue.text = value
                selectedDate = value
                binding.clickableField.setOnClickListener {
                    val parts = value.split("-")
                    val year = parts.getOrNull(0)?.toIntOrNull() ?: 2000
                    val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
                    val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
                    DatePickerDialog(this, { _, y, m, d ->
                        selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                        binding.displayValue.text = selectedDate
                    }, year, month, day).show()
                }
            }
            else -> {
                showOnly(binding.editTextLayout)
                binding.editField.setText(value)
                if (fieldKey == "username") {
                    binding.editField.doAfterTextChanged {
                        val username = it.toString().trim()
                        checkJob?.cancel()
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.editTextLayout.error = null
                        checkJob = lifecycleScope.launch {
                            delay(500)
                            binding.loadingIndicator.visibility = View.GONE
                            val isTaken = checkUsernameTaken(username)
                            binding.editTextLayout.error = if (isTaken) "Tên người dùng đã tồn tại" else null
                        }
                    }
                }
                if (fieldKey == "intro") {
                    binding.editField.hint = "Giới thiệu ngắn gọn về bạn..."
                    binding.editField.maxLines = 5
                }
            }
        }
    }

    private fun setupFieldDescription(fieldKey: String?) {
        binding.tvDescription.visibility = View.VISIBLE
        binding.tvDescription.text = when (fieldKey) {
            "name" -> "Bạn chỉ có thể đổi tên 2 lần trong 14 ngày."
            "username" -> "Bạn có thể chỉnh sửa username tối đa 5 lần trong 30 phút."
            "phone" -> "Dùng để xác minh và khôi phục mật khẩu."
            "intro" -> "Giới thiệu ngắn để người khác biết bạn là ai."
            "birth", "gender" -> "Nhập thông tin để nâng cấp trải nghiệm người dùng."
            else -> {
                binding.tvDescription.visibility = View.GONE
                ""
            }
        }
    }

    private fun showOnly(visibleView: View) {
        binding.editTextLayout.visibility = if (visibleView == binding.editTextLayout) View.VISIBLE else View.GONE
        binding.spinnerGender.visibility = if (visibleView == binding.spinnerGender) View.VISIBLE else View.GONE
        binding.datePickerField.visibility = if (visibleView == binding.datePickerField) View.VISIBLE else View.GONE
    }

    private suspend fun checkUsernameTaken(username: String): Boolean {
        return try {
            val db = Firebase.firestore
            val result = db.collection("users").whereEqualTo("username", username).get().await()
            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun canChangeUsername(email: String): Boolean {
        val db = Firebase.firestore
        val doc = db.collection("users").document(email).get().await()
        val logs = doc.get("usernameChangeLog") as? List<Long> ?: emptyList()
        val now = System.currentTimeMillis()
        return logs.count { it >= now - 30 * 60 * 1000 } < 5
    }

    private suspend fun canChangeName(email: String): Boolean {
        val db = Firebase.firestore
        val doc = db.collection("users").document(email).get().await()
        val logs = doc.get("nameChangeLog") as? List<Long> ?: emptyList()
        val now = System.currentTimeMillis()
        return logs.count { it >= now - 14 * 24 * 60 * 60 * 1000 } < 2
    }

    private fun updateChangeLog(email: String, field: String?) {
        val db = Firebase.firestore
        val logField = when (field) {
            "username" -> "usernameChangeLog"
            "name" -> "nameChangeLog"
            else -> return
        }
        db.collection("users").document(email)
            .update(logField, FieldValue.arrayUnion(System.currentTimeMillis()))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        checkJob?.cancel()
    }
}
