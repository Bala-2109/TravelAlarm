package com.travelapp.alarm.tracking

import android.content.Context
import android.location.Location
import android.util.Log
import com.travelapp.alarm.data.model.Checkpoint
import com.travelapp.alarm.data.model.Trip
import com.travelapp.alarm.manager.TripManager

/**
 * Tracks active trip progress
 * Calculates distances to checkpoints and destination
 * Detects when checkpoints are reached
 */
class ActiveTripTracker(private val context: Context) {

    private val tripManager = TripManager.getInstance(context)
    private var activeTrip: Trip? = null
    private var currentCheckpointIndex = 0

    // Callbacks
    private var onCheckpointReached: ((com.travelapp.alarm.data.model.Trip.Checkpoint, Int) -> Unit)? = null
    private var onDestinationReached: (() -> Unit)? = null
    private var onProgressUpdate: ((TripProgressInfo) -> Unit)? = null

    companion object {
        private const val TAG = "ActiveTripTracker"
        private const val CHECKPOINT_RADIUS_METERS = 100f
        private const val DESTINATION_RADIUS_METERS = 100f
    }

    /**
     * Start tracking active trip
     */
    fun startTracking() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🎯 STARTING TRIP TRACKING")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        activeTrip = tripManager.getActiveTrip()

        if (activeTrip == null) {
            Log.w(TAG, "⚠️ No active trip found")
            return
        }

        currentCheckpointIndex = 0

        Log.d(TAG, "✅ Tracking trip: ${activeTrip!!.name}")
        Log.d(TAG, "📍 Destination: ${activeTrip!!.originalDestinationName}")
        Log.d(TAG, "📌 Checkpoints: ${activeTrip!!.checkpoints.size}")
    }

    /**
     * Stop tracking
     */
    fun stopTracking() {
        Log.d(TAG, "🛑 Stopping trip tracking")
        activeTrip = null
        currentCheckpointIndex = 0
    }

    /**
     * Update current location and check progress
     */
    fun updateLocation(location: Location): TripProgressInfo? {
        val trip = activeTrip ?: return null

        Log.d(TAG, "📍 Updating location for trip: ${trip.name}")

        var distanceToNextCheckpoint = 0f
        var nextCheckpointName = "Destination"

        // Check if at current checkpoint
        if (currentCheckpointIndex < trip.checkpoints.size) {
            val checkpoint = trip.checkpoints[currentCheckpointIndex]
            distanceToNextCheckpoint = calculateDistance(
                location.latitude,
                location.longitude,
                checkpoint.latitude,
                checkpoint.longitude
            )

            nextCheckpointName = checkpoint.name

            Log.d(TAG, "📏 Distance to ${checkpoint.name}: ${distanceToNextCheckpoint.toInt()}m")

            // Check if reached checkpoint
            if (distanceToNextCheckpoint <= CHECKPOINT_RADIUS_METERS) {
                onCheckpointReachedInternal(checkpoint, currentCheckpointIndex)
            }
        }

        // Calculate distance to final destination
        val distanceToDestination = trip.currentDestination?.let {
            calculateDistance(
                location.latitude,
                location.longitude,
                it.latitude,
                it.longitude
            )
        } ?: 0f

        Log.d(TAG, "📏 Distance to destination: ${distanceToDestination.toInt()}m")

        // Check if reached destination
        if (distanceToDestination <= DESTINATION_RADIUS_METERS) {
            onDestinationReachedInternal()
        }

        // Create progress info
        val progressInfo = TripProgressInfo(
            tripName = trip.name,
            currentCheckpointIndex = currentCheckpointIndex,
            totalCheckpoints = trip.checkpoints.size,
            nextCheckpointName = nextCheckpointName,
            distanceToNextCheckpoint = distanceToNextCheckpoint,
            distanceToDestination = distanceToDestination,
            progressPercentage = calculateProgressPercentage()
        )

        // Notify listeners
        onProgressUpdate?.invoke(progressInfo)

        return progressInfo
    }

    private fun onCheckpointReachedInternal(checkpoint: com.travelapp.alarm.data.model.Trip.Checkpoint, index: Int) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ CHECKPOINT REACHED: ${checkpoint.name}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        onCheckpointReached?.invoke(checkpoint, index)
        currentCheckpointIndex++

        Log.d(TAG, "📊 Progress: $currentCheckpointIndex/${activeTrip!!.checkpoints.size} checkpoints")
    }

    private fun onDestinationReachedInternal() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🎯 DESTINATION REACHED!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        onDestinationReached?.invoke()

        // Mark trip as complete
        activeTrip?.let {
            tripManager.completeTrip(it.id)
        }
    }

    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    /**
     * Calculate progress percentage
     */
    private fun calculateProgressPercentage(): Int {
        val trip = activeTrip ?: return 0
        return if (trip.checkpoints.size > 0) {
            ((currentCheckpointIndex.toFloat() / trip.checkpoints.size) * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Get active trip
     */
    fun getActiveTrip(): Trip? = activeTrip

    /**
     * Set callback for checkpoint reached
     */
    fun setOnCheckpointReached(callback: (com.travelapp.alarm.data.model.Trip.Checkpoint, Int) -> Unit) {
        onCheckpointReached = callback
    }

    /**
     * Set callback for destination reached
     */
    fun setOnDestinationReached(callback: () -> Unit) {
        onDestinationReached = callback
    }

    /**
     * Set callback for progress updates
     */
    fun setOnProgressUpdate(callback: (TripProgressInfo) -> Unit) {
        onProgressUpdate = callback
    }
}

/**
 * Simple trip progress information
 */
data class TripProgressInfo(
    val tripName: String,
    val currentCheckpointIndex: Int,
    val totalCheckpoints: Int,
    val nextCheckpointName: String,
    val distanceToNextCheckpoint: Float,
    val distanceToDestination: Float,
    val progressPercentage: Int
) {
    fun formatDistanceToNextCheckpoint(): String {
        return if (distanceToNextCheckpoint < 1000) {
            "${distanceToNextCheckpoint.toInt()}m"
        } else {
            String.format("%.1fkm", distanceToNextCheckpoint / 1000)
        }
    }

    fun formatDistanceToDestination(): String {
        return if (distanceToDestination < 1000) {
            "${distanceToDestination.toInt()}m"
        } else {
            String.format("%.1fkm", distanceToDestination / 1000)
        }
    }
}
