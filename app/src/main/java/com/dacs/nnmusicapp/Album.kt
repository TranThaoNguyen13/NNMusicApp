package com.dacs.nnmusicapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: Int,
    val title: String,
    val artist: String?,
    val coverUrl: String?
) : Parcelable