package com.dacs.nnmusicapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets; // Sửa import

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegisterLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Ánh xạ các thành phần
        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        // Xử lý nút "Đăng nhập"
        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(username, password)
        }

        // Xử lý nút "Đăng ký"
        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(username: String, password: String) {
        val url = "${MainActivity.BASE_URL}/login"
        val requestQueue = Volley.newRequestQueue(this)

        val requestBody = JSONObject().apply {
            put("name", username) // Sửa username thành name
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                try {
                    val status = response.getString("status")
                    if (status == "success") {
                        val role = response.getString("role")
                        val userId = response.getString("user_id")
                        Log.d("Login", "Success: role=$role, user_id=$userId")
                        Toast.makeText(this, "Đăng nhập thành công! Vai trò: $role", Toast.LENGTH_SHORT).show()

                        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().apply {
                            putBoolean("isLoggedIn", true)
                            putString("username", username)
                            putString("role", role)
                            putString("user_id", userId)
                            apply()
                        }

                        if (role == "admin") {
                            startActivity(Intent(this, AdminActivity::class.java))
                            finish()
                        } else {
                            finish()
                        }
                    } else {
                        val message = response.getString("message")
                        Log.d("Login", "Error: $message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Login", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                var errorMessage = "Lỗi kết nối đến server: ${error.message ?: "Không thể kết nối"}"
                if (error.networkResponse != null) {
                    try {
                        val serverError = String(error.networkResponse.data, StandardCharsets.UTF_8) // Sửa Charsets thành StandardCharsets
                        Log.e("Login", "Server error: $serverError")
                        errorMessage += "\nChi tiết: $serverError"
                    } catch (e: Exception) {
                        Log.e("Login", "Error parsing server response: ${e.message}")
                    }
                }
                Log.e("Login", errorMessage)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        requestQueue.add(request)
    }
}