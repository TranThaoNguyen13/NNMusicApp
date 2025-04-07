package com.dacs.nnmusicapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dacs.nnmusicapp.databinding.ItemAlbumBinding

class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)
        holder.itemView.setOnClickListener { onAlbumClick(album) }
    }

    override fun getItemCount(): Int = albums.size

    class AlbumViewHolder(private val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: Album) {
            binding.tvAlbumTitle.text = album.title
            binding.tvArtist.text = album.artist ?: "Unknown Artist"
            Glide.with(binding.ivAlbumCover.context)
                .load(album.coverUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(binding.ivAlbumCover)
        }
    }
}