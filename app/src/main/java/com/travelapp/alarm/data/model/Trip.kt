package com.travelapp.alarm.data.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trip(
    @get:Exclude
    var id: String = "",
    var tripName: String = "",
    var startLocation: String = "",
    var destination: String = "",
    var currentDestinationName: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var checkpoints: List<Checkpoint> = emptyList(),
    var traveler: Traveler = Traveler(),
    var pickupPeople: List<Contact> = emptyList(),
    var batteryLevel: Int = 100,
    var name: String = "",
    var originalDestinationName: String = "",
    var currentDestination: Checkpoint? = null,
    var enabled: Boolean = true,
) : Parcelable {
    @Parcelize
    data class Checkpoint(
        var name: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var isReached: Boolean = false,
        var checkpointName: String = ""
    ) : Parcelable

    fun getETA(): String? {
        // TODO: Implement ETA calculation
        return null
    }
}
