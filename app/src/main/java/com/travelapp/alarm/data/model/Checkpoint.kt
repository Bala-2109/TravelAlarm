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
    val radius: Float = 200f, // meters

    // Visual
    val icon: CheckpointIcon = CheckpointIcon.FLAG,
    val color: String = "#FF5722",

    // Behavior - customizable notifications
    val notifyOnEntry: Boolean = true,        // Master toggle
    val notifyContacts: Boolean = true,       // Notify pickup people
    val notifyTraveler: Boolean = true,       // Alert traveler
    val soundAlert: Boolean = true,
    val vibrate: Boolean = true,

    // Status
    var hasBeenReached: Boolean = false,
    var reachedAt: Long? = null,

    // Optional details
    val description: String? = null,
    val estimatedArrivalTime: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if a location is within this checkpoint's radius
     */
    fun isLocationInside(currentLocation: LatLng): Boolean {
        val distance = location.distanceTo(currentLocation)
        return distance <= radius
    }

    /**
     * Mark checkpoint as reached
     */
    fun markAsReached() {
        hasBeenReached = true
        reachedAt = System.currentTimeMillis()
    }

    /**
     * Get status text
     */
    fun getStatusText(): String {
        return when {
            hasBeenReached -> "✅ Reached"
            else -> "○ Pending"
        }
    }

    /**
     * Get formatted distance from current location
     */
    fun getDistanceText(currentLocation: LatLng): String {
        val distance = location.distanceTo(currentLocation)
        return when {
            distance < 1000 -> "${distance.toInt()} m"
            else -> String.format("%.1f km", distance / 1000)
        }
    }

    /**
     * Check if should trigger any notifications
     */
    fun shouldTriggerNotifications(): Boolean {
        return notifyOnEntry && (notifyContacts || notifyTraveler)
    }
}