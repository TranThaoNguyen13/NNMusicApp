package com.dacs.nnmusicapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LyricsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lyrics, container, false)

        val tvLyrics: TextView = view.findViewById(R.id.tvLyrics)
        val lyrics = arguments?.getString("lyrics") ?: "Lời bài hát chưa có sẵn"
        tvLyrics.text = lyrics

        return view
    }
}