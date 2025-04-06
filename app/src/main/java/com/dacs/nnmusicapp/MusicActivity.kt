package com.dacs.nnmusicapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.SimpleExoPlayer

class MusicActivity : AppCompatActivity() {
    lateinit var playerView: PlayerView
    lateinit var exoPlayer: SimpleExoPlayer
    var isVip = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        playerView = findViewById(R.id.playerView)
        isVip = intent.getBooleanExtra("isVip", false)

        // Đặt URL nhạc dựa trên loại tài khoản (VIP hoặc không VIP)
        val songUrl = if (isVip) "http://yourserver.com/music_high.mp3" else "http://yourserver.com/music_low.mp3"

        // Tạo và cấu hình ExoPlayer
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        // Tạo MediaItem từ URL và thêm vào ExoPlayer
        val mediaItem = MediaItem.fromUri(songUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Giải phóng tài nguyên khi Activity bị hủy
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
