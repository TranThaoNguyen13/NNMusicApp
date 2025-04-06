package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UpgradeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade)

        // Khi nhấn nút "Nâng cấp", mở màn hình thanh toán MoMo
        findViewById<Button>(R.id.btnUpgrade).setOnClickListener {
            startActivity(Intent(this, MoMoPaymentActivity::class.java))
        }
    }
}
