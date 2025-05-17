package com.dacs.nnmusicapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2

class SongActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        viewPager = findViewById(R.id.viewPager)

        val songTitle = intent.getStringExtra("song_title") ?: "Unknown Title"
        val songArtist = intent.getStringExtra("song_artist") ?: "Unknown Artist"
        val songUrl = intent.getStringExtra("song_url")
        val songThumbnail = intent.getStringExtra("song_thumbnail")
        val songLyrics = intent.getStringExtra("song_lyrics")

        Log.d("SongActivity", "songUrl: $songUrl")
        if (songUrl.isNullOrEmpty()) {
            Log.e("SongActivity", "songUrl is null or empty!")
            Toast.makeText(this, "URL bài hát không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewPager(songTitle, songArtist, songUrl, songThumbnail, songLyrics)
    }

    private fun setupViewPager(
        songTitle: String,
        songArtist: String,
        songUrl: String?,
        songThumbnail: String?,
        songLyrics: String?
    ) {
        viewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return when (position) {
                    0 -> MainSongFragment.newInstance(songTitle, songArtist, songUrl ?: "", songThumbnail ?: "")
                    1 -> LyricsFragment().apply {
                        arguments = Bundle().apply {
                            putString("lyrics", songLyrics ?: "Lời bài hát chưa có sẵn")
                        }
                    }
                    else -> throw IllegalStateException("Unexpected position $position")
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}