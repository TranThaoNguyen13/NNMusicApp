package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class AlbumSongsActivity : AppCompatActivity() {

    private lateinit var songs: List<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_songs)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Hiển thị nút quay lại
        supportActionBar?.title = "Danh sách bài hát"

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
        val url = "http://10.0.2.2/nnmusicapp_api/api.php?action=songs_by_album&album_id=$albumId"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("AlbumSongsActivity", "API response: $response")
                val songList = mutableListOf<Song>()
                try {
                    for (i in 0 until response.length()) {
                        val songJson = response.getJSONObject(i)
                        val song = Song(
                            id = songJson.getInt("id"),
                            title = songJson.getString("title"),
                            artist = songJson.getString("artist"),
                            url = songJson.optString("url", null),
                            quality = songJson.optString("quality", null),
                            trendingScore = if (songJson.isNull("trending_score")) null else songJson.optInt("trending_score", 0),
                            isRecommended = if (songJson.isNull("is_recommended")) null else songJson.optInt("is_recommend", 0) == 1,
                            thumbnailUrl = songJson.optString("thumbnail_url", null),
                            albumId = if (songJson.isNull("album_id")) null else songJson.optInt("album_id", 0),
                            lyrics = songJson.optString("lyrics", null)
                        )
                        Log.d("AlbumSongsActivity", "Parsed song: $song")
                        songList.add(song)
                    }
                    songs = songList

                    if (songs.isEmpty()) {
                        Toast.makeText(this, "Không có bài hát trong album", Toast.LENGTH_LONG).show()
                        finish()
                        return@JsonArrayRequest
                    }

                    // Truyền danh sách bài hát cho SongListFragment
                    val fragment = SongListFragment.newInstance(songs)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                } catch (e: JSONException) {
                    Log.e("AlbumSongsActivity", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Error parsing songs", Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            { error ->
                Log.e("AlbumSongsActivity", "Volley error: ${error.message}")
                Toast.makeText(this, "Error fetching songs: ${error.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    fun navigateToPlayer(song: Song) {
        Log.d("AlbumSongsActivity", "Song URL before passing to SongActivity: ${song.url}")
        if (song.url.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuyển sang SongActivity thay vì MusicPlayerActivity
        val intent = Intent(this, SongActivity::class.java).apply {
            putExtra("song_title", song.title)
            putExtra("song_artist", song.artist)
            putExtra("song_url", song.url)
            putExtra("song_thumbnail", song.thumbnailUrl)
            putExtra("song_lyrics", song.lyrics) // Truyền lyrics
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}