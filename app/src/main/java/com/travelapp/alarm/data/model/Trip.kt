package com.travelapp.alarm.data.model

/**
 * Trip status states
 */
enum class TripStatus {
    PENDING,        // Created but not started
    ACTIVE,         // Currently tracking
    PAUSED,         // Temporarily paused
    COMPLETED,      // Successfully completed
    CANCELLED       // Cancelled by user
}

/**
 * Trip event types for history
 */
enum class TripEventType {
    TRIP_STARTED,
    CHECKPOINT_REACHED,
    MAJOR_LOCATION_ENTERED,
    LOW_BATTERY_WARNING,
    ROUTE_DEVIATION,
    SPEED_CHANGED,
    LOCATION_CHANGED,
    CONTACT_NOTIFIED,
    TRIP_COMPLETED,
    TRIP_CANCELLED
}

/**
 * Event that occurred during a trip
 */
data class TripEvent(
    val type: TripEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val location: LatLng? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents a point in location history
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Float = 0f,
    val batteryLevel: Int = 100,
    val accuracy: Float? = null
)

/**
 * Represents a destination change
 */
data class LocationChange(
    val timestamp: Long,
    val oldLocation: LatLng,
    val oldLocationName: String,
    val newLocation: LatLng,
    val newLocationName: String,
    val reason: String?,
    val notifiedContacts: List<String>
)

/**
 * Represents a complete journey from start to destination
 */
data class Trip(
    val id: String,

    // Traveler info - using User object
    val traveler: User,

    // Locations
    val startLocation: LatLng,
    val startLocationName: String,
    val originalDestination: LatLng,
    val originalDestinationName: String,
    var currentDestination: LatLng,
    var currentDestinationName: String,

    // Alarm settings
    val alarmRadius: Float = 500f,
    val notificationRadius: Float = 200f,
    var alarmEnabled: Boolean = true,
    var alarmSound: String? = null,

    // Contacts
    val pickupPeople: MutableList<Contact> = mutableListOf(),

    // Checkpoints
    val checkpoints: MutableList<Checkpoint> = mutableListOf(),

    // Route tracking
    val locationHistory: MutableList<LocationPoint> = mutableListOf(),
    val majorLocationsPassed: MutableList<String> = mutableListOf(),
    val locationChanges: MutableList<LocationChange> = mutableListOf(),

    // Status
    var status: TripStatus = TripStatus.PENDING,
    var currentLocation: LatLng? = null,
    var currentSpeed: Float = 0f,
    var batteryLevel: Int = 100,

    // Timing
    val createdAt: Long = System.currentTimeMillis(),
    var startedAt: Long? = null,
    var estimatedArrival: Long? = null,
    var completedAt: Long? = null,

    // Features toggles
    var liveLocationSharing: Boolean = true,
    var chatEnabled: Boolean = true,
    var trajectoryPrediction: Boolean = true,
    var autoNotifications: Boolean = true,

    // Events history
    val events: MutableList<TripEvent> = mutableListOf()
) {

    /**
     * Start the trip
     */
    fun start() {
        status = TripStatus.ACTIVE
        startedAt = System.currentTimeMillis()
        traveler.addTripToHistory(id)
        addEvent(TripEventType.TRIP_STARTED, "${traveler.name} started trip from $startLocationName")
    }

    /**
     * Complete the trip
     */
    fun complete() {
        status = TripStatus.COMPLETED
        completedAt = System.currentTimeMillis()
        addEvent(TripEventType.TRIP_COMPLETED, "Arrived at $currentDestinationName")
    }

    /**
     * Cancel the trip
     */
    fun cancel() {
        status = TripStatus.CANCELLED
        completedAt = System.currentTimeMillis()
        addEvent(TripEventType.TRIP_CANCELLED, "Trip cancelled")
    }

    /**
     * Update current location
     */
    fun updateLocation(location: LatLng, speed: Float, battery: Int) {
        currentLocation = location
        currentSpeed = speed
        batteryLevel = battery

        locationHistory.add(
            LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis(),
                speed = speed,
                batteryLevel = battery
            )
        )
    }

    /**
     * Change destination
     */
    fun changeDestination(
        newLocation: LatLng,
        newLocationName: String,
        reason: String? = null
    ) {
        val change = LocationChange(
            timestamp = System.currentTimeMillis(),
            oldLocation = currentDestination,
            oldLocationName = currentDestinationName,
            newLocation = newLocation,
            newLocationName = newLocationName,
            reason = reason,
            notifiedContacts = pickupPeople.map { it.id }
        )

        locationChanges.add(change)
        currentDestination = newLocation
        currentDestinationName = newLocationName

        addEvent(
            TripEventType.LOCATION_CHANGED,
            "Destination changed to $newLocationName",
            newLocation
        )
    }

    /**
     * Add event to history
     */
    fun addEvent(
        type: TripEventType,
        description: String,
        location: LatLng? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        events.add(
            TripEvent(
                type = type,
                timestamp = System.currentTimeMillis(),
                description = description,
                location = location,
                metadata = metadata
            )
        )
    }

    /**
     * Calculate remaining distance to destination
     */
    fun getRemainingDistance(): Double? {
        return currentLocation?.distanceTo(currentDestination)
    }

    /**
     * Calculate progress percentage (0-100)
     */
    fun getProgressPercent(): Int {
        val current = currentLocation ?: return 0

        val totalDistance = startLocation.distanceTo(currentDestination)
        val traveled = startLocation.distanceTo(current)

        if (totalDistance == 0.0) return 0

        val progress = ((traveled / totalDistance) * 100.0).toInt()
        return progress.coerceIn(0, 100)
    }

    /**
     * Get estimated time of arrival (ETA) in minutes
     */
    fun getETA(): Int? {
        val remainingDistance = getRemainingDistance() ?: return null

        val speed = if (currentSpeed > 0) currentSpeed.toDouble() else 15.0

        if (speed == 0.0) return null

        val etaSeconds = remainingDistance / speed
        return (etaSeconds / 60.0).toInt()
    }

    /**
     * Check if destination is reached
     */
    fun isDestinationReached(): Boolean {
        val current = currentLocation ?: return false
        val distance = current.distanceTo(currentDestination)
        return distance <= notificationRadius
    }

    /**
     * Get trip duration in minutes
     */
    fun getDurationMinutes(): Int? {
        val start = startedAt ?: return null
        val end = completedAt ?: System.currentTimeMillis()
        return ((end - start) / 60000).toInt()
    }

    /**
     * Get number of checkpoints reached
     */
    fun getCheckpointsReachedCount(): Int {
        return checkpoints.count { it.hasBeenReached }
    }
}