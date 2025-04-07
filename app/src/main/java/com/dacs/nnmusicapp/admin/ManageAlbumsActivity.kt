package com.dacs.nnmusicapp.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dacs.nnmusicapp.R

class ManageAlbumsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_albums)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Quản lý album"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Thêm nút quay lại

        // Xử lý nút quay lại
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}