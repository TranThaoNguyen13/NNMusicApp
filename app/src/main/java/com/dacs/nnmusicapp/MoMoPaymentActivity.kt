package com.dacs.nnmusicapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MomoPaymentActivity : AppCompatActivity() {

    private lateinit var btnPayWithMomo: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_momo_payment)

        // Khởi tạo view
        btnPayWithMomo = findViewById(R.id.btnPayWithMomo)
        progressBar = findViewById(R.id.progressBar)

        // Xử lý sự kiện nhấn nút thanh toán
        btnPayWithMomo.setOnClickListener {
            startSimulatedPayment()
        }

        Toast.makeText(this, "Momo Payment Activity", Toast.LENGTH_SHORT).show()
    }

    private fun startSimulatedPayment() {
        progressBar.visibility = View.VISIBLE
        btnPayWithMomo.isEnabled = false

        // Chuyển sang SimulatedPaymentActivity
        val intent = Intent(this, SimulatedPaymentActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_PAYMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        progressBar.visibility = View.GONE
        btnPayWithMomo.isEnabled = true

        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                // Kiểm tra kết quả từ SimulatedPaymentActivity
                val paymentResult = data?.getStringExtra("paymentResult")
                if (paymentResult == "success") {
                    Toast.makeText(this, "Thanh toán thành công! Tài khoản đã nâng cấp VIP.", Toast.LENGTH_LONG).show()

                    // Lưu trạng thái VIP vào SharedPreferences
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("isVip", true)
                        apply()
                    }

                    // Quay lại MainActivity
                    finish()
                }
            } else {
                Toast.makeText(this, "Thanh toán đã bị hủy", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PAYMENT = 1001
    }
}