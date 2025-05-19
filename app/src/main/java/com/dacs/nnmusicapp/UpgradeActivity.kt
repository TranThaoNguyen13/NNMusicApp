package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UpgradeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade) // Đảm bảo layout tồn tại

        // Ví dụ: Chuyển đến MomoPaymentActivity
        val upgradeButton = findViewById<Button>(R.id.btnUpgrade) // Thay bằng ID thực tế
        upgradeButton.setOnClickListener {
            val intent = Intent(this, MomoPaymentActivity::class.java)
            startActivity(intent)
        }

        Toast.makeText(this, "Upgrade Activity", Toast.LENGTH_SHORT).show()
    }
}