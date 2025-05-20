package com.dacs.nnmusicapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class SongAdapter(
    private val context: Context,
    private val songs: List<Song>,
    private val favoriteSongs: List<Song>,
    private val onSongClick: (Song) -> Unit,
    private val onEditClick: (Song) -> Unit = {},
    private val onDeleteClick: (Song) -> Unit = {},
    private val onFavoriteClick: (Song, Boolean) -> Unit,
    private val onDownloadClick: (Song) -> Unit = {},
    private val isManageMode: Boolean = false,
    private val userId: String? = null,
    private val isVip: Boolean = false
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
        val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)

        fun bind(song: Song, position: Int) {
            Log.d("SongAdapter", "Binding song: ${song.title}, position: $position")
            tvRank.text = if (position < 9) "Top 0${position + 1}" else "Top ${position + 1}"
            tvTitle.text = song.title ?: "No title"
            tvArtist.text = song.artist ?: "Unknown Artist"

            // Tải thumbnail với Picasso
            if (!song.thumbnailUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(song.thumbnailUrl)
                    .resize(100, 100) // Giảm kích thước hơn nữa để tối ưu emulator
                    .centerCrop()
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(ivThumbnail, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            Log.d("SongAdapter", "Thumbnail loaded: ${song.thumbnailUrl}")
                        }

                        override fun onError(e: Exception?) {
                            Log.e("SongAdapter", "Error loading thumbnail: ${song.thumbnailUrl}, ${e?.message}")
                            ivThumbnail.setImageResource(R.drawable.ic_music_placeholder)
                        }
                    })
            } else {
                Log.d("SongAdapter", "No thumbnail for song: ${song.title}")
                ivThumbnail.setImageResource(R.drawable.ic_music_placeholder)
            }

            // Xử lý nút yêu thích
            if (userId != null && isLoggedIn(context)) {
                val isFavorite = favoriteSongs.any { it.id == song.id }
                btnFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
                btnFavorite.setOnClickListener {
                    val newFavoriteStatus = !isFavorite
                    onFavoriteClick(song, newFavoriteStatus)
                    btnFavorite.setImageResource(
                        if (newFavoriteStatus) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    )
                }
                btnFavorite.visibility = View.VISIBLE
            } else {
                btnFavorite.visibility = View.GONE
            }

            // Xử lý nút tải xuống
            if (isVip && !isManageMode) {
                btnDownload.visibility = View.VISIBLE
                btnDownload.setOnClickListener { onDownloadClick(song) }
            } else {
                btnDownload.visibility = View.GONE
            }

            // Xử lý chế độ quản lý
            if (isManageMode) {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnFavorite.visibility = View.GONE
                btnDownload.visibility = View.GONE
                btnEdit.setOnClickListener { onEditClick(song) }
                btnDelete.setOnClickListener { onDeleteClick(song) }
            } else {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
                itemView.setOnClickListener { onSongClick(song) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount(): Int {
        Log.d("SongAdapter", "Item count: ${songs.size}")
        return songs.size
    }

    private fun isLoggedIn(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
}