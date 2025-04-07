package com.dacs.nnmusicapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dacs.nnmusicapp.databinding.ItemSongBinding

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit = { _ -> },
    private val onEditClick: (Song) -> Unit = { _ -> },
    private val onDeleteClick: (Song) -> Unit = { _ -> },
    private val isManageMode: Boolean = false
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, isManageMode)
        holder.itemView.setOnClickListener { onSongClick(song) }
        if (isManageMode) {
            holder.binding.btnEdit.setOnClickListener { onEditClick(song) }
            holder.binding.btnDelete.setOnClickListener { onDeleteClick(song) }
        }
    }

    override fun getItemCount(): Int = songs.size

    class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, isManageMode: Boolean) {
            binding.tvTitle.text = song.title ?: "Unknown Title"
            binding.tvArtist.text = song.artist ?: "Unknown Artist"
            Glide.with(binding.ivThumbnail.context)
                .load(song.thumbnailUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(binding.ivThumbnail)

            // Hiển thị hoặc ẩn nút Sửa/Xóa dựa trên chế độ
            if (isManageMode) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
            } else {
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            }

            // (Tùy chọn) Hiển thị trạng thái isRecommended nếu cần
            // Nếu layout item_song.xml có TextView tvIsRecommended
            // binding.tvIsRecommended?.text = if (song.isRecommended == true) "Recommended" else "Not Recommended"
        }
    }
}