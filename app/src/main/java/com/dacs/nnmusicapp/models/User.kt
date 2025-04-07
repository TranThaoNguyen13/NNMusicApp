package com.dacs.nnmusicapp.models

import android.os.Parcel
import android.os.Parcelable

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        username = parcel.readString() ?: "",
        email = parcel.readString() ?: "",
        role = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(role)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User = User(parcel)
        override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
    }
}