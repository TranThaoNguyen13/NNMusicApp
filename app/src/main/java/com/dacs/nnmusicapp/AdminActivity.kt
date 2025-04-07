package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.dacs.nnmusicapp.admin.ManageUsersActivity
import com.dacs.nnmusicapp.admin.ManageSongsActivity
import com.dacs.nnmusicapp.admin.ManageAlbumsActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Trang Quản Trị Admin"

        // Hiển thị thông báo chào mừng
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Admin") ?: "Admin"
        Toast.makeText(this, "Chào mừng $username đến trang Admin", Toast.LENGTH_SHORT).show()

        // Ánh xạ các CardView
        val cardManageUsers = findViewById<CardView>(R.id.cardManageUsers)
        val cardManageSongs = findViewById<CardView>(R.id.cardManageSongs)
        val cardManageAlbums = findViewById<CardView>(R.id.cardManageAlbums)

        // Xử lý sự kiện click vào các CardView
        cardManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        cardManageSongs.setOnClickListener {
            startActivity(Intent(this, ManageSongsActivity::class.java))
        }

        cardManageAlbums.setOnClickListener {
            startActivity(Intent(this, ManageAlbumsActivity::class.java))
        }
    }

    // Thêm menu đăng xuất vào Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Đăng xuất
                val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putBoolean("isLoggedIn", false)
                    putString("role", "user")
                    apply()
                }
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}