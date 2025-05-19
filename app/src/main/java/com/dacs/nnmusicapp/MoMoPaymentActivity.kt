package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MomoPaymentActivity : AppCompatActivity() {

    private lateinit var btnPayWithMomo: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_momo_payment)

        // Khởi tạo view
        btnPayWithMomo = findViewById(R.id.btnPayWithMomo)
        progressBar = findViewById(R.id.progressBar)
        requestQueue = Volley.newRequestQueue(this)

        // Xử lý sự kiện nhấn nút thanh toán
        btnPayWithMomo.setOnClickListener {
            processPayment()
        }

        Toast.makeText(this, "Momo Payment Activity", Toast.LENGTH_SHORT).show()
    }

    private fun processPayment() {
        progressBar.visibility = View.VISIBLE
        btnPayWithMomo.isEnabled = false

        // Thông tin thanh toán (thay bằng giá trị thực tế từ Momo Developer Portal)
        val amount = 50000L // 50,000 VND
        val orderId = "ORDER_${System.currentTimeMillis()}"
        val requestId = orderId
        val partnerCode = "MOMOAII20210519" // Thay bằng partnerCode thực tế
        val accessKey = "w9lZ7fV7rQ8kL3mN" // Thay bằng accessKey thực tế
        val secretKey = "your_secret_key" // Thay bằng secretKey thực tế
        val requestType = "payWithApp"
        val redirectUrl = "yourapp://callback" // Thay bằng scheme của ứng dụng
        val ipnUrl = "https://yourserver/ipn" // Thay bằng URL server của bạn

        // Tạo dữ liệu gửi đến API Momo
        val rawData = "partnerCode=$partnerCode&accessKey=$accessKey&requestId=$requestId&amount=$amount&orderId=$orderId" +
                "&orderInfo=Thanh toán gói VIP&redirectUrl=$redirectUrl&ipnUrl=$ipnUrl&requestType=$requestType"

        // Tính toán signature HMAC-SHA256
        val signature = calculateSignature(rawData, secretKey)

        // Tạo JSON request
        val json = JSONObject().apply {
            put("partnerCode", partnerCode)
            put("accessKey", accessKey)
            put("requestId", requestId)
            put("amount", amount)
            put("orderId", orderId)
            put("orderInfo", "Thanh toán gói VIP")
            put("redirectUrl", redirectUrl)
            put("ipnUrl", ipnUrl)
            put("requestType", requestType)
            put("signature", signature)
            put("extraData", "") // Thêm nếu cần
            put("lang", "vi") // Ngôn ngữ (vi hoặc en)
        }

        // Gọi API Momo (môi trường thử nghiệm)
        val momoApiUrl = "https://test-payment.momo.vn/v2/gateway/api/create"
        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST, momoApiUrl, json,
            { response ->
                progressBar.visibility = View.GONE
                btnPayWithMomo.isEnabled = true

                val payUrl = response.optString("payUrl", "")
                if (payUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(payUrl))
                    startActivityForResult(intent, REQUEST_CODE_PAYMENT)
                } else {
                    Toast.makeText(this, "Không nhận được link thanh toán: ${response.optString("message")}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                btnPayWithMomo.isEnabled = true
                Toast.makeText(this, "Lỗi gọi API Momo: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        requestQueue.add(jsonRequest)
    }

    // Hàm tính signature HMAC-SHA256
    private fun calculateSignature(rawData: String, secretKey: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKeySpec)
        val hash = sha256Hmac.doFinal(rawData.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP) // Momo yêu cầu encode base64
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        progressBar.visibility = View.GONE
        btnPayWithMomo.isEnabled = true

        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == RESULT_OK) {
                // Kiểm tra chi tiết từ callback (nếu có)
                data?.data?.let { uri ->
                    val result = uri.getQueryParameter("result")
                    if (result == "success") {
                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()

                        // Lưu trạng thái VIP vào SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().apply {
                            putBoolean("isVip", true)
                            apply()
                        }

                        // Quay lại MainActivity
                        finish()
                    } else {
                        Toast.makeText(this, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_LONG).show()
                    }
                } ?: run {
                    Toast.makeText(this, "Thanh toán thành công! (Mô phỏng)", Toast.LENGTH_LONG).show()
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("isVip", true)
                        apply()
                    }
                    finish()
                }
            } else {
                Toast.makeText(this, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_LONG).show()
            }
        }
    }



    companion object {
        private const val REQUEST_CODE_PAYMENT = 1001
    }
}