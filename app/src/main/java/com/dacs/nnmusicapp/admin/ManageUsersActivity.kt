package com.dacs.nnmusicapp.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dacs.nnmusicapp.R
import com.dacs.nnmusicapp.adapters.UserAdapter
import com.dacs.nnmusicapp.models.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Quản lý người dùng"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Xử lý nút quay lại
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Ánh xạ RecyclerView, TextView và FloatingActionButton
        rvUsers = findViewById(R.id.rvUsers)
        tvEmpty = findViewById(R.id.tvEmpty)
        val fabAddUser = findViewById<FloatingActionButton>(R.id.fabAddUser)

        // Thiết lập RecyclerView
        rvUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(users, { user -> showEditUserDialog(user) }, { user -> deleteUser(user) })
        rvUsers.adapter = userAdapter

        // Lấy danh sách người dùng
        fetchUsers()

        // Xử lý nút thêm người dùng
        fabAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun fetchUsers() {
        val url = "http://10.0.2.2/nnmusicapp_api/users.php?action=get_users"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("ManageUsersActivity", "API Response: $response")
                try {
                    val jsonArray = JSONArray(response)
                    users.clear()
                    for (i in 0 until jsonArray.length()) {
                        val userJson = jsonArray.getJSONObject(i)
                        val user = User(
                            id = userJson.getInt("id"),
                            username = userJson.getString("username"),
                            email = userJson.getString("email"),
                            role = userJson.getString("role")
                        )
                        users.add(user)
                    }
                    Log.d("ManageUsersActivity", "Parsed users: $users")
                    userAdapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                    if (users.isNotEmpty()) {
                        Log.d("ManageUsersActivity", "RecyclerView should display ${users.size} users")
                    }
                } catch (e: Exception) {
                    Log.e("ManageUsersActivity", "Error parsing response: ${e.message}")
                    try {
                        val jsonObject = JSONObject(response)
                        if (jsonObject.has("error")) {
                            Toast.makeText(this, "Lỗi từ API: ${jsonObject.getString("error")}", Toast.LENGTH_LONG).show()
                        } else if (jsonObject.has("status") && jsonObject.getString("status") == "error") {
                            Toast.makeText(this, "Lỗi từ API: ${jsonObject.getString("message")}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        tvEmpty.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Toast.makeText(this, "Lỗi không xác định: ${e.message}", Toast.LENGTH_LONG).show()
                        tvEmpty.visibility = View.VISIBLE
                    }
                }
            },
            { error ->
                Log.e("ManageUsersActivity", "Error fetching users: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API: ${error.message}", Toast.LENGTH_LONG).show()
                tvEmpty.visibility = View.VISIBLE
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Thêm người dùng")
            .setView(dialogView)
            .create()

        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addUser(username, email, password, role) {
                dialog.dismiss()
                fetchUsers()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Sửa người dùng")
            .setView(dialogView)
            .create()

        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        etUsername.setText(user.username)
        etEmail.setText(user.email)
        spinnerRole.setSelection(if (user.role == "admin") 0 else 1)

        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            if (username.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên người dùng và email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUser(user.id, username, email, password, role) {
                dialog.dismiss()
                fetchUsers()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addUser(username: String, email: String, password: String, role: String, onSuccess: () -> Unit) {
        val url = "http://10.0.2.2/nnmusicapp_api/users.php?action=add_user"
        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                val jsonObject = JSONObject(response)
                if (jsonObject.getString("status") == "success") {
                    Toast.makeText(this, "Thêm người dùng thành công", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ManageUsersActivity", "Error adding user: ${error.message}")
                Toast.makeText(this, "Error adding user: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray {
                val params = JSONObject().apply {
                    put("username", username)
                    put("email", email)
                    put("password", password)
                    put("role", role)
                }
                return params.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun updateUser(id: Int, username: String, email: String, password: String, role: String, onSuccess: () -> Unit) {
        val url = "http://10.0.2.2/nnmusicapp_api/users.php?action=update_user"
        val request = object : StringRequest(
            Method.PUT, url,
            { response ->
                val jsonObject = JSONObject(response)
                if (jsonObject.getString("status") == "success") {
                    Toast.makeText(this, "Cập nhật người dùng thành công", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ManageUsersActivity", "Error updating user: ${error.message}")
                Toast.makeText(this, "Error updating user: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray {
                val params = JSONObject().apply {
                    put("id", id)
                    put("username", username)
                    put("email", email)
                    if (password.isNotEmpty()) put("password", password)
                    put("role", role)
                }
                return params.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun deleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Xóa người dùng")
            .setMessage("Bạn có chắc chắn muốn xóa người dùng ${user.username}?")
            .setPositiveButton("Xóa") { _, _ ->
                val url = "http://10.0.2.2/nnmusicapp_api/users.php?action=delete_user&id=${user.id}"
                val request = StringRequest(
                    Request.Method.DELETE, url,
                    { response ->
                        val jsonObject = JSONObject(response)
                        if (jsonObject.getString("status") == "success") {
                            Toast.makeText(this, "Xóa người dùng thành công", Toast.LENGTH_SHORT).show()
                            fetchUsers()
                        } else {
                            Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error ->
                        Log.e("ManageUsersActivity", "Error deleting user: ${error.message}")
                        Toast.makeText(this, "Error deleting user: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
                Volley.newRequestQueue(this).add(request)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}