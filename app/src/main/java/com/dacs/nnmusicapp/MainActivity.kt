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
    import com.android.volley.toolbox.Volley
    import com.dacs.nnmusicapp.databinding.ActivityMainBinding
    import com.google.gson.Gson
    import com.google.gson.reflect.TypeToken
    import org.json.JSONException

    class MainActivity : AppCompatActivity() {

        companion object {
            const val BASE_URL = "http://10.0.2.2:8000/api" // URL gốc của API Laravel
        }

        private lateinit var binding: ActivityMainBinding
        private lateinit var viewPager: ViewPager2
        private lateinit var btnAuth: Button
        private lateinit var btnFavorite: ImageButton
        private lateinit var requestQueue: RequestQueue
        private lateinit var albumAdapter: AlbumAdapter
        private lateinit var trendingSongAdapter: SongAdapter
        private lateinit var favoriteSongAdapter: SongAdapter
        private var isLoggedIn = false
        private var userId: String? = null
        private val allTrendingSongs = mutableListOf<Song>() // Lưu trữ tất cả bài hát trending
        private val displayedTrendingSongs = mutableListOf<Song>() // Lưu trữ bài hát trending đang hiển thị
        private val favoriteSongs = mutableListOf<Song>()
        private val gson = Gson()
        private var isExpanded = false // Trạng thái: true nếu hiển thị 10 bài, false nếu hiển thị 3 bài

        private val sliderImages = listOf(
            "https://i.imgur.com/stW72UJ.png",
            "https://i.imgur.com/vGm99k8.png",
            "https://i.imgur.com/y7zP0U8.png"
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Kiểm tra trạng thái đăng nhập
            val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            userId = sharedPreferences.getString("user_id", "default_user")

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            try {
                // Ánh xạ các thành phần
                btnAuth = binding.btnAuth
                btnFavorite = binding.btnFavorite
                viewPager = binding.viewPager
                binding.rvAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvRecommendations.layoutManager = LinearLayoutManager(this)

                // Thiết lập RecyclerView cho danh sách trending
                binding.rvTrendingSongs.layoutManager = LinearLayoutManager(this)
                displayedTrendingSongs.clear()
                trendingSongAdapter = SongAdapter(
                    context = this@MainActivity,
                    songs = displayedTrendingSongs,
                    onSongClick = { song ->
                        if (isLoggedIn) {
                            Log.d("MainActivity", "Song URL before passing to SongActivity: ${song.filepath}")
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
                        } else {
                            Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    },
                    onFavoriteClick = { song, isFavorite ->
                        if (isLoggedIn) {
                            Toast.makeText(this, if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích", Toast.LENGTH_SHORT).show()
                            updateFavoriteSongs()
                        } else {
                            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    },
                    isManageMode = false,
                    userId = userId ?: "default_user"
                )
                binding.rvTrendingSongs.adapter = trendingSongAdapter

                // Thiết lập RecyclerView cho danh sách yêu thích
                binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)
                favoriteSongAdapter = SongAdapter(
                    context = this@MainActivity,
                    songs = favoriteSongs,
                    onSongClick = { song ->
                        if (isLoggedIn) {
                            Log.d("MainActivity", "Song URL before passing to SongActivity: ${song.filepath}")
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
                        } else {
                            Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    },
                    onFavoriteClick = { song, isFavorite ->
                        if (isLoggedIn) {
                            Toast.makeText(this, if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích", Toast.LENGTH_SHORT).show()
                            updateFavoriteSongs()
                        } else {
                            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    },
                    isManageMode = false,
                    userId = userId ?: "default_user"
                )
                binding.rvFavoriteSongs.adapter = favoriteSongAdapter

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
                            putString("user_id", null)
                            apply()
                        }
                        isLoggedIn = false
                        userId = "default_user"
                        updateAuthButton()
                        updateFavoriteSongs()
                        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                    } else {
                        // Đăng nhập
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }

                // Xử lý nút Yêu thích
                btnFavorite.setOnClickListener {
                    val intent = Intent(this, FavoriteSongsActivity::class.java)
                    intent.putParcelableArrayListExtra("all_songs", ArrayList(allTrendingSongs))
                    startActivity(intent)
                }

                // Xử lý nút "Xem thêm"/"Thu lại" cho danh sách trending
                binding.btnToggleViewMore.setOnClickListener {
                    toggleTrendingSongs()
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
            val url = "$BASE_URL/albums"

            binding.progressBar?.visibility = View.VISIBLE // Hiển thị loading (nếu có ProgressBar)
            val request = JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
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
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
                    Log.e("MainActivity", "Volley error: ${error.message}")
                    Toast.makeText(this, "Error fetching albums: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            requestQueue.add(request)
        }

        private fun fetchTrendingSongs() {
            val url = "$BASE_URL/songs/trending"

            binding.progressBar?.visibility = View.VISIBLE // Hiển thị loading
            val request = JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
                    Log.d("MainActivity", "API Response: $response")
                    try {
                        allTrendingSongs.clear()
                        for (i in 0 until response.length()) {
                            val jsonObject = response.getJSONObject(i)
                            val song = Song(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.getString("artist"),
                                filepath = jsonObject.optString("filepath", null),
                                quality = jsonObject.optString("quality", null),
                                trendingScore = if (jsonObject.isNull("trending_score")) null else jsonObject.optInt("trending_score", 0),
                                isRecommended = if (jsonObject.isNull("is_recommended")) null else jsonObject.optInt("is_recommended", 0) == 1,
                                thumbnailUrl = jsonObject.optString("thumbnail_url", null),
                                albumId = if (jsonObject.isNull("album_id")) null else jsonObject.optInt("album_id", 0),
                                lyrics = jsonObject.optString("lyrics", null)
                            )
                            allTrendingSongs.add(song)
                        }
                        // Sắp xếp theo trendingScore (giảm dần)
                        allTrendingSongs.sortByDescending { it.trendingScore ?: 0 }
                        // Hiển thị 3 bài đầu tiên ban đầu
                        updateTrendingSongs()
                        updateFavoriteSongs()
                    } catch (e: JSONException) {
                        Log.e("MainActivity", "Error parsing response: ${e.message}")
                        Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                { error ->
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
                    Log.e("MainActivity", "Error fetching trending songs: ${error.message}")
                    Toast.makeText(this, "Lỗi kết nối API: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            requestQueue.add(request)
        }

        private fun fetchRecommendations() {
            val url = "$BASE_URL/songs/recommendations"

            binding.progressBar?.visibility = View.VISIBLE // Hiển thị loading
            val jsonArrayRequest = JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
                    val songs = mutableListOf<Song>()
                    for (i in 0 until response.length()) {
                        try {
                            val jsonObject = response.getJSONObject(i)
                            val song = Song(
                                id = jsonObject.getInt("id"),
                                title = jsonObject.getString("title"),
                                artist = jsonObject.getString("artist"),
                                filepath = jsonObject.optString("filepath", null),
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
                        context = this@MainActivity,
                        songs = songs,
                        onSongClick = { selectedSong ->
                            if (isLoggedIn) {
                                Log.d("MainActivity", "Song URL before passing to SongActivity: ${selectedSong.filepath}")
                                if (selectedSong.filepath.isNullOrEmpty()) {
                                    Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = Intent(this, SongActivity::class.java).apply {
                                        putExtra("song_title", selectedSong.title)
                                        putExtra("song_artist", selectedSong.artist)
                                        putExtra("song_url", selectedSong.filepath)
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
                        onFavoriteClick = { song, isFavorite ->
                            if (isLoggedIn) {
                                Toast.makeText(this, if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích", Toast.LENGTH_SHORT).show()
                                updateFavoriteSongs()
                            } else {
                                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                        },
                        isManageMode = false,
                        userId = userId ?: "default_user"
                    )
                },
                { error ->
                    binding.progressBar?.visibility = View.GONE // Ẩn loading
                    Toast.makeText(this, "Lỗi khi lấy gợi ý bài hát: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            requestQueue.add(jsonArrayRequest)
        }

        private fun updateFavoriteSongs() {
            val sharedPreferences = getSharedPreferences("favorites_${userId}", MODE_PRIVATE)
            val favoritesJson = sharedPreferences.getString("favorite_songs", "[]")
            val type = object : TypeToken<MutableSet<Int>>() {}.type
            val favoriteSongIds: MutableSet<Int> = gson.fromJson(favoritesJson, type) ?: mutableSetOf()

            favoriteSongs.clear()
            favoriteSongs.addAll(allTrendingSongs.filter { favoriteSongIds.contains(it.id) })
            if (::favoriteSongAdapter.isInitialized) {
                favoriteSongAdapter.notifyDataSetChanged()
            }
        }

        private fun updateTrendingSongs() {
            displayedTrendingSongs.clear()
            val displayCount = if (isExpanded) 10 else 3 // Hiển thị 10 bài nếu mở rộng, 3 bài nếu thu gọn
            displayedTrendingSongs.addAll(allTrendingSongs.take(displayCount))
            trendingSongAdapter.notifyDataSetChanged()

            // Cập nhật icon của nút "Xem thêm"/"Thu lại"
            binding.btnToggleViewMore.setImageResource(
                if (isExpanded) android.R.drawable.ic_menu_close_clear_cancel // Icon "Thu lại"
                else android.R.drawable.ic_menu_more // Icon "Xem thêm"
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
            userId = sharedPreferences.getString("user_id", "default_user")
            updateAuthButton()
            updateFavoriteSongs()
        }
    }