package com.dacs.nnmusicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var ivThumbnail: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var viewPager: ViewPager2

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var songs: List<Song>
    private var currentSongPosition: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        // Khởi tạo các view
        ivThumbnail = findViewById(R.id.ivThumbnail)
        tvSongTitle = findViewById(R.id.tvSongTitle)
        tvSongArtist = findViewById(R.id.tvSongArtist)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        viewPager = findViewById(R.id.viewPager)

        // Lấy dữ liệu từ Intent
        songs = intent.getParcelableArrayListExtra("songs") ?: emptyList()
        currentSongPosition = intent.getIntExtra("selected_song_position", 0)

        if (songs.isEmpty()) {
            Log.e("MusicPlayerActivity", "No songs available")
            Toast.makeText(this, "Không có bài hát để phát", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Thiết lập ViewPager để hiển thị lời bài hát
        setupViewPager()

        // Phát bài hát đầu tiên
        playSong(currentSongPosition)

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
            playPreviousSong()
        }

        btnNext.setOnClickListener {
            playNextSong()
        }

        // Thiết lập SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Cập nhật SeekBar và thời gian
        updateSeekBar()
    }

    private fun setupViewPager() {
        viewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 1

            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return LyricsFragment().apply {
                    arguments = Bundle().apply {
                        putString("lyrics", songs[currentSongPosition].lyrics ?: "Lời bài hát không có sẵn")
                    }
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPager.visibility = View.VISIBLE
            }
        })
    }

    private fun playSong(position: Int) {
        currentSongPosition = position
        val song = songs[currentSongPosition]

        tvSongTitle.text = song.title
        tvSongArtist.text = song.artist
        Glide.with(this)
            .load(song.thumbnailUrl)
            .thumbnail(0.25f)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(ivThumbnail)

        try {
            mediaPlayer?.release()
            if (song.file_path.isNullOrEmpty()) {
                throw IllegalArgumentException("Đường dẫn bài hát không hợp lệ")
            }
            var adjustedUrl = song.file_path
            adjustedUrl = adjustedUrl.replace("127.0.0.1", "10.0.2.2")
            if (!adjustedUrl.contains(":8000")) {
                adjustedUrl = adjustedUrl.replace("10.0.2.2", "10.0.2.2:8000")
            }
            if (adjustedUrl.startsWith("https://")) {
                adjustedUrl = adjustedUrl.replace("https://", "http://")
            }
            Log.d("MusicPlayerActivity", "Attempting to play from URL: $adjustedUrl")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(adjustedUrl)
                setOnPreparedListener {
                    Log.d("MusicPlayerActivity", "MediaPlayer prepared, starting playback")
                    start()
                    seekBar.max = duration
                    tvTotalTime.text = formatTime(duration)
                    updateSeekBar()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MusicPlayerActivity", "MediaPlayer Error: what=$what, extra=$extra")
                    when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                            if (extra == -2147483648) {
                                Log.e("MusicPlayerActivity", "ENOENT: File not found at $adjustedUrl")
                                Toast.makeText(this@MusicPlayerActivity, "Không tìm thấy file bài hát", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("MusicPlayerActivity", "Unknown error with extra: $extra")
                                Toast.makeText(this@MusicPlayerActivity, "Lỗi không xác định: $extra", Toast.LENGTH_LONG).show()
                            }
                        }
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                            Log.e("MusicPlayerActivity", "Server died")
                            Toast.makeText(this@MusicPlayerActivity, "Lỗi server", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.e("MusicPlayerActivity", "Other error: $what")
                            Toast.makeText(this@MusicPlayerActivity, "Lỗi phát nhạc: $what", Toast.LENGTH_LONG).show()
                        }
                    }
                    false
                }
                setOnCompletionListener {
                    Log.d("MusicPlayerActivity", "Playback completed")
                    playNextSong()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MusicPlayerActivity", "Error playing music: ${e.message}")
            Toast.makeText(this, "Lỗi phát nhạc: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun playPreviousSong() {
        if (currentSongPosition > 0) {
            currentSongPosition--
            playSong(currentSongPosition)
        }
    }

    private fun playNextSong() {
        if (currentSongPosition < songs.size - 1) {
            currentSongPosition++
            playSong(currentSongPosition)
        }
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
                    Log.e("MusicPlayerActivity", "SeekBar update error: ${e.message}")
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}