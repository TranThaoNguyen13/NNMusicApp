package com.dacs.nnmusicapp

import android.os.Parcel
import android.os.Parcelable

data class Album(
    val id: Int,
    val title: String,
    val artist: String?,
    val coverUrl: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString() ?: "", // title không nullable, nhưng vẫn thêm để an toàn
        artist = parcel.readString() ?: "", // Xử lý null cho artist
        coverUrl = parcel.readString() ?: "" // Xử lý null cho coverUrl
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(coverUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Album> {
        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }
}