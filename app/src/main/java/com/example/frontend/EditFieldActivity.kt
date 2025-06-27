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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class EditFieldActivity : AppCompatActivity() {

    private lateinit var binding: EditFieldBinding
    private var fieldKey: String? = null
    private var selectedDate: String? = null
    private var checkJob: Job? = null
    private var lastInput: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nhận dữ liệu từ Intent
        val title = intent.getStringExtra("title") ?: "Chỉnh sửa"
        val value = intent.getStringExtra("value") ?: ""
        fieldKey = intent.getStringExtra("fieldKey")

        // Setup Toolbar
        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menu_done) {
                // Chặn lưu nếu username lỗi
                if (fieldKey == "username" && binding.editTextLayout.error != null) {
                    Toast.makeText(this, "Tên người dùng không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener true
                }

                val newValue = when (fieldKey) {
                    "gender" -> binding.spinnerGender.selectedItem.toString()
                    "birth" -> selectedDate ?: value
                    else -> binding.editField.text.toString().trim()
                }

                val resultIntent = Intent().apply {
                    putExtra("fieldKey", fieldKey)
                    putExtra("newValue", newValue)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
                true
            } else false
        }

        setupFieldView(fieldKey, value)
        setupFieldDescription(fieldKey)
    }

    private fun setupFieldView(fieldKey: String?, value: String) {
        when (fieldKey) {
            "gender" -> {
                showOnly(binding.spinnerGender)
                val options = listOf("Nam", "Nữ", "Khác")
                val adapter = ArrayAdapter(this, R.layout.spinner_gender, options)
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

                    val dp = DatePickerDialog(this, { _, y, m, d ->
                        selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                        binding.displayValue.text = selectedDate
                    }, year, month, day)
                    dp.show()
                }
            }

            else -> {
                showOnly(binding.editTextLayout)
                binding.editField.setText(value)

                // Xử lý riêng cho username
                if (fieldKey == "username") {
                    binding.editField.doAfterTextChanged {
                        val username = it.toString().trim()
                        lastInput = username
                        checkJob?.cancel()

                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.editTextLayout.error = null

                        checkJob = lifecycleScope.launch {
                            delay(500)
                            val isTaken = checkUsernameTaken(username)
                            binding.loadingIndicator.visibility = View.GONE

                            if (isTaken) {
                                binding.editTextLayout.error = "Tên người dùng đã tồn tại"
                            } else {
                                binding.editTextLayout.error = null
                            }
                        }
                    }
                }

                // Hint riêng cho intro
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
            "name" -> "Hãy lấy tên mà bạn thường dùng để tài khoản của bạn dễ tìm thấy hơn.\nBạn chỉ có thể đổi tên mình 2 lần trong vòng 14 ngày."
            "username" -> "Bạn có thể chỉnh sửa tên người dùng tối đa 5 lần trong 30 phút."
            "phone" -> "Số điện thoại sẽ được dùng để xác minh tài khoản và khôi phục mật khẩu."
            "intro" -> "Giới thiệu ngắn gọn để người khác biết bạn là ai (tối đa 150 ký tự)."
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
        delay(300) // giả lập call API
        val taken = listOf("hanna", "admin", "test123")
        return taken.contains(username.lowercase())
    }
}
