package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.json.JSONException
import java.nio.charset.StandardCharsets

class RegisterActivity : AppCompatActivity() {
    lateinit var edtUsername: EditText
    lateinit var edtEmail: EditText
    lateinit var edtPassword: EditText
    lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            register(username, email, password)
        }
    }

    private fun register(username: String, email: String, password: String) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${MainActivity.BASE_URL}/register" // Đảm bảo URL đúng
        val requestQueue = Volley.newRequestQueue(this)

        Log.d("Register", "Sending request to URL: $url")
        Log.d("Register", "Sending: name=$username, email=$email, password=$password")

        val requestBody = JSONObject().apply {
            put("name", username)
            put("email", email)
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, requestBody,
            { response ->
                Log.d("Register", "Response: $response")
                try {
                    val status = response.getString("status")
                    if (status == "success") {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        val message = response.getString("message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    Log.e("Register", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("Register", "Volley error: ${error.message ?: "Unknown error"}")
                Log.e("Register", "Network response: ${error.networkResponse?.statusCode}")
                Log.e("Register", "Error details: ${error.toString()}")
                var errorMessage = "Không thể kết nối đến server"
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        val serverError = String(error.networkResponse.data, StandardCharsets.UTF_8)
                        Log.e("Register", "Server error message: $serverError")
                        errorMessage = if (serverError.contains("404")) "Endpoint không tồn tại (404)" else serverError
                        errorMessage = errorMessage.substring(0, minOf(errorMessage.length, 100))
                    } catch (e: Exception) {
                        Log.e("Register", "Error parsing server response: ${e.message}")
                        errorMessage = "Lỗi phân tích phản hồi server"
                    }
                } else {
                    errorMessage = "Không thể kết nối đến server. Kiểm tra server hoặc mạng."
                }
                Toast.makeText(this, "Lỗi: $errorMessage", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }

        requestQueue.add(request)
    }
}