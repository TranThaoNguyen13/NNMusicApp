package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.nnmusicapp.databinding.ActivityFavoriteSongsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoriteSongsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoriteSongsBinding
    private lateinit var favoriteSongAdapter: SongAdapter
    private val favoriteSongs = mutableListOf<Song>()
    private val gson = Gson()
    private var userId: String? = null
    private var isLoggedIn = false
    private val allSongs = mutableListOf<Song>() // Lưu trữ tất cả bài hát để lọc danh sách yêu thích

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteSongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Thiết lập Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy userId và trạng thái đăng nhập từ SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        userId = sharedPreferences.getString("user_id", "default_user")

        // Kiểm tra đăng nhập
        if (!isLoggedIn) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem danh sách yêu thích", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Lấy danh sách tất cả bài hát từ Intent (truyền từ MainActivity)
        val songs: ArrayList<Song>? = intent.getParcelableArrayListExtra("all_songs")
        if (songs != null) {
            allSongs.clear()
            allSongs.addAll(songs)
        } else {
            Toast.makeText(this, "Không tìm thấy danh sách bài hát", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Thiết lập RecyclerView cho danh sách yêu thích
        binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)
        favoriteSongAdapter = SongAdapter(
            context = this@FavoriteSongsActivity,
            songs = favoriteSongs,
            onSongClick = { song ->
                if (song.filepath.isNullOrEmpty()) {
                    Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, SongActivity::class.java).apply {
                        putExtra("song_title", song.title)
                        putExtra("song_artist", song.artist)
                        putExtra("song_url", song.filepath)
                        putExtra("song_thumbnail", song.thumbnailUrl)
                        putExtra("song_lyrics", song.lyrics)
                    }
                    startActivity(intent)
                }
            },
            onFavoriteClick = { song, isFavorite ->
                Toast.makeText(
                    this,
                    if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
                updateFavoriteSongs()
            },
            isManageMode = false,
            userId = userId ?: "default_user"
        )
        binding.rvFavoriteSongs.adapter = favoriteSongAdapter

        // Lấy danh sách bài hát yêu thích
        updateFavoriteSongs()
    }

    private fun updateFavoriteSongs() {
        val sharedPreferences = getSharedPreferences("favorites_${userId}", MODE_PRIVATE)
        val favoritesJson = sharedPreferences.getString("favorite_songs", "[]")
        val type = object : TypeToken<MutableSet<Int>>() {}.type
        val favoriteSongIds: MutableSet<Int> = gson.fromJson(favoritesJson, type) ?: mutableSetOf()

        favoriteSongs.clear()
        favoriteSongs.addAll(allSongs.filter { favoriteSongIds.contains(it.id) })
        favoriteSongAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        updateFavoriteSongs() // Cập nhật danh sách yêu thích khi quay lại activity
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}