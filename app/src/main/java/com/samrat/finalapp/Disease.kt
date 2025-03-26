package com.samrat.finalapp

import android.os.Parcel
import android.os.Parcelable

data class Disease(
    val name: String,
    val scientificName: String,
    val symptoms: List<String>,
    val management: List<String>,
    val alsoFoundIn: List<String>,
    val imageResId: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(scientificName)
        parcel.writeStringList(symptoms)
        parcel.writeStringList(management)
        parcel.writeStringList(alsoFoundIn)
        parcel.writeString(imageResId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Disease> {
        override fun createFromParcel(parcel: Parcel): Disease = Disease(parcel)
        override fun newArray(size: Int): Array<Disease?> = arrayOfNulls(size)
    }
}