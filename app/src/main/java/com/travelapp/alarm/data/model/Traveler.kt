package com.travelapp.alarm.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Traveler(
    val name: String = ""
) : Parcelable