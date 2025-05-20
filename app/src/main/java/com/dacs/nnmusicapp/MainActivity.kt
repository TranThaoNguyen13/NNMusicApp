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
    private lateinit var requestQueue: com.android.volley.RequestQueue

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var trendingSongAdapter: SongAdapter
    private lateinit var favoriteSongAdapter: SongAdapter

    private var isLoggedIn = false
    private var isVip = false
    private var userId: String? = null

    private val allTrendingSongs = mutableListOf<Song>()
    private val filteredTrendingSongs = mutableListOf<Song>()
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

        // Load login/vip/user info từ SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        isVip = sharedPreferences.getBoolean("isVip", false)
        userId = sharedPreferences.getString("user_id", null)

        // Inflate layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init RequestQueue Volley
        requestQueue = Volley.newRequestQueue(this)

        // Setup RecyclerView LayoutManagers
        binding.rvAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecommendations.layoutManager = LinearLayoutManager(this)
        binding.rvTrendingSongs.layoutManager = LinearLayoutManager(this)
        binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)

        // Setup Album Adapter
        albumAdapter = AlbumAdapter(albums) { album ->
            val intent = Intent(this, AlbumSongsActivity::class.java).apply {
                putExtra("albumId", album.id)
            }
            startActivity(intent)
        }
        binding.rvAlbums.adapter = albumAdapter

        // Setup Trending Songs Adapter
        trendingSongAdapter = SongAdapter(
            context = this,
            songs = filteredTrendingSongs,
            favoriteSongs = favoriteSongs,
            onSongClick = ::handleSongClick,
            onFavoriteClick = ::handleFavoriteClick,
            onDownloadClick = ::handleDownloadClick,
            isManageMode = false,
            userId = userId,
            isVip = isVip
        )
        binding.rvTrendingSongs.adapter = trendingSongAdapter

        // Setup Favorite Songs Adapter
        favoriteSongAdapter = SongAdapter(
            context = this,
            songs = favoriteSongs,
            favoriteSongs = favoriteSongs,
            onSongClick = ::handleSongClick,
            onFavoriteClick = ::handleFavoriteClick,
            onDownloadClick = ::handleDownloadClick,
            isManageMode = false,
            userId = userId,
            isVip = isVip
        )
        binding.rvFavoriteSongs.adapter = favoriteSongAdapter

        // UI update for login status
        updateAuthButton()
        setupSlider()

        // Fetch data from API
        fetchAlbums()
        fetchTrendingSongs()
        fetchRecommendations()

        // Button Listeners
        binding.btnAuth.setOnClickListener {
            if (isLoggedIn) {
                logout(sharedPreferences)
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        binding.btnUpgradeVip.setOnClickListener {
            startActivity(Intent(this, MomoPaymentActivity::class.java))
        }

        binding.btnFavorite.setOnClickListener {
            val intent = Intent(this, FavoriteSongsActivity::class.java).apply {
                putParcelableArrayListExtra("all_songs", ArrayList(allTrendingSongs))
                putParcelableArrayListExtra("all_albums", ArrayList(albums))
            }
            startActivity(intent)
        }

        binding.btnToggleViewMore.setOnClickListener {
            isExpanded = !isExpanded
            updateTrendingSongs()
        }

        // SearchView: Tìm kiếm trên nút Submit (không realtime lọc)
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()) {
                        val intent = Intent(this@MainActivity, SearchResultsActivity::class.java).apply {
                            putExtra("search_query", it.trim())
                            putParcelableArrayListExtra("all_songs", ArrayList(allTrendingSongs))
                            putParcelableArrayListExtra("all_albums", ArrayList(albums))
                        }
                        startActivity(intent)
                    }
                }
                binding.searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
    }

    // Xử lý khi nhấn bài hát
    private fun handleSongClick(song: Song) {
        if (isLoggedIn) {
            navigateToSongActivity(song)
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // Xử lý khi nhấn yêu thích
    private fun handleFavoriteClick(song: Song, isFavorite: Boolean) {
        if (isLoggedIn) {
            updateFavoriteOnServer(song, isFavorite)
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    // Xử lý khi nhấn tải về
    private fun handleDownloadClick(song: Song) {
        if (isVip) {
            downloadSong(song)
        } else {
            Toast.makeText(this, "Bạn cần nâng cấp VIP để tải bài hát", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MomoPaymentActivity::class.java))
        }
    }

    private fun logout(sharedPreferences: android.content.SharedPreferences) {
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
    }

    private fun updateAuthButton() {
        if (isLoggedIn) {
            binding.btnAuth.setImageResource(R.drawable.ic_logout)
            binding.btnUpgradeVip.visibility = View.VISIBLE
        } else {
            binding.btnAuth.setImageResource(R.drawable.ic_login)
            binding.btnUpgradeVip.visibility = View.GONE
        }
    }

    private fun setupSlider() {
        val sliderAdapter = SliderAdapter(sliderImages)
        binding.viewPager.adapter = sliderAdapter
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = binding.viewPager.currentItem
                val nextItem = if (currentItem == sliderImages.size - 1) 0 else currentItem + 1
                binding.viewPager.setCurrentItem(nextItem, true)
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
                        albums.add(
                            Album(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.optString("artist", null),
                                coverUrl = jsonObject.optString("cover_url", null)
                            )
                        )
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
                        val filePathRaw = jsonObject.optString("file_path", null)
                        val filePath = if (!filePathRaw.isNullOrEmpty() && !filePathRaw.startsWith("http")) {
                            "http://10.0.2.2:8000/$filePathRaw"
                        } else {
                            filePathRaw
                        }
                        allTrendingSongs.add(
                            Song(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.getString("artist"),
                                file_path = filePath,
                                quality = jsonObject.optString("quality", null),
                                trendingScore = jsonObject.optInt("trending_score", 0),
                                isRecommended = jsonObject.optInt("is_recommended", 0) == 1,
                                thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                                albumId = jsonObject.optInt("album_id", 0),
                                lyrics = jsonObject.optString("lyrics", null)
                            )
                        )
                    }
                    allTrendingSongs.sortByDescending { it.trendingScore }
                    isExpanded = false
                    updateTrendingSongs()
                    if (isLoggedIn && userId != null) {
                        updateFavoriteSongsFromServer()
                    } else {
                        favoriteSongs.clear()
                        favoriteSongAdapter.notifyDataSetChanged()
                        trendingSongAdapter.notifyDataSetChanged()
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
                        val filePathRaw = jsonObject.optString("file_path", null)
                        val filePath = if (!filePathRaw.isNullOrEmpty() && !filePathRaw.startsWith("http")) {
                            "http://10.0.2.2:8000/$filePathRaw"
                        } else {
                            filePathRaw
                        }
                        songs.add(
                            Song(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.getString("artist"),
                                file_path = filePath,
                                quality = jsonObject.optString("quality", null),
                                trendingScore = jsonObject.optInt("trending_score", 0),
                                isRecommended = jsonObject.optInt("is_recommended", 0) == 1,
                                thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                                albumId = jsonObject.optInt("album_id", 0),
                                lyrics = jsonObject.optString("lyrics", null)
                            )
                        )
                    }
                    binding.rvRecommendations.adapter = SongAdapter(
                        context = this,
                        songs = songs,
                        favoriteSongs = favoriteSongs,
                        onSongClick = ::handleSongClick,
                        onFavoriteClick = ::handleFavoriteClick,
                        onDownloadClick = ::handleDownloadClick,
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

    private fun updateTrendingSongs() {
        filteredTrendingSongs.clear()
        val displayCount = if (isExpanded) 10 else 3
        filteredTrendingSongs.addAll(allTrendingSongs.take(displayCount))
        trendingSongAdapter.notifyDataSetChanged()
        binding.btnToggleViewMore.setImageResource(
            if (isExpanded) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_more
        )
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
                Toast.makeText(this, "Lỗi cập nhật yêu thích: ${error.message ?: "Không xác định"}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Content-Type" to "application/json")
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
                        val songObj = jsonObject.getJSONObject("song")
                        favoriteSongs.add(
                            Song(
                                id = songObj.getInt("id"),
                                title = songObj.getString("title"),
                                artist = songObj.getString("artist"),
                                file_path = songObj.optString("file_path", null),
                                quality = songObj.optString("quality", null),
                                trendingScore = songObj.optInt("trending_score", 0),
                                isRecommended = songObj.optInt("is_recommended", 0) == 1,
                                thumbnailUrl = songObj.optString("thumbnail_url", null),
                                albumId = songObj.optInt("album_id", 0),
                                lyrics = songObj.optString("lyrics", null)
                            )
                        )
                    }
                }
                favoriteSongAdapter.notifyDataSetChanged()
                trendingSongAdapter.notifyDataSetChanged()
            },
            { error ->
                Log.e("MainActivity", "Error fetching favorites: ${error.message}")
                Toast.makeText(this, "Lỗi lấy danh sách yêu thích: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun navigateToSongActivity(song: Song) {
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
        Log.d("MainActivity", "Downloading song from: ${song.file_path}")
        // Bạn có thể bổ sung code tải file thực tế tại đây
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
