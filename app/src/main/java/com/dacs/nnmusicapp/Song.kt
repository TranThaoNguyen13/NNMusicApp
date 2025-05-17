package com.dacs.nnmusicapp

import android.os.Parcel
import android.os.Parcelable

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val file_path: String?, // Sửa từ filepath thành file_path
    val quality: String?,
    val trendingScore: Int?, // Thêm trường trending_score
    val isRecommended: Boolean?, // Thêm trường is_recommend
    val thumbnailUrl: String?,
    val albumId: Int?,
    val lyrics: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString() ?: "",
        artist = parcel.readString() ?: "",
        file_path = parcel.readString() ?: "", // Thêm giá trị mặc định nếu null
        quality = parcel.readString() ?: "",   // Thêm giá trị mặc định nếu null
        trendingScore = parcel.readInt().let { if (it == -1) null else it }, // Đọc trending_score
        isRecommended = parcel.readByte().let { if (it == 0.toByte()) null else it == 1.toByte() }, // Đọc is_recommend
        thumbnailUrl = parcel.readString() ?: "", // Thêm giá trị mặc định nếu null
        albumId = parcel.readInt().let { if (it == -1) null else it }, // Đọc albumId
        lyrics = parcel.readString() ?: ""      // Thêm giá trị mặc định nếu null
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(file_path) // Sửa từ filepath thành file_path
        parcel.writeString(quality)
        parcel.writeInt(trendingScore ?: -1) // Ghi trending_score, -1 nếu null
        parcel.writeByte((if (isRecommended == null) 0 else if (isRecommended) 1 else 2).toByte()) // Ghi is_recommend
        parcel.writeString(thumbnailUrl)
        parcel.writeInt(albumId ?: -1) // Ghi albumId, -1 nếu null
        parcel.writeString(lyrics)
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