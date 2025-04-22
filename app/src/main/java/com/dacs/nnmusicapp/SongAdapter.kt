package com.dacs.nnmusicapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso

class SongAdapter(
    private val context: Context,
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit,
    private val onEditClick: (Song) -> Unit = {},
    private val onDeleteClick: (Song) -> Unit = {},
    private val onFavoriteClick: (Song, Boolean) -> Unit,
    private val isManageMode: Boolean = false,
    private val userId: String? = null
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvRank) // Thêm TextView cho thứ hạng
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)

        fun bind(song: Song, position: Int) { // Thêm tham số position để hiển thị thứ hạng
            // Hiển thị thứ hạng
            tvRank.text = "Top ${position + 1}"

            tvTitle.text = song.title
            tvArtist.text = song.artist

            // Tải hình ảnh từ thumbnailUrl
            if (!song.thumbnailUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(song.thumbnailUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(ivThumbnail)
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_music_placeholder)
            }

            if (userId != null) {
                val sharedPreferences = context.getSharedPreferences("favorites_${userId}", Context.MODE_PRIVATE)
                val favoritesJson = sharedPreferences.getString("favorite_songs", "[]")
                val type = object : TypeToken<MutableSet<Int>>() {}.type
                val favoriteSongIds: MutableSet<Int> = Gson().fromJson(favoritesJson, type) ?: mutableSetOf()
                val isFavorite = favoriteSongIds.contains(song.id)
                btnFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )

                btnFavorite.setOnClickListener {
                    val newFavoriteStatus = !isFavorite
                    if (newFavoriteStatus) {
                        favoriteSongIds.add(song.id)
                    } else {
                        favoriteSongIds.remove(song.id)
                    }
                    sharedPreferences.edit().putString("favorite_songs", Gson().toJson(favoriteSongIds)).apply()
                    btnFavorite.setImageResource(
                        if (newFavoriteStatus) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    )
                    onFavoriteClick(song, newFavoriteStatus)
                }
            } else {
                btnFavorite.visibility = View.GONE
            }

            if (isManageMode) {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnFavorite.visibility = View.GONE
                btnEdit.setOnClickListener { onEditClick(song) }
                btnDelete.setOnClickListener { onDeleteClick(song) }
            } else {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
                btnFavorite.visibility = if (userId != null) View.VISIBLE else View.GONE
                itemView.setOnClickListener { onSongClick(song) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position) // Truyền position vào bind()
    }

    override fun getItemCount(): Int = songs.size
}