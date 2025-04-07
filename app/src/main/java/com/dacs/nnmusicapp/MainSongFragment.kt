package com.dacs.nnmusicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class MainSongFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var ivThumbnail: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(
            songTitle: String,
            songArtist: String,
            songUrl: String,
            songThumbnail: String
        ): MainSongFragment {
            return MainSongFragment().apply {
                arguments = Bundle().apply {
                    putString("song_title", songTitle)
                    putString("song_artist", songArtist)
                    putString("song_url", songUrl)
                    putString("song_thumbnail", songThumbnail)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_song, container, false)

        // Ánh xạ các view
        ivThumbnail = view.findViewById(R.id.ivThumbnail)
        tvSongTitle = view.findViewById(R.id.tvSongTitle)
        tvSongArtist = view.findViewById(R.id.tvSongArtist)
        seekBar = view.findViewById(R.id.seekBar)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnNext = view.findViewById(R.id.btnNext)

        // Lấy dữ liệu từ arguments
        val songTitle = arguments?.getString("song_title") ?: "Unknown Title"
        val songArtist = arguments?.getString("song_artist") ?: "Unknown Artist"
        val songUrl = arguments?.getString("song_url")
        val songThumbnail = arguments?.getString("song_thumbnail")

        // Cập nhật giao diện
        tvSongTitle.text = songTitle
        tvSongArtist.text = songArtist
        Glide.with(this)
            .load(songThumbnail)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(ivThumbnail)

        // Kiểm tra URL bài hát
        if (songUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
            return view
        }

        // Phát nhạc
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(songUrl)
                prepare()
                start()
            }
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)

            // Cập nhật SeekBar và thời gian
            val duration = mediaPlayer?.duration ?: 0
            seekBar.max = duration
            tvTotalTime.text = formatTime(duration)
            tvCurrentTime.text = formatTime(0)
            updateSeekBar()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi phát nhạc: ${e.message}", Toast.LENGTH_LONG).show()
            requireActivity().finish()
            return view
        }

        // Thiết lập sự kiện cho các nút
        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer?.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnPrevious.setOnClickListener {
            Toast.makeText(requireContext(), "Không có bài hát trước đó", Toast.LENGTH_SHORT).show()
        }

        btnNext.setOnClickListener {
            Toast.makeText(requireContext(), "Không có bài hát tiếp theo", Toast.LENGTH_SHORT).show()
        }

        // Thiết lập SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        return view
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    seekBar.progress = currentPosition
                    tvCurrentTime.text = formatTime(currentPosition)
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1000)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}