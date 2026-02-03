package com.travelapp.alarm.data.model

/**
 * Icon types for checkpoints
 */
enum class CheckpointIcon {
    FLAG,
    STAR,
    HOME,
    STATION,
    AIRPORT,
    RESTAURANT,
    GAS_STATION,
    COFFEE,
    HOTEL,
    CUSTOM
}

/**
 * Represents a waypoint along the route
 */
data class Checkpoint(
    val id: String,
    val name: String,
    val location: LatLng,
    val radius: Float = 200f,

    val icon: CheckpointIcon = CheckpointIcon.FLAG,
    val color: String = "#FF5722",

    val notifyOnEntry: Boolean = true,
    val notifyContacts: Boolean = true,
    val notifyTraveler: Boolean = true,
    val soundAlert: Boolean = true,
    val vibrate: Boolean = true,

    var hasBeenReached: Boolean = false,
    var reachedAt: Long? = null,

    val description: String? = null,
    val estimatedArrivalTime: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    fun isLocationInside(currentLocation: LatLng): Boolean {
        val distance = location.distanceTo(currentLocation)
        return distance <= radius
    }

    fun markAsReached() {
        hasBeenReached = true
        reachedAt = System.currentTimeMillis()
    }

    fun getStatusText(): String {
        return when {
            hasBeenReached -> "✅ Reached"
            else -> "○ Pending"
        }
    }

    fun getDistanceText(currentLocation: LatLng): String {
        val distance = location.distanceTo(currentLocation)
        return when {
            distance < 1000 -> "${distance.toInt()} m"
            else -> String.format("%.1f km", distance / 1000)
        }
    }

    fun shouldTriggerNotifications(): Boolean {
        return notifyOnEntry && (notifyContacts || notifyTraveler)
    }
}