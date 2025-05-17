package com.dacs.nnmusicapp.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dacs.nnmusicapp.R
import com.dacs.nnmusicapp.Song
import com.dacs.nnmusicapp.SongAdapter
import com.dacs.nnmusicapp.databinding.ActivityManageSongsBinding
import org.json.JSONArray
import org.json.JSONObject

class ManageSongsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageSongsBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var requestQueue: RequestQueue
    private val songs = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo RequestQueue
        requestQueue = Volley.newRequestQueue(this)

        // Thiết lập Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Quản lý bài hát"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Xử lý nút quay lại
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Thiết lập RecyclerView
        binding.rvSongs.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(
            context = this,
            songs = songs,
            onSongClick = { /* Không cần xử lý click vào bài hát trong chế độ quản lý */ },
            onEditClick = { song -> showEditSongDialog(song) },
            onDeleteClick = { song -> deleteSong(song) },
            onFavoriteClick = { _, _ -> /* Không cần xử lý yêu thích trong ManageSongsActivity */ },
            isManageMode = true,
            userId = "admin" // Hoặc lấy userId từ SharedPreferences nếu cần
        )
        binding.rvSongs.adapter = songAdapter

        // Xử lý nút thêm bài hát
        binding.fabAddSong.setOnClickListener {
            showAddSongDialog()
        }

        // Lấy danh sách bài hát
        fetchSongs()
    }

    private fun fetchSongs() {
        val url = "http://10.0.2.2/nnmusicapp_api/songs.php?action=get_songs"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("ManageSongsActivity", "API Response: $response")
                try {
                    val jsonArray = JSONArray(response)
                    songs.clear()
                    for (i in 0 until jsonArray.length()) {
                        val songJson = jsonArray.getJSONObject(i)
                        val song = Song(
                            id = songJson.getInt("id"),
                            title = songJson.getString("title"),
                            artist = songJson.getString("artist"),
                            filepath = if (songJson.isNull("filepath")) null else songJson.getString("url"),
                            quality = if (songJson.isNull("quality")) null else songJson.getString("quality"),
                            trendingScore = if (songJson.isNull("trending_score")) null else songJson.getInt("trending_score"),
                            isRecommended = if (songJson.isNull("is_recommended")) null else songJson.getInt("is_recommended") == 1,
                            thumbnailUrl = if (songJson.isNull("thumbnail_url")) null else songJson.getString("thumbnail_url"),
                            albumId = if (songJson.isNull("album_id")) null else songJson.getInt("album_id"),
                            lyrics = if (songJson.isNull("lyrics")) null else songJson.getString("lyrics")
                        )
                        songs.add(song)
                    }
                    Log.d("ManageSongsActivity", "Parsed songs: $songs")
                    songAdapter.notifyDataSetChanged()
                    if (songs.isNotEmpty()) {
                        Log.d("ManageSongsActivity", "RecyclerView should display ${songs.size} songs")
                    } else {
                        Toast.makeText(this, "Không có bài hát", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("ManageSongsActivity", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("ManageSongsActivity", "Error fetching songs: ${error.message}")
                Toast.makeText(this, "Lỗi kết nối API: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        requestQueue.add(request)
    }

    private fun showAddSongDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_song, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Thêm bài hát")
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etArtist = dialogView.findViewById<EditText>(R.id.etArtist)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val spinnerQuality = dialogView.findViewById<Spinner>(R.id.spinnerQuality)
        val cbIsRecommended = dialogView.findViewById<CheckBox>(R.id.cbIsRecommended)
        val etThumbnailUrl = dialogView.findViewById<EditText>(R.id.etThumbnailUrl)
        val etAlbumId = dialogView.findViewById<EditText>(R.id.etAlbumId)
        val etLyrics = dialogView.findViewById<EditText>(R.id.etLyrics)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val qualityLevels = resources.getStringArray(R.array.quality_levels)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuality.adapter = adapter

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val artist = etArtist.text.toString().trim()
            val url = etUrl.text.toString().trim().ifEmpty { null }
            val quality = spinnerQuality.selectedItem.toString()
            val isRecommended = cbIsRecommended.isChecked
            val thumbnailUrl = etThumbnailUrl.text.toString().trim().ifEmpty { null }
            val albumId = etAlbumId.text.toString().trim().toIntOrNull()
            val lyrics = etLyrics.text.toString().trim().ifEmpty { null }

            if (title.isEmpty() || artist.isEmpty() || url == null) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin (tên, ca sĩ, URL)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addSong(title, artist, url, quality, isRecommended, thumbnailUrl, albumId, lyrics) {
                dialog.dismiss()
                fetchSongs()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditSongDialog(song: Song) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_song, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Sửa bài hát")
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etArtist = dialogView.findViewById<EditText>(R.id.etArtist)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val spinnerQuality = dialogView.findViewById<Spinner>(R.id.spinnerQuality)
        val cbIsRecommended = dialogView.findViewById<CheckBox>(R.id.cbIsRecommended)
        val etThumbnailUrl = dialogView.findViewById<EditText>(R.id.etThumbnailUrl)
        val etAlbumId = dialogView.findViewById<EditText>(R.id.etAlbumId)
        val etLyrics = dialogView.findViewById<EditText>(R.id.etLyrics)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        etTitle.setText(song.title)
        etArtist.setText(song.artist)
        etUrl.setText(song.filepath)
        etThumbnailUrl.setText(song.thumbnailUrl)
        etAlbumId.setText(song.albumId?.toString())
        etLyrics.setText(song.lyrics)
        cbIsRecommended.isChecked = song.isRecommended == true

        val qualityLevels = resources.getStringArray(R.array.quality_levels)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuality.adapter = adapter
        spinnerQuality.setSelection(qualityLevels.indexOf(song.quality ?: "low"))

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val artist = etArtist.text.toString().trim()
            val url = etUrl.text.toString().trim().ifEmpty { null }
            val quality = spinnerQuality.selectedItem.toString()
            val isRecommended = cbIsRecommended.isChecked
            val thumbnailUrl = etThumbnailUrl.text.toString().trim().ifEmpty { null }
            val albumId = etAlbumId.text.toString().trim().toIntOrNull()
            val lyrics = etLyrics.text.toString().trim().ifEmpty { null }

            if (title.isEmpty() || artist.isEmpty() || url == null) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin (tên, ca sĩ, URL)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateSong(song.id, title, artist, url, quality, isRecommended, thumbnailUrl, albumId, lyrics) {
                dialog.dismiss()
                fetchSongs()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addSong(
        title: String,
        artist: String,
        url: String?,
        quality: String?,
        isRecommended: Boolean,
        thumbnailUrl: String?,
        albumId: Int?,
        lyrics: String?,
        onSuccess: () -> Unit
    ) {
        val urlApi = "http://10.0.2.2/nnmusicapp_api/songs.php?action=add_song"
        val request = object : StringRequest(
            Method.POST, urlApi,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        Toast.makeText(this, "Thêm bài hát thành công", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ManageSongsActivity", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("ManageSongsActivity", "Error adding song: ${error.message}")
                Toast.makeText(this, "Error adding song: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray {
                val params = JSONObject().apply {
                    put("title", title)
                    put("artist", artist)
                    put("url", url)
                    put("quality", quality)
                    put("is_recommended", if (isRecommended) 1 else 0)
                    put("thumbnail_url", thumbnailUrl)
                    put("album_id", albumId)
                    put("lyrics", lyrics)
                }
                return params.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }
        requestQueue.add(request)
    }

    private fun updateSong(
        id: Int,
        title: String,
        artist: String,
        url: String?,
        quality: String?,
        isRecommended: Boolean,
        thumbnailUrl: String?,
        albumId: Int?,
        lyrics: String?,
        onSuccess: () -> Unit
    ) {
        val urlApi = "http://10.0.2.2/nnmusicapp_api/songs.php?action=update_song"
        val request = object : StringRequest(
            Method.PUT, urlApi,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        Toast.makeText(this, "Cập nhật bài hát thành công", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ManageSongsActivity", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("ManageSongsActivity", "Error updating song: ${error.message}")
                Toast.makeText(this, "Error updating song: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray {
                val params = JSONObject().apply {
                    put("id", id)
                    put("title", title)
                    put("artist", artist)
                    put("url", url)
                    put("quality", quality)
                    put("is_recommended", if (isRecommended) 1 else 0)
                    put("thumbnail_url", thumbnailUrl)
                    put("album_id", albumId)
                    put("lyrics", lyrics)
                }
                return params.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }
        requestQueue.add(request)
    }

    private fun deleteSong(song: Song) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc chắn muốn xóa bài hát ${song.title}?")
            .setPositiveButton("Xóa") { _, _ ->
                val url = "http://10.0.2.2/nnmusicapp_api/songs.php?action=delete_song&id=${song.id}"
                val request = StringRequest(
                    Request.Method.DELETE, url,
                    { response ->
                        try {
                            val jsonObject = JSONObject(response)
                            if (jsonObject.getString("status") == "success") {
                                Toast.makeText(this, "Xóa bài hát thành công", Toast.LENGTH_SHORT).show()
                                fetchSongs()
                            } else {
                                Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("ManageSongsActivity", "Error parsing response: ${e.message}")
                            Toast.makeText(this, "Lỗi phân tích dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    { error ->
                        Log.e("ManageSongsActivity", "Error deleting song: ${error.message}")
                        Toast.makeText(this, "Error deleting song: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
                requestQueue.add(request)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}