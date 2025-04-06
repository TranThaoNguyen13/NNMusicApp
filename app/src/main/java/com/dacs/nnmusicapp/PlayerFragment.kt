package com.dacs.nnmusicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class PlayerFragment : Fragment() {

    private var tvSongTitle: TextView? = null
    private var tvSongArtist: TextView? = null
    private var ivSongThumbnail: ImageView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var shouldUpdateSongInfo: Boolean = false // Biến để kiểm tra xem có cần cập nhật UI không

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)

        tvSongTitle = view.findViewById(R.id.tvSongTitle)
        tvSongArtist = view.findViewById(R.id.tvSongArtist)
        ivSongThumbnail = view.findViewById(R.id.ivSongThumbnail)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Kiểm tra nếu có dữ liệu cần cập nhật
        if (shouldUpdateSongInfo || arguments != null) {
            updateSongInfo()
        }
    }

    fun updateSongInfo() {
        val songTitle = arguments?.getString("song_title")
        val songArtist = arguments?.getString("song_artist")
        val songUrl = arguments?.getString("song_url")
        val songThumbnail = arguments?.getString("song_thumbnail")

        Log.d("PlayerFragment", "Received song_url: $songUrl")

        if (songUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra xem các view đã được khởi tạo chưa
        if (tvSongTitle == null || tvSongArtist == null || ivSongThumbnail == null) {
            Log.d("PlayerFragment", "Views not initialized, deferring update")
            shouldUpdateSongInfo = true // Đánh dấu để cập nhật sau
            return
        }

        tvSongTitle?.text = songTitle ?: "Unknown Title"
        tvSongArtist?.text = songArtist ?: "Unknown Artist"
        Glide.with(this)
            .load(songThumbnail)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(ivSongThumbnail!!)

        // Phát nhạc với songUrl
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(songUrl)
                prepare()
                start()
            }
            Log.d("PlayerFragment", "Music started playing: $songUrl")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlayerFragment", "Error playing music: ${e.message}")
            Toast.makeText(requireContext(), "Lỗi phát nhạc: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        tvSongTitle = null
        tvSongArtist = null
        ivSongThumbnail = null
        shouldUpdateSongInfo = false
    }
}