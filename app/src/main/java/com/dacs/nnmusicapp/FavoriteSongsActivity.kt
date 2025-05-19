package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dacs.nnmusicapp.databinding.ActivityFavoriteSongsBinding
import org.json.JSONException
import org.json.JSONObject

class FavoriteSongsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoriteSongsBinding
    private lateinit var favoriteSongAdapter: SongAdapter
    private val favoriteSongs = mutableListOf<Song>()
    private lateinit var requestQueue: RequestQueue
    private var userId: String? = null
    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteSongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        userId = sharedPreferences.getString("user_id", "default_user")

        if (!isLoggedIn || userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem danh sách yêu thích", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)
        favoriteSongAdapter = SongAdapter(
            context = this,
            songs = favoriteSongs,
            favoriteSongs = favoriteSongs,
            onSongClick = { song ->
                if (isLoggedIn) {
                    navigateToSongActivity(song)
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            },
            onFavoriteClick = { song, isFavorite ->
                if (isLoggedIn) {
                    updateFavoriteOnServer(song, isFavorite)
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            },
            isManageMode = false,
            userId = userId
        )
        binding.rvFavoriteSongs.adapter = favoriteSongAdapter

        requestQueue = Volley.newRequestQueue(this)
        updateFavoriteSongsFromServer()


    }

    private fun updateFavoriteSongsFromServer() {
        val url = "${MainActivity.BASE_URL}/favorites/${userId ?: "default_user"}"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val favoritesArray = response.optJSONArray("favorites")
                favoriteSongs.clear()
                if (favoritesArray != null) {
                    for (i in 0 until favoritesArray.length()) {
                        val jsonObject = favoritesArray.getJSONObject(i)
                        val song = Song(
                            id = jsonObject.getJSONObject("song").getInt("id"),
                            title = jsonObject.getJSONObject("song").getString("title"),
                            artist = jsonObject.getJSONObject("song").getString("artist"),
                            file_path = jsonObject.getJSONObject("song").optString("file_path", null),
                            quality = jsonObject.getJSONObject("song").optString("quality", null),
                            trendingScore = jsonObject.getJSONObject("song").optInt("trending_score", 0),
                            isRecommended = jsonObject.getJSONObject("song").optInt("is_recommended", 0) == 1,
                            thumbnailUrl = jsonObject.getJSONObject("song").optString("thumbnail_url", null),
                            albumId = jsonObject.getJSONObject("song").optInt("album_id", 0),
                            lyrics = jsonObject.getJSONObject("song").optString("lyrics", null)
                        )
                        favoriteSongs.add(song)
                    }
                }
                favoriteSongAdapter.notifyDataSetChanged()
            },
            { error ->
                Log.e("FavoriteSongsActivity", "Error fetching favorites: ${error.message}")
                Log.e("FavoriteSongsActivity", "Network response: ${error.networkResponse?.statusCode}")
                Log.e("FavoriteSongsActivity", "Error details: ${error.networkResponse?.data?.let { String(it) }}")
                Toast.makeText(this, "Lỗi lấy danh sách yêu thích: ${error.message ?: "Không xác định"}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun navigateToSongActivity(song: Song) {
        Log.d("FavoriteSongsActivity", "Song URL before passing to SongActivity: ${song.file_path}")
        if (song.file_path.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, SongActivity::class.java).apply {
                putExtra("song_title", song.title)
                putExtra("song_artist", song.artist)
                putExtra("song_url", song.file_path)
                putExtra("song_thumbnail", song.thumbnailUrl)
                putExtra("song_lyrics", song.lyrics)
            }
            startActivity(intent)
        }
    }

    private fun updateFavoriteOnServer(song: Song, isFavorite: Boolean) {
        if (userId == null || song.id == null) {
            Log.e("FavoriteSongsActivity", "Invalid userId or songId: userId=$userId, songId=${song.id}")
            Toast.makeText(this, "Không thể cập nhật yêu thích: Dữ liệu không hợp lệ", Toast.LENGTH_LONG).show()
            return
        }

        val url = if (isFavorite) "${MainActivity.BASE_URL}/favorites/add" else "${MainActivity.BASE_URL}/favorites/remove"
        val requestBody = JSONObject().apply {
            put("user_id", userId)
            put("song_id", song.id)
        }

        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                Log.d("FavoriteSongsActivity", "Server response: $response")
                Toast.makeText(this, response.optString("message", "Cập nhật thành công"), Toast.LENGTH_SHORT).show()
                updateFavoriteSongsFromServer() // Làm mới danh sách sau khi cập nhật
            },
            { error ->
                Log.e("FavoriteSongsActivity", "Error updating favorite: ${error.message}")
                Log.e("FavoriteSongsActivity", "Network response: ${error.networkResponse?.statusCode}")
                Log.e("FavoriteSongsActivity", "Error details: ${error.networkResponse?.data?.let { String(it) }}")
                Toast.makeText(this, "Lỗi cập nhật yêu thích: ${error.message ?: "Không xác định"}", Toast.LENGTH_LONG).show()
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
}