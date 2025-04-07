package com.dacs.nnmusicapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SongListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var songs: List<Song>
    private var userId: String? = null
    private var isLoggedIn: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)

        // Lấy userId và trạng thái đăng nhập từ SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", "default_user")
        isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        recyclerView = view.findViewById(R.id.rvSongs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Lấy danh sách bài hát từ arguments hoặc từ activity
        songs = arguments?.getParcelableArrayList("songs") ?: emptyList()

        if (songs.isEmpty()) {
            // Xử lý khi không có bài hát
            Toast.makeText(requireContext(), "Không có bài hát để hiển thị", Toast.LENGTH_SHORT).show()
            return view
        }

        songAdapter = SongAdapter(
            context = requireContext(), // Truyền context
            songs = songs,
            onSongClick = { selectedSong ->
                // Lấy activity chứa fragment (AlbumSongsActivity) và gọi navigateToPlayer
                val activity = requireActivity()
                if (activity is AlbumSongsActivity) {
                    activity.navigateToPlayer(selectedSong)
                }
            },
            onEditClick = { /* Không cần xử lý chỉnh sửa trong SongListFragment */ },
            onDeleteClick = { /* Không cần xử lý xóa trong SongListFragment */ },
            onFavoriteClick = { song, isFavorite ->
                if (isLoggedIn) {
                    Toast.makeText(
                        requireContext(),
                        if (isFavorite) "Đã thêm ${song.title} vào yêu thích" else "Đã xóa ${song.title} khỏi yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Cập nhật danh sách yêu thích nếu cần (có thể thông báo cho activity để cập nhật)
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

        return view
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