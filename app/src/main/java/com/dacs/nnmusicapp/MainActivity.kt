package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var rvAlbums: RecyclerView
    private lateinit var rvRecommendations: RecyclerView
    private lateinit var btnAuth: Button
    private lateinit var requestQueue: RequestQueue
    private lateinit var albumAdapter: AlbumAdapter
    private var isLoggedIn = false

    private val sliderImages = listOf(
        "https://i.imgur.com/stW72UJ.png",
        "https://i.imgur.com/vGm99k8.png",
        "https://i.imgur.com/y7zP0U8.png"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Kiểm tra trạng thái đăng nhập
            val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

            // Ánh xạ các thành phần
            btnAuth = findViewById(R.id.btnAuth)
            viewPager = findViewById(R.id.viewPager)
            rvAlbums = findViewById(R.id.rvAlbums)
            rvRecommendations = findViewById(R.id.rvRecommendations)
            val searchView = findViewById<SearchView>(R.id.searchView)

            // Thiết lập Toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)

            // Cập nhật trạng thái nút đăng nhập/đăng xuất
            updateAuthButton()

            // Thiết lập ViewPager2 cho slider
            setupSlider()

            // Thiết lập RecyclerView cho album và gợi ý
            rvAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rvRecommendations.layoutManager = LinearLayoutManager(this)

            // Khởi tạo Volley RequestQueue
            requestQueue = Volley.newRequestQueue(this)

            // Gọi API
            fetchAlbums()
            fetchTopTrending()
            fetchRecommendations()

            // Xử lý nút đăng nhập/đăng xuất
            btnAuth.setOnClickListener {
                if (isLoggedIn) {
                    // Đăng xuất
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("isLoggedIn", false)
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
            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
                    rvAlbums.adapter = albumAdapter
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

    private fun fetchTopTrending() {
        val url = "http://10.0.2.2/nnmusicapp_api/api.php?action=top_trending"
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.length() > 0) {
                    try {
                        val jsonObject = response.getJSONObject(0)
                        val song = Song(
                            id = jsonObject.getInt("id"),
                            title = jsonObject.getString("title"),
                            artist = jsonObject.getString("artist"),
                            url = jsonObject.optString("url", null),
                            quality = jsonObject.optString("quality", null),
                            thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                            albumId = 0,
                            lyrics = jsonObject.optString("lyrics", null) // Thêm lyrics
                        )
                        findViewById<TextView>(R.id.tvTopTrendingTitleSong)?.text = song.title
                        findViewById<TextView>(R.id.tvTopTrendingArtist)?.text = song.artist
                        Glide.with(this)
                            .load(song.thumbnailUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(findViewById<ImageView>(R.id.ivTopTrendingThumbnail))

                        findViewById<ImageView>(R.id.ivTopTrendingThumbnail)?.setOnClickListener {
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
                                        putExtra("song_lyrics", song.lyrics) // Truyền lyrics
                                    }
                                    startActivity(intent)
                                }
                            } else {
                                Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Lỗi phân tích dữ liệu Top 1 Trending", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Không có dữ liệu Top 1 Trending", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Lỗi khi lấy Top 1 Trending: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(jsonArrayRequest)
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
                            thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                            albumId = 0,
                            lyrics = jsonObject.optString("lyrics", null) // Thêm lyrics
                        )
                        songs.add(song)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Lỗi phân tích dữ liệu gợi ý", Toast.LENGTH_SHORT).show()
                    }
                }
                rvRecommendations.adapter = SongAdapter(songs) { selectedSong ->
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
                                putExtra("song_lyrics", selectedSong.lyrics) // Truyền lyrics
                            }
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }
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
        updateAuthButton()
    }
}