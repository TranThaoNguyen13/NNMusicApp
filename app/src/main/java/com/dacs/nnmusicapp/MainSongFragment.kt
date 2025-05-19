package com.dacs.nnmusicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

    private lateinit var ivThumbnail: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPrepared = false

    companion object {
        fun newInstance(songTitle: String, songArtist: String, songUrl: String, songThumbnail: String): MainSongFragment {
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

        ivThumbnail = view.findViewById(R.id.ivThumbnail)
        tvSongTitle = view.findViewById(R.id.tvSongTitle)
        tvSongArtist = view.findViewById(R.id.tvSongArtist)
        seekBar = view.findViewById(R.id.seekBar)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnNext = view.findViewById(R.id.btnNext)

        val songTitle = arguments?.getString("song_title") ?: "Unknown Title"
        val songArtist = arguments?.getString("song_artist") ?: "Unknown Artist"
        val songUrl = arguments?.getString("song_url") ?: ""
        val songThumbnail = arguments?.getString("song_thumbnail")

        tvSongTitle.text = songTitle
        tvSongArtist.text = songArtist
        Glide.with(this)
            .load(songThumbnail)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(ivThumbnail)

        Log.d("MainSongFragment", "Received songUrl: $songUrl")

        if (songUrl.isEmpty()) {
            Log.e("MainSongFragment", "songUrl is empty or null")
            Toast.makeText(requireContext(), "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
            return view
        }

        setupMediaPlayer(songUrl)
        setupButtons()

        return view
    }

    private fun setupMediaPlayer(songUrl: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(songUrl)
                setOnPreparedListener {
                    isPrepared = true
                    Log.d("MainSongFragment", "MediaPlayer prepared, starting playback")
                    start()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    val duration = duration
                    seekBar.max = duration
                    tvTotalTime.text = formatTime(duration)
                    tvCurrentTime.text = formatTime(0)
                    updateSeekBar()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MainSongFragment", "MediaPlayer Error: what=$what, extra=$extra")
                    isPrepared = false
                    when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                            if (extra == -2147483648) {
                                Log.e("MainSongFragment", "ENOENT: File not found at $songUrl")
                                Toast.makeText(context, "Không tìm thấy file bài hát", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("MainSongFragment", "Unknown error with extra: $extra")
                                Toast.makeText(context, "Lỗi không xác định: $extra", Toast.LENGTH_LONG).show()
                            }
                        }
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                            Log.e("MainSongFragment", "Server died")
                            Toast.makeText(context, "Lỗi server", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.e("MainSongFragment", "Other error: $what")
                            Toast.makeText(context, "Lỗi phát nhạc: $what", Toast.LENGTH_LONG).show()
                        }
                    }
                    mp.release()
                    mediaPlayer = null
                    true
                }
                setOnCompletionListener {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    isPrepared = false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainSongFragment", "Error setting up MediaPlayer: ${e.message}")
            Toast.makeText(requireContext(), "Lỗi tải bài hát: ${e.message}", Toast.LENGTH_LONG).show()
            mediaPlayer?.release()
            mediaPlayer = null
            requireActivity().finish()
        }
    }

    private fun setupButtons() {
        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else if (isPrepared) {
                mediaPlayer?.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                Toast.makeText(requireContext(), "Đang tải bài hát...", Toast.LENGTH_SHORT).show()
            }
        }

        btnPrevious.setOnClickListener {
            Toast.makeText(requireContext(), "Không có bài hát trước đó", Toast.LENGTH_SHORT).show()
        }

        btnNext.setOnClickListener {
            Toast.makeText(requireContext(), "Không có bài hát tiếp theo", Toast.LENGTH_SHORT).show()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isPrepared) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    if (isPrepared) {
                        val currentPosition = mediaPlayer?.currentPosition ?: 0
                        seekBar.progress = currentPosition
                        tvCurrentTime.text = formatTime(currentPosition)
                    }
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("MainSongFragment", "SeekBar update error: ${e.message}")
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