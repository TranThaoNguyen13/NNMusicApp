package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SongListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var songs: List<Song>
    private val favoriteSongs = mutableListOf<Song>() // Danh sách bài hát yêu thích
    private var userId: String? = null
    private var isLoggedIn: Boolean = false
    private lateinit var requestQueue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)

        // Khởi tạo requestQueue cho Volley
        requestQueue = Volley.newRequestQueue(requireContext())

        // Lấy userId và trạng thái đăng nhập từ SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", "default_user")
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        recyclerView = view.findViewById(R.id.rvSongs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Lấy danh sách bài hát từ arguments
        songs = arguments?.getParcelableArrayList("songs") ?: emptyList()

        if (songs.isEmpty()) {
            Toast.makeText(requireContext(), "Không có bài hát để hiển thị", Toast.LENGTH_SHORT).show()
            return view
        }

        // Lấy danh sách bài hát yêu thích từ server
        if (isLoggedIn && userId != null) {
            fetchFavoriteSongsFromServer()
        } else {
            // Nếu chưa đăng nhập, khởi tạo adapter với danh sách rỗng
            initializeAdapter()
        }

        return view
    }

    private fun fetchFavoriteSongsFromServer() {
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
                initializeAdapter()
            },
            { error ->
                Log.e("SongListFragment", "Error fetching favorites: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Lỗi lấy danh sách yêu thích: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Nếu lỗi, vẫn khởi tạo adapter với danh sách rỗng
                initializeAdapter()
            }
        )
        requestQueue.add(request)
    }

    private fun initializeAdapter() {
        songAdapter = SongAdapter(
            context = requireContext(),
            songs = songs,
            favoriteSongs = favoriteSongs, // Truyền danh sách yêu thích vào adapter
            onSongClick = { selectedSong ->
                val activity = requireActivity()
                if (activity is AlbumSongsActivity) {
                    activity.navigateToPlayer(selectedSong)
                }
            },
            onEditClick = { /* Không cần xử lý chỉnh sửa trong SongListFragment */ },
            onDeleteClick = { /* Không cần xử lý xóa trong SongListFragment */ },
            onFavoriteClick = { song, isFavorite ->
                if (isLoggedIn) {
                    updateFavoriteOnServer(song, isFavorite)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Vui lòng đăng nhập để thêm vào yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                }
            },
            isManageMode = false,
            userId = userId ?: "default_user"
        )
        recyclerView.adapter = songAdapter
    }

    private fun updateFavoriteOnServer(song: Song, isFavorite: Boolean) {
        if (userId == null || song.id == null) {
            Log.e("SongListFragment", "Invalid userId or songId: userId=$userId, songId=${song.id}")
            Toast.makeText(requireContext(), "Dữ liệu không hợp lệ", Toast.LENGTH_LONG).show()
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
                Log.d("SongListFragment", "Server response: $response")
                Toast.makeText(
                    requireContext(),
                    if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
                fetchFavoriteSongsFromServer() // Cập nhật lại danh sách yêu thích
            },
            { error ->
                Log.e("SongListFragment", "Error updating favorite: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Lỗi cập nhật yêu thích: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
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

    companion object {
        fun newInstance(songs: List<Song>): SongListFragment {
            return SongListFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("songs", ArrayList(songs))
                }
            }
        }
    }
}