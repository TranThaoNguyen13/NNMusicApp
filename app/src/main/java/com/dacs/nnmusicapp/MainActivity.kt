package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dacs.nnmusicapp.databinding.ActivityMainBinding
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL = "http://10.0.2.2:8000/api"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var btnAuth: ImageButton
    private lateinit var btnUpgradeVip: Button
    private lateinit var btnFavorite: ImageButton
    private lateinit var requestQueue: RequestQueue
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var trendingSongAdapter: SongAdapter
    private lateinit var favoriteSongAdapter: SongAdapter
    private var isLoggedIn = false
    private var isVip = false
    private var userId: String? = null
    private val allTrendingSongs = mutableListOf<Song>()
    private val displayedTrendingSongs = mutableListOf<Song>()
    private val favoriteSongs = mutableListOf<Song>()
    private val albums = mutableListOf<Album>()
    private var isExpanded = false

    private val sliderImages = listOf(
        "https://i.imgur.com/stW72UJ.png",
        "https://i.imgur.com/vGm99k8.png",
        "https://i.imgur.com/y7zP0U8.png"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        isVip = sharedPreferences.getBoolean("isVip", false)
        userId = sharedPreferences.getString("user_id", null)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnAuth = binding.btnAuth
        btnUpgradeVip = binding.btnUpgradeVip
        btnFavorite = binding.btnFavorite
        viewPager = binding.viewPager
        binding.rvAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecommendations.layoutManager = LinearLayoutManager(this)
        binding.rvTrendingSongs.layoutManager = LinearLayoutManager(this)
        binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(binding.toolbar)

        albumAdapter = AlbumAdapter(
            albums = albums,
            onAlbumClick = { album ->
                val intent = Intent(this, AlbumSongsActivity::class.java).apply {
                    putExtra("albumId", album.id)
                }
                startActivity(intent)
            }
        )
        binding.rvAlbums.adapter = albumAdapter

        trendingSongAdapter = SongAdapter(
            context = this,
            songs = displayedTrendingSongs,
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
            onDownloadClick = { song ->
                if (isVip) {
                    downloadSong(song)
                } else {
                    Toast.makeText(this, "Bạn cần nâng cấp VIP để tải bài hát", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MomoPaymentActivity::class.java))
                }
            },
            isManageMode = false,
            userId = userId,
            isVip = isVip
        )
        binding.rvTrendingSongs.adapter = trendingSongAdapter

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
            onDownloadClick = { song ->
                if (isVip) {
                    downloadSong(song)
                } else {
                    Toast.makeText(this, "Bạn cần nâng cấp VIP để tải bài hát", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MomoPaymentActivity::class.java))
                }
            },
            isManageMode = false,
            userId = userId,
            isVip = isVip
        )
        binding.rvFavoriteSongs.adapter = favoriteSongAdapter

        updateAuthButton()
        setupSlider()
        requestQueue = Volley.newRequestQueue(this)
        fetchAlbums()
        fetchTrendingSongs()
        fetchRecommendations()

        btnAuth.setOnClickListener {
            if (isLoggedIn) {
                sharedPreferences.edit().apply {
                    putBoolean("isLoggedIn", false)
                    putString("user_id", null)
                    apply()
                }
                isLoggedIn = false
                userId = null
                updateAuthButton()
                favoriteSongs.clear()
                favoriteSongAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        btnUpgradeVip.setOnClickListener {
            startActivity(Intent(this, MomoPaymentActivity::class.java))
        }

        btnFavorite.setOnClickListener {
            val intent = Intent(this, FavoriteSongsActivity::class.java)
            intent.putParcelableArrayListExtra("all_songs", ArrayList(allTrendingSongs))
            startActivity(intent)
        }

        binding.btnToggleViewMore.setOnClickListener {
            toggleTrendingSongs()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@MainActivity, "Tìm kiếm: $query", Toast.LENGTH_SHORT).show()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun updateAuthButton() {
        if (isLoggedIn) {
            btnAuth.setImageResource(R.drawable.ic_logout) // Sử dụng ic_logout
            btnUpgradeVip.visibility = View.VISIBLE
        } else {
            btnAuth.setImageResource(R.drawable.ic_login) // Sử dụng ic_login
            btnUpgradeVip.visibility = View.GONE
        }
    }

    private fun setupSlider() {
        val sliderAdapter = SliderAdapter(sliderImages)
        viewPager.adapter = sliderAdapter
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
    }

    private fun fetchAlbums() {
        val url = "$BASE_URL/albums"
        binding.progressBar?.visibility = View.VISIBLE
        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.progressBar?.visibility = View.GONE
                try {
                    albums.clear()
                    if (response.length() == 0) {
                        Toast.makeText(this, "Không có album nào", Toast.LENGTH_SHORT).show()
                        return@JsonArrayRequest
                    }
                    for (i in 0 until response.length()) {
                        val jsonObject = response.getJSONObject(i)
                        val album = Album(
                            id = jsonObject.getInt("id"),
                            title = jsonObject.getString("title"),
                            artist = jsonObject.optString("artist", null),
                            coverUrl = jsonObject.optString("cover_url", null)
                        )
                        albums.add(album)
                    }
                    albumAdapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    Log.e("MainActivity", "Error parsing albums: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu album: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                binding.progressBar?.visibility = View.GONE
                Log.e("MainActivity", "Error fetching albums: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API albums: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun fetchTrendingSongs() {
        val url = "$BASE_URL/songs/trending"
        binding.progressBar?.visibility = View.VISIBLE
        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.progressBar?.visibility = View.GONE
                try {
                    allTrendingSongs.clear()
                    for (i in 0 until response.length()) {
                        val jsonObject = response.getJSONObject(i)
                        val file_path = jsonObject.optString("file_path", null)
                        val adjustedFilepath = if (!file_path.isNullOrEmpty() && !file_path.startsWith("http")) {
                            "http://10.0.2.2:8000/$file_path"
                        } else {
                            file_path
                        }
                        val song = Song(
                            id = jsonObject.getInt("id"),
                            title = jsonObject.getString("title"),
                            artist = jsonObject.getString("artist"),
                            file_path = adjustedFilepath,
                            quality = jsonObject.optString("quality", null),
                            trendingScore = jsonObject.optInt("trending_score", 0),
                            isRecommended = jsonObject.optInt("is_recommended", 0) == 1,
                            thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                            albumId = jsonObject.optInt("album_id", 0),
                            lyrics = jsonObject.optString("lyrics", null)
                        )
                        allTrendingSongs.add(song)
                    }
                    allTrendingSongs.sortByDescending { it.trendingScore }
                    updateTrendingSongs()
                    if (isLoggedIn && userId != null) {
                        updateFavoriteSongsFromServer()
                    } else {
                        favoriteSongs.clear()
                        trendingSongAdapter.notifyDataSetChanged()
                        favoriteSongAdapter.notifyDataSetChanged()
                    }
                } catch (e: JSONException) {
                    Log.e("MainActivity", "Error parsing trending songs: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu trending: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                binding.progressBar?.visibility = View.GONE
                Log.e("MainActivity", "Error fetching trending songs: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API trending: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun fetchRecommendations() {
        val url = "$BASE_URL/songs/recommendations"
        binding.progressBar?.visibility = View.VISIBLE
        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.progressBar?.visibility = View.GONE
                try {
                    val songs = mutableListOf<Song>()
                    for (i in 0 until response.length()) {
                        val jsonObject = response.getJSONObject(i)
                        val file_path = jsonObject.optString("file_path", null)
                        val adjustedFilepath = if (!file_path.isNullOrEmpty() && !file_path.startsWith("http")) {
                            "http://10.0.2.2:8000/$file_path"
                        } else {
                            file_path
                        }
                        val song = Song(
                            id = jsonObject.getInt("id"),
                            title = jsonObject.getString("title"),
                            artist = jsonObject.getString("artist"),
                            file_path = adjustedFilepath,
                            quality = jsonObject.optString("quality", null),
                            trendingScore = jsonObject.optInt("trending_score", 0),
                            isRecommended = jsonObject.optInt("is_recommended", 0) == 1,
                            thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                            albumId = jsonObject.optInt("album_id", 0),
                            lyrics = jsonObject.optString("lyrics", null)
                        )
                        songs.add(song)
                    }
                    binding.rvRecommendations.adapter = SongAdapter(
                        context = this,
                        songs = songs,
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
                        onDownloadClick = { song ->
                            if (isVip) {
                                downloadSong(song)
                            } else {
                                Toast.makeText(this, "Bạn cần nâng cấp VIP để tải bài hát", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MomoPaymentActivity::class.java))
                            }
                        },
                        isManageMode = false,
                        userId = userId,
                        isVip = isVip
                    )
                } catch (e: JSONException) {
                    Log.e("MainActivity", "Error parsing recommendations: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu gợi ý: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                binding.progressBar?.visibility = View.GONE
                Log.e("MainActivity", "Error fetching recommendations: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API recommendations: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun updateFavoriteOnServer(song: Song, isFavorite: Boolean) {
        if (userId == null || song.id == null) {
            Log.e("MainActivity", "Invalid userId or songId: userId=$userId, songId=${song.id}")
            Toast.makeText(this, "Không thể cập nhật yêu thích: Dữ liệu không hợp lệ", Toast.LENGTH_LONG).show()
            return
        }

        val url = if (isFavorite) "$BASE_URL/favorites/add" else "$BASE_URL/favorites/remove"
        val requestBody = JSONObject().apply {
            put("user_id", userId)
            put("song_id", song.id)
        }

        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                Log.d("MainActivity", "Server response: $response")
                Toast.makeText(this, response.optString("message", "Cập nhật thành công"), Toast.LENGTH_SHORT).show()
                updateFavoriteSongsFromServer()
            },
            { error ->
                Log.e("MainActivity", "Error updating favorite: ${error.message}")
                Log.e("MainActivity", "Network response: ${error.networkResponse?.statusCode}")
                Log.e("MainActivity", "Error details: ${error.networkResponse?.data?.let { String(it) }}")
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

    private fun updateFavoriteSongsFromServer() {
        val url = "$BASE_URL/favorites/${userId ?: "default_user"}"
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
                trendingSongAdapter.notifyDataSetChanged() // Cập nhật trạng thái yêu thích trong danh sách trending
            },
            { error ->
                Log.e("MainActivity", "Error fetching favorites: ${error.message}")
                Toast.makeText(this, "Lỗi lấy danh sách yêu thích: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun navigateToSongActivity(song: Song) {
        Log.d("MainActivity", "Song URL before passing to SongActivity: ${song.file_path}")
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

    private fun downloadSong(song: Song) {
        Toast.makeText(this, "Đang tải bài hát: ${song.title}", Toast.LENGTH_SHORT).show()
        // Logic tải file (mô phỏng)
        Log.d("MainActivity", "Downloading song from: ${song.file_path}")
        // Bạn có thể dùng thư viện như DownloadManager để tải file thực tế
    }

    private fun updateTrendingSongs() {
        displayedTrendingSongs.clear()
        val displayCount = if (isExpanded) 10 else 3
        displayedTrendingSongs.addAll(allTrendingSongs.take(displayCount))
        trendingSongAdapter.notifyDataSetChanged()
        binding.btnToggleViewMore.setImageResource(
            if (isExpanded) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_more
        )
    }

    private fun toggleTrendingSongs() {
        isExpanded = !isExpanded
        updateTrendingSongs()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        isVip = sharedPreferences.getBoolean("isVip", false)
        userId = sharedPreferences.getString("user_id", null)
        updateAuthButton()
        if (isLoggedIn) {
            updateFavoriteSongsFromServer()
        } else {
            favoriteSongs.clear()
            favoriteSongAdapter.notifyDataSetChanged()
        }
    }
}