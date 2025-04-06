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
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class SongActivity : AppCompatActivity() {

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
    private lateinit var viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        // Thiết lập Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Hiển thị nút quay lại
        supportActionBar?.setDisplayShowTitleEnabled(false) // Ẩn tiêu đề mặc định

        // Ánh xạ các view
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
        val songTitle = intent.getStringExtra("song_title") ?: "Unknown Title"
        val songArtist = intent.getStringExtra("song_artist") ?: "Unknown Artist"
        val songUrl = intent.getStringExtra("song_url")
        val songThumbnail = intent.getStringExtra("song_thumbnail")
        val songLyrics = intent.getStringExtra("song_lyrics")

        // Cập nhật giao diện
        tvSongTitle.text = songTitle
        tvSongArtist.text = songArtist
        Glide.with(this)
            .load(songThumbnail)
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(ivThumbnail)

        // Thiết lập ViewPager để hiển thị lời bài hát
        setupViewPager(songLyrics)

        // Kiểm tra URL bài hát
        if (songUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy URL bài hát", Toast.LENGTH_SHORT).show()
            finish()
            return
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
            Log.d("SongActivity", "MediaPlayer duration: $duration")
            seekBar.max = duration
            tvTotalTime.text = formatTime(duration)
            tvCurrentTime.text = formatTime(0) // Khởi tạo thời gian hiện tại là 0
            updateSeekBar()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SongActivity", "Error playing music: ${e.message}")
            Toast.makeText(this, "Lỗi phát nhạc: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
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
            Toast.makeText(this, "Không có bài hát trước đó", Toast.LENGTH_SHORT).show()
        }

        btnNext.setOnClickListener {
            Toast.makeText(this, "Không có bài hát tiếp theo", Toast.LENGTH_SHORT).show()
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
    }

    private fun setupViewPager(lyrics: String?) {
        viewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 1

            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return LyricsFragment().apply {
                    arguments = Bundle().apply {
                        putString("lyrics", lyrics ?: "Lời bài hát chưa có sẵn")
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

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    Log.d("SongActivity", "Current position: $currentPosition")
                    seekBar.progress = currentPosition
                    tvCurrentTime.text = formatTime(currentPosition)
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SongActivity", "Error updating SeekBar: ${e.message}")
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}