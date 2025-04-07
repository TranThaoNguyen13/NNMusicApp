package com.dacs.nnmusicapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit? = { _ -> }, // Callback khi click vào bài hát
    private val onEditClick: (Song) -> Unit? = { _ -> }, // Callback khi click nút Sửa (quản lý)
    private val onDeleteClick: (Song) -> Unit? = { _ -> }, // Callback khi click nút Xóa (quản lý)
    private val isManageMode: Boolean = false // Chế độ quản lý: hiển thị nút Sửa/Xóa
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, isManageMode)
        holder.itemView.setOnClickListener { onSongClick(song) }
        if (isManageMode) {
            holder.btnEdit?.setOnClickListener { onEditClick(song) }
            holder.btnDelete?.setOnClickListener { onDeleteClick(song) }
        }
    }

    override fun getItemCount(): Int = songs.size

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnEdit: ImageButton? = itemView.findViewById(R.id.btnEdit) // Nút Sửa (có thể null)
        val btnDelete: ImageButton? = itemView.findViewById(R.id.btnDelete) // Nút Xóa (có thể null)

        fun bind(song: Song, isManageMode: Boolean) {
            tvTitle.text = song.title ?: "Unknown Title"
            tvArtist.text = song.artist ?: "Unknown Artist"
            Glide.with(itemView.context)
                .load(song.thumbnailUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivThumbnail)

            // Hiển thị hoặc ẩn nút Sửa/Xóa dựa trên chế độ
            if (isManageMode) {
                btnEdit?.visibility = View.VISIBLE
                btnDelete?.visibility = View.VISIBLE
            } else {
                btnEdit?.visibility = View.GONE
                btnDelete?.visibility = View.GONE
            }
        }
    }
}