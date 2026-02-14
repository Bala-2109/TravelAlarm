package com.travelapp.alarm.data.model

enum class TripStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}

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

data class TripEvent(
    val type: TripEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val location: LatLng? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Float = 0f,
    val batteryLevel: Int = 100,
    val accuracy: Float? = null
)

data class LocationChange(
    val timestamp: Long,
    val oldLocation: LatLng,
    val oldLocationName: String,
    val newLocation: LatLng,
    val newLocationName: String,
    val reason: String?,
    val notifiedContacts: List<String>
)

data class Trip(
    val id: String,
    val name: String,
    val traveler: User,

    val startLocation: LatLng,
    val startLocationName: String,
    val originalDestination: LatLng,
    val originalDestinationName: String,
    var currentDestination: LatLng,
    var currentDestinationName: String,

    val alarmRadius: Float = 500f,
    val notificationRadius: Float = 200f,
    var alarmEnabled: Boolean = true,
    var alarmSound: String? = null,

    val pickupPeople: MutableList<Contact> = mutableListOf(),
    val checkpoints: MutableList<Checkpoint> = mutableListOf(),

    val locationHistory: MutableList<LocationPoint> = mutableListOf(),
    val majorLocationsPassed: MutableList<String> = mutableListOf(),
    val locationChanges: MutableList<LocationChange> = mutableListOf(),

    var status: TripStatus = TripStatus.PENDING,
    var currentLocation: LatLng? = null,
    var currentSpeed: Float = 0f,
    var batteryLevel: Int = 100,

    val createdAt: Long = System.currentTimeMillis(),
    var startedAt: Long? = null,
    var estimatedArrival: Long? = null,
    var completedAt: Long? = null,

    var liveLocationSharing: Boolean = true,
    var chatEnabled: Boolean = true,
    var trajectoryPrediction: Boolean = true,
    var autoNotifications: Boolean = true,

    val events: MutableList<TripEvent> = mutableListOf()
) {

    fun start() {
        status = TripStatus.ACTIVE
        startedAt = System.currentTimeMillis()
        traveler.addTripToHistory(id)
        addEvent(TripEventType.TRIP_STARTED, "${traveler.name} started trip from $startLocationName")
    }

    fun complete() {
        status = TripStatus.COMPLETED
        completedAt = System.currentTimeMillis()
        addEvent(TripEventType.TRIP_COMPLETED, "Arrived at $currentDestinationName")
    }

    fun cancel() {
        status = TripStatus.CANCELLED
        completedAt = System.currentTimeMillis()
        addEvent(TripEventType.TRIP_CANCELLED, "Trip cancelled")
    }

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

    fun getRemainingDistance(): Double? {
        return currentLocation?.distanceTo(currentDestination)
    }

    fun getProgressPercent(): Int {
        val current = currentLocation ?: return 0

        val totalDistance = startLocation.distanceTo(currentDestination)
        val traveled = startLocation.distanceTo(current)

        if (totalDistance == 0.0) return 0

        val progress = ((traveled / totalDistance) * 100.0).toInt()
        return progress.coerceIn(0, 100)
    }

    fun getETA(): Int? {
        val remainingDistance = getRemainingDistance() ?: return null

        val speed = if (currentSpeed > 0) currentSpeed.toDouble() else 15.0

        if (speed == 0.0) return null

        val etaSeconds = remainingDistance / speed
        return (etaSeconds / 60.0).toInt()
    }

    fun isDestinationReached(): Boolean {
        val current = currentLocation ?: return false
        val distance = current.distanceTo(currentDestination)
        return distance <= notificationRadius
    }

    fun getDurationMinutes(): Int? {
        val start = startedAt ?: return null
        val end = completedAt ?: System.currentTimeMillis()
        return ((end - start) / 60000).toInt()
    }

    fun getCheckpointsReachedCount(): Int {
        return checkpoints.count { it.hasBeenReached }
    }
}