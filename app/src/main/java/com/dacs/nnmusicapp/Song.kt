package com.dacs.nnmusicapp

import android.os.Parcel
import android.os.Parcelable

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val url: String?,
    val quality: String?,
    val thumbnailUrl: String?,
    val albumId: Int,
    val lyrics: String? // Thêm trường lyrics
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString() ?: "",
        artist = parcel.readString() ?: "",
        url = parcel.readString(),
        quality = parcel.readString(),
        thumbnailUrl = parcel.readString(),
        albumId = parcel.readInt(),
        lyrics = parcel.readString() // Đọc lyrics từ Parcel
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(url)
        parcel.writeString(quality)
        parcel.writeString(thumbnailUrl)
        parcel.writeInt(albumId)
        parcel.writeString(lyrics) // Ghi lyrics vào Parcel
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song {
            return Song(parcel)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}