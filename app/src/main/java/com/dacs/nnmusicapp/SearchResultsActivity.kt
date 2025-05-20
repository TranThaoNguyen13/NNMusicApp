package com.dacs.nnmusicapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dacs.nnmusicapp.databinding.ActivitySearchResultsBinding

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultsBinding

    private val allSongs = mutableListOf<Song>()
    private val allAlbums = mutableListOf<Album>()

    private val filteredSongs = mutableListOf<Song>()
    private val filteredAlbums = mutableListOf<Album>()

    private lateinit var songAdapter: SongAdapter
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo RecyclerView với LayoutManager
        binding.rvSongs.layoutManager = LinearLayoutManager(this)
        binding.rvAlbums.layoutManager = LinearLayoutManager(this)

        // Adapter cho danh sách bài hát, truyền callback nhấn bài hát
        songAdapter = SongAdapter(
            context = this,
            songs = filteredSongs,
            favoriteSongs = mutableListOf(),
            onSongClick = { song ->
                val intent = Intent(this, SongActivity::class.java).apply {
                    putExtra("song_title", song.title)
                    putExtra("song_artist", song.artist)
                    putExtra("song_url", song.file_path)
                    putExtra("song_thumbnail", song.thumbnailUrl)
                    putExtra("song_lyrics", song.lyrics)
                }
                startActivity(intent)
            },
            onFavoriteClick = { _, _ -> /* không dùng ở đây */ },
            onDownloadClick = { _ -> /* không dùng ở đây */ },
            isManageMode = false,
            userId = null,
            isVip = false
        )
        binding.rvSongs.adapter = songAdapter

        // Adapter cho danh sách album, truyền callback nhấn album
        albumAdapter = AlbumAdapter(
            albums = filteredAlbums,
            onAlbumClick = { album ->
                val intent = Intent(this, AlbumSongsActivity::class.java).apply {
                    putExtra("albumId", album.id)
                }
                startActivity(intent)
            }
        )
        binding.rvAlbums.adapter = albumAdapter

        // Lấy dữ liệu từ intent
        val query = intent.getStringExtra("search_query") ?: ""

        val songsFromIntent = intent.getParcelableArrayListExtra<Song>("all_songs")
        val albumsFromIntent = intent.getParcelableArrayListExtra<Album>("all_albums")

        if (songsFromIntent != null) allSongs.addAll(songsFromIntent)
        if (albumsFromIntent != null) allAlbums.addAll(albumsFromIntent)

        title = "Kết quả tìm kiếm cho \"$query\""

        // Lọc và hiển thị kết quả
        filterResults(query)
    }

    private fun filterResults(query: String) {
        filteredSongs.clear()
        filteredAlbums.clear()

        val q = query.trim()
        // Lọc bài hát theo tên hoặc nghệ sĩ
        filteredSongs.addAll(allSongs.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        })
        // Lọc album theo tên hoặc nghệ sĩ
        filteredAlbums.addAll(allAlbums.filter {
            it.title.contains(q, ignoreCase = true) || (it.artist?.contains(q, ignoreCase = true) ?: false)
        })

        songAdapter.notifyDataSetChanged()
        albumAdapter.notifyDataSetChanged()

        // Nếu không có bài hát thì hiện thông báo và ẩn danh sách bài hát
        if (filteredSongs.isEmpty()) {
            binding.tvNoSongs.visibility = View.VISIBLE
            binding.rvSongs.visibility = View.GONE
        } else {
            binding.tvNoSongs.visibility = View.GONE
            binding.rvSongs.visibility = View.VISIBLE
        }

        // Nếu không có album thì ẩn luôn phần album (cả tiêu đề + recycler)
        if (filteredAlbums.isEmpty()) {
            binding.tvNoAlbums.visibility = View.GONE
            binding.rvAlbums.visibility = View.GONE
            binding.tvAlbumTitle.visibility = View.GONE  // TextView tiêu đề "Kết quả album"
        } else {
            binding.tvNoAlbums.visibility = View.GONE
            binding.rvAlbums.visibility = View.VISIBLE
            binding.tvAlbumTitle.visibility = View.VISIBLE
        }

        // Nếu cả 2 đều không có thì Toast thông báo
        if (filteredSongs.isEmpty() && filteredAlbums.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy kết quả nào cho \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }
}
