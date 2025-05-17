package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class AlbumSongsActivity : AppCompatActivity() {

    private lateinit var songs: MutableList<Song>
    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_songs)

        // Kiểm tra trạng thái đăng nhập
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Danh sách bài hát"

        // Khởi tạo danh sách bài hát
        songs = mutableListOf()

        // Lấy albumId từ Intent
        val albumId = intent.getIntExtra("albumId", -1)
        if (albumId == -1) {
            Toast.makeText(this, "Invalid album ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fetchSongsByAlbum(albumId)
    }

    private fun fetchSongsByAlbum(albumId: Int) {
        val url = "${MainActivity.BASE_URL}/albums/$albumId/songs"
        Log.d("AlbumSongsActivity", "Fetching songs from: $url")

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("AlbumSongsActivity", "Raw API response: $response")
                try {
                    val songsArray = response.optJSONArray("songs")
                    if (songsArray == null || songsArray.length() == 0) {
                        Toast.makeText(this, "Không có bài hát nào trong album này", Toast.LENGTH_LONG).show()
                        finish()
                        return@JsonObjectRequest
                    }

                    songs.clear()
                    for (i in 0 until songsArray.length()) {
                        val songJson = songsArray.getJSONObject(i)
                        val file_path = songJson.optString("file_path", null)
                        if (file_path.isNullOrEmpty()) {
                            Log.w("AlbumSongsActivity", "Empty or null file_path for song ${songJson.getString("title")}")
                            continue
                        }
                        // Đảm bảo filepath là URL đầy đủ (nếu cần)
                        val adjustedFilepath = if (!file_path.startsWith("http")) {
                            "http://10.0.2.2:8000/$file_path"
                        } else {
                            file_path
                        }
                        Log.d("AlbumSongsActivity", "Adjusted file_path: $adjustedFilepath")
                        val song = Song(
                            id = songJson.getInt("id"),
                            title = songJson.getString("title"),
                            artist = songJson.getString("artist"),
                            file_path = adjustedFilepath,
                            quality = songJson.optString("quality", null),
                            trendingScore = if (songJson.isNull("trending_score")) null else songJson.optInt("trending_score", 0),
                            isRecommended = if (songJson.isNull("is_recommended")) null else songJson.optInt("is_recommended", 0) == 1,
                            thumbnailUrl = songJson.optString("thumbnail_url", null),
                            albumId = if (songJson.isNull("album_id")) null else songJson.optInt("album_id", 0),
                            lyrics = songJson.optString("lyrics", null)
                        )
                        Log.d("AlbumSongsActivity", "Parsed song: $song")
                        songs.add(song)
                    }

                    if (songs.isEmpty()) {
                        Toast.makeText(this, "Không có bài hát nào có URL hợp lệ", Toast.LENGTH_LONG).show()
                        finish()
                        return@JsonObjectRequest
                    }

                    val fragment = SongListFragment.newInstance(songs)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                } catch (e: JSONException) {
                    Log.e("AlbumSongsActivity", "JSON parsing error: ${e.message}")
                    Log.e("AlbumSongsActivity", "Full response causing error: $response")
                    Toast.makeText(this, "Error parsing songs: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            { error ->
                Log.e("AlbumSongsActivity", "Volley error: ${error.message ?: "Unknown error"}")
                Log.e("AlbumSongsActivity", "Network response: ${error.networkResponse?.statusCode}")
                if (error.networkResponse?.statusCode == 404) {
                    Log.e("AlbumSongsActivity", "Endpoint not found or file not accessible")
                }
                Toast.makeText(this, "Error fetching songs: ${error.message ?: "Unable to connect to server"}", Toast.LENGTH_LONG).show()
                finish()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    fun navigateToPlayer(song: Song) {
        if (!isLoggedIn) {
            Toast.makeText(this, "Vui lòng đăng nhập để nghe nhạc", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        Log.d("AlbumSongsActivity", "Song URL before passing to SongActivity: ${song.file_path}")
        if (song.file_path.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuyển sang SongActivity
        val intent = Intent(this, SongActivity::class.java).apply {
            putExtra("song_title", song.title)
            putExtra("song_artist", song.artist)
            putExtra("song_url", song.file_path)
            putExtra("song_thumbnail", song.thumbnailUrl)
            putExtra("song_lyrics", song.lyrics)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

}