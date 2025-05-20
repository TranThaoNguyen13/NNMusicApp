package com.dacs.nnmusicapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimulatedPaymentActivity : AppCompatActivity() {

    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulated_payment)

        // Khởi tạo Volley
        requestQueue = Volley.newRequestQueue(this)

        // Khởi tạo các nút
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        // Xử lý nút Hủy
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // Xử lý nút Xác nhận
        btnConfirm.setOnClickListener {
            saveOrderToApi()
        }
    }

    private fun saveOrderToApi() {
        // Lấy user_id và token từ SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)
        val token = sharedPreferences.getString("token", null)

        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // Lấy ngày hiện tại (order_date)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val orderDate = sdf.format(Date())

        // Tạo dữ liệu JSON để gửi
        val json = JSONObject().apply {
            put("total_amount", 50000)
            put("order_date", orderDate)
            put("payment_method", "momo")
            put("items", JSONArray().apply {
                put(JSONObject().apply {
                    put("item_type", "vip_package")
                    put("item_id", 1) // Giả định gói VIP có item_id = 1
                    put("quantity", 1)
                    put("price", 50000)
                })
            })
        }

        // Gọi API Laravel
        val apiUrl = "http://10.0.2.2:8000/api/orders"
        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST, apiUrl, json,
            { response ->
                Log.d("SimulatedPayment", "Order created: ${response.toString()}")
                Toast.makeText(this, "Thanh toán thành công! Đơn hàng đã được lưu.", Toast.LENGTH_LONG).show()

                // Trả về kết quả thành công
                val resultIntent = Intent()
                resultIntent.putExtra("paymentResult", "success")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            },
            { error ->
                Log.e("SimulatedPayment", "Error creating order: ${error.message}")
                Toast.makeText(this, "Lỗi khi lưu đơn hàng: ${error.message}", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        requestQueue.add(jsonRequest)
    }
}