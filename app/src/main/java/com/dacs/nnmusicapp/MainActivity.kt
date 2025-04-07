package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dacs.nnmusicapp.AlbumAdapter
import com.dacs.nnmusicapp.SongAdapter
import com.dacs.nnmusicapp.SliderAdapter
import com.dacs.nnmusicapp.adapters.TrendingSongAdapter
import com.dacs.nnmusicapp.databinding.ActivityMainBinding
import com.dacs.nnmusicapp.Album
import com.dacs.nnmusicapp.Song
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var btnAuth: Button
    private lateinit var requestQueue: RequestQueue
    private lateinit var albumAdapter: AlbumAdapter
    private var isLoggedIn = false
    private val trendingSongs = mutableListOf<Song>()
    private lateinit var trendingSongAdapter: TrendingSongAdapter

    private val sliderImages = listOf(
        "https://i.imgur.com/stW72UJ.png",
        "https://i.imgur.com/vGm99k8.png",
        "https://i.imgur.com/y7zP0U8.png"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra vai trò ngay khi khởi động
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val role = sharedPreferences.getString("role", "user") // Mặc định là "user" nếu không có role

        // Nếu là admin, chuyển hướng đến AdminActivity
        if (isLoggedIn && role == "admin") {
            startActivity(Intent(this, AdminActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Ánh xạ các thành phần
            btnAuth = binding.btnAuth
            viewPager = binding.viewPager
            binding.rvAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvRecommendations.layoutManager = LinearLayoutManager(this)

            // Thiết lập RecyclerView cho danh sách trending
            binding.rvTrendingSongs.layoutManager = LinearLayoutManager(this)
            trendingSongAdapter = TrendingSongAdapter(trendingSongs) { song ->
                if (isLoggedIn) {
                    Log.d("MainActivity", "Song URL before passing to SongActivity: ${song.url}")
                    if (song.url.isNullOrEmpty()) {
                        Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, SongActivity::class.java).apply {
                            putExtra("song_title", song.title)
                            putExtra("song_artist", song.artist)
                            putExtra("song_url", song.url)
                            putExtra("song_thumbnail", song.thumbnailUrl)
                            putExtra("song_lyrics", song.lyrics)
                        }
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            binding.rvTrendingSongs.adapter = trendingSongAdapter

            // Thiết lập Toolbar
            setSupportActionBar(binding.toolbar)

            // Cập nhật trạng thái nút đăng nhập/đăng xuất
            updateAuthButton()

            // Thiết lập ViewPager2 cho slider
            setupSlider()

            // Khởi tạo Volley RequestQueue
            requestQueue = Volley.newRequestQueue(this)

            // Gọi API
            fetchAlbums()
            fetchTrendingSongs()
            fetchRecommendations()

            // Xử lý nút đăng nhập/đăng xuất
            btnAuth.setOnClickListener {
                if (isLoggedIn) {
                    // Đăng xuất
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("isLoggedIn", false)
                        putString("role", "user") // Reset role về user
                        apply()
                    }
                    isLoggedIn = false
                    updateAuthButton()
                    Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                } else {
                    // Đăng nhập
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }

            // Xử lý SearchView
            binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Toast.makeText(this@MainActivity, "Tìm kiếm: $query", Toast.LENGTH_SHORT).show()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khởi tạo MainActivity: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateAuthButton() {
        if (isLoggedIn) {
            btnAuth.text = "Đăng xuất"
        } else {
            btnAuth.text = "Đăng nhập"
        }
    }

    private fun setupSlider() {
        try {
            val sliderAdapter = SliderAdapter(sliderImages)
            viewPager.adapter = sliderAdapter

            // Tự động cuộn sau 3 giây
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    val currentItem = viewPager.currentItem
                    val nextItem = if (currentItem == sliderImages.size - 1) 0 else currentItem + 1
                    viewPager.setCurrentItem(nextItem, true)
                    handler.postDelayed(this, 3000)
                }
            }
            handler.postDelayed(runnable, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi thiết lập slider: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchAlbums() {
        val url = "http://10.0.2.2/nnmusicapp_api/api.php?action=albums"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                val albums = mutableListOf<Album>()
                try {
                    for (i in 0 until response.length()) {
                        val albumJson = response.getJSONObject(i)
                        val album = Album(
                            id = albumJson.getInt("id"),
                            title = albumJson.getString("title"),
                            artist = albumJson.optString("artist", null),
                            coverUrl = albumJson.optString("cover_url", null)
                        )
                        albums.add(album)
                    }

                    albumAdapter = AlbumAdapter(albums) { album ->
                        val intent = Intent(this, AlbumSongsActivity::class.java).apply {
                            putExtra("albumId", album.id)
                        }
                        startActivity(intent)
                    }
                    binding.rvAlbums.adapter = albumAdapter
                } catch (e: JSONException) {
                    Log.e("MainActivity", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Error parsing albums", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("MainActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error fetching albums: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(request)
    }

    private fun fetchTrendingSongs() {
        val url = "http://10.0.2.2/nnmusicapp_api/songs.php?action=get_trending_songs"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("MainActivity", "API Response: $response")
                try {
                    if (response.trim().startsWith("[")) {
                        val jsonArray = org.json.JSONArray(response)
                        trendingSongs.clear()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val song = Song(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.getString("artist"),
                                url = jsonObject.optString("url", null),
                                quality = jsonObject.optString("quality", null),
                                trendingScore = if (jsonObject.isNull("trending_score")) null else jsonObject.optInt("trending_score", 0),
                                isRecommended = if (jsonObject.isNull("is_recommended")) null else jsonObject.optInt("is_recommended", 0) == 1,
                                thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                                albumId = if (jsonObject.isNull("album_id")) null else jsonObject.optInt("album_id", 0),
                                lyrics = jsonObject.optString("lyrics", null)
                            )
                            trendingSongs.add(song)
                        }
                        trendingSongAdapter.notifyDataSetChanged()
                    } else {
                        val jsonObject = JSONObject(response)
                        if (jsonObject.has("status") && jsonObject.getString("status") == "error") {
                            Toast.makeText(this, "Lỗi từ API: ${jsonObject.getString("message")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("MainActivity", "Error fetching trending songs: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun fetchRecommendations() {
        val url = "http://10.0.2.2/nnmusicapp_api/api.php?action=recommendations"
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                val songs = mutableListOf<Song>()
                for (i in 0 until response.length()) {
                    try {
                        val jsonObject = response.getJSONObject(i)
                        val song = Song(
                            id = jsonObject.getInt("id"),
                            title = jsonObject.getString("title"),
                            artist = jsonObject.getString("artist"),
                            url = jsonObject.optString("url", null),
                            quality = jsonObject.optString("quality", null),
                            trendingScore = if (jsonObject.isNull("trending_score")) null else jsonObject.optInt("trending_score", 0),
                            isRecommended = if (jsonObject.isNull("is_recommended")) null else jsonObject.optInt("is_recommended", 0) == 1,
                            thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                            albumId = if (jsonObject.isNull("album_id")) null else jsonObject.optInt("album_id", 0),
                            lyrics = jsonObject.optString("lyrics", null)
                        )
                        songs.add(song)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Lỗi phân tích dữ liệu gợi ý", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.rvRecommendations.adapter = SongAdapter(
                    songs = songs,
                    onSongClick = { selectedSong ->
                        if (isLoggedIn) {
                            Log.d("MainActivity", "Song URL before passing to SongActivity: ${selectedSong.url}")
                            if (selectedSong.url.isNullOrEmpty()) {
                                Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(this, SongActivity::class.java).apply {
                                    putExtra("song_title", selectedSong.title)
                                    putExtra("song_artist", selectedSong.artist)
                                    putExtra("song_url", selectedSong.url)
                                    putExtra("song_thumbnail", selectedSong.thumbnailUrl)
                                    putExtra("song_lyrics", selectedSong.lyrics)
                                }
                                startActivity(intent)
                            }
                        } else {
                            Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    },
                    onEditClick = { /* Không cần xử lý chỉnh sửa trong MainActivity */ },
                    onDeleteClick = { /* Không cần xử lý xóa trong MainActivity */ },
                    isManageMode = false
                )
            },
            { error ->
                Toast.makeText(this, "Lỗi khi lấy gợi ý bài hát: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(jsonArrayRequest)
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val role = sharedPreferences.getString("role", "user")
        if (isLoggedIn && role == "admin") {
            startActivity(Intent(this, AdminActivity::class.java))
            finish()
            return
        }
        updateAuthButton()
    }
}