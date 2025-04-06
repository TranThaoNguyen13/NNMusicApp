package com.dacs.nnmusicapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)
        holder.itemView.setOnClickListener { onAlbumClick(album) }
    }

    override fun getItemCount(): Int = albums.size

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlbumTitle: TextView = itemView.findViewById(R.id.tvAlbumTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val ivAlbumCover: ImageView = itemView.findViewById(R.id.ivAlbumCover)

        fun bind(album: Album) {
            tvAlbumTitle.text = album.title
            tvArtist.text = album.artist ?: "Unknown Artist"
            Glide.with(itemView.context)
                .load(album.coverUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivAlbumCover)
        }
    }
}