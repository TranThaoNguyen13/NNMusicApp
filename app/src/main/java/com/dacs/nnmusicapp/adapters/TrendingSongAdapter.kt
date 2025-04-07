package com.dacs.nnmusicapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dacs.nnmusicapp.R
import com.dacs.nnmusicapp.databinding.ItemShowMoreBinding
import com.dacs.nnmusicapp.databinding.ItemTrendingSongBinding
import com.dacs.nnmusicapp.Song

class TrendingSongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isExpanded = false
    private val ITEM_TYPE_SONG = 0
    private val ITEM_TYPE_SHOW_MORE = 1

    override fun getItemViewType(position: Int): Int {
        return if (position == getVisibleItemCount() && songs.size > 3 && !isExpanded) {
            ITEM_TYPE_SHOW_MORE
        } else if (position == songs.size && isExpanded) {
            ITEM_TYPE_SHOW_MORE
        } else {
            ITEM_TYPE_SONG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_SONG) {
            val binding = ItemTrendingSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SongViewHolder(binding)
        } else {
            val binding = ItemShowMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ShowMoreViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SongViewHolder) {
            holder.bind(songs[position], position + 1)
        } else if (holder is ShowMoreViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return if (isExpanded) {
            songs.size + 1 // +1 cho nút "Thu gọn"
        } else if (songs.size <= 3) {
            songs.size
        } else {
            getVisibleItemCount() + 1 // +1 cho nút "Hiển thị thêm"
        }
    }

    private fun getVisibleItemCount(): Int {
        return if (songs.size > 3) 3 else songs.size
    }

    inner class SongViewHolder(private val binding: ItemTrendingSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, rank: Int) {
            binding.tvRank.text = rank.toString()
            binding.tvTitle.text = song.title
            binding.tvArtist.text = song.artist
            if (song.thumbnailUrl != null) {
                Glide.with(binding.ivThumbnail.context)
                    .load(song.thumbnailUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_music_note)
            }
            binding.root.setOnClickListener { onSongClick(song) }
        }
    }

    inner class ShowMoreViewHolder(private val binding: ItemShowMoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            if (isExpanded) {
                binding.ivShowMore.setImageResource(R.drawable.ic_collapse_less)
            } else {
                binding.ivShowMore.setImageResource(R.drawable.ic_expand_more)
            }
            binding.ivShowMore.setOnClickListener {
                isExpanded = !isExpanded
                notifyDataSetChanged()
            }
        }
    }
}