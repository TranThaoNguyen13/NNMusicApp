package com.dacs.nnmusicapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val tvAdminTitle = findViewById<TextView>(R.id.tvAdminTitle)
        val btnManageUsers = findViewById<Button>(R.id.btnManageUsers)

        // Thêm logic cho nút "Quản lý người dùng" nếu cần
        btnManageUsers.setOnClickListener {
            // Ví dụ: Chuyển hướng đến một màn hình quản lý người dùng
        }
    }
}