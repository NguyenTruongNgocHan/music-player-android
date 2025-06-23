package com.example.frontend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.frontend.databinding.EditFieldBinding

class EditFieldActivity : AppCompatActivity() {

    private lateinit var binding: EditFieldBinding
    private var fieldKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy dữ liệu từ Intent
        val title = intent.getStringExtra("title") ?: "Chỉnh sửa"
        val value = intent.getStringExtra("value") ?: ""
        fieldKey = intent.getStringExtra("fieldKey")

        // Setup toolbar
        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_done -> {
                    val resultIntent = Intent().apply {
                        putExtra("fieldKey", fieldKey)
                        putExtra("newValue", binding.editField.text.toString().trim())
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Hiển thị giá trị cũ
        binding.editField.setText(value)

        // Gợi ý mô tả tuỳ theo fieldKey
        when (fieldKey) {
            "name" -> {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = "Hãy lấy tên mà bạn thường dùng để tài khoản của bạn dễ tìm thấy hơn. Đó có thể là tên đầy đủ, biệt danh hoặc tên doanh nghiệp.\n\nBạn chỉ có thể đổi tên mình 2 lần trong vòng 14 ngày."
            }
            "username" -> {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = "Thông thường, bạn sẽ có thêm 14 ngày để đổi tên người dùng lại về tên cũ. \n\nBạn có thể chỉnh sửa tên người dùng tối đa 5 lần trong 30 phút."
            }
            else -> {
                binding.tvDescription.visibility = View.GONE
            }
        }
    }
}
