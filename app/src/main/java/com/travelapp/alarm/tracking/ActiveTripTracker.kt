package com.travelapp.alarm.tracking

import android.content.Context
import android.location.Location
import android.util.Log
import com.travelapp.alarm.data.model.Checkpoint
import com.travelapp.alarm.data.model.LatLng
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

    private var onCheckpointReached: ((Checkpoint) -> Unit)? = null
    private var onDestinationReached: (() -> Unit)? = null
    private var onProgressUpdate: ((TripProgress) -> Unit)? = null

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
    fun updateLocation(location: Location) {
        val trip = activeTrip ?: return

        Log.d(TAG, "📍 Location update - Lat: ${location.latitude}, Lng: ${location.longitude}")

        // Check if at current checkpoint
        if (currentCheckpointIndex < trip.checkpoints.size) {
            val checkpoint = trip.checkpoints[currentCheckpointIndex]
            val distanceToCheckpoint = calculateDistance(
                location.latitude,
                location.longitude,
                checkpoint.location.latitude,
                checkpoint.location.longitude
            )

            Log.d(TAG, "📏 Distance to checkpoint ${currentCheckpointIndex + 1}: ${distanceToCheckpoint.toInt()}m")

            if (distanceToCheckpoint <= CHECKPOINT_RADIUS_METERS) {
                onCheckpointReachedInternal(checkpoint)
            }
        }

        // Check distance to final destination
        val distanceToDestination = calculateDistance(
            location.latitude,
            location.longitude,
            trip.currentDestination.latitude,
            trip.currentDestination.longitude
        )

        Log.d(TAG, "📏 Distance to destination: ${distanceToDestination.toInt()}m")

        if (distanceToDestination <= DESTINATION_RADIUS_METERS) {
            onDestinationReachedInternal()
        }

        // Send progress update
        val progress = TripProgress(
            name = trip.name,
            currentCheckpointIndex = currentCheckpointIndex,
            totalCheckpoints = trip.checkpoints.size,
            distanceToNextCheckpoint = if (currentCheckpointIndex < trip.checkpoints.size) {
                calculateDistance(
                    location.latitude,
                    location.longitude,
                    trip.checkpoints[currentCheckpointIndex].location.latitude,
                    trip.checkpoints[currentCheckpointIndex].location.longitude
                )
            } else {
                distanceToDestination
            },
            distanceToDestination = distanceToDestination,
            isAtCheckpoint = false,
            isAtDestination = distanceToDestination <= DESTINATION_RADIUS_METERS
        )

        onProgressUpdate?.invoke(progress)
    }

    private fun onCheckpointReachedInternal(checkpoint: Checkpoint) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ CHECKPOINT REACHED: ${checkpoint.name}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        onCheckpointReached?.invoke(checkpoint)
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
     * Get current trip progress
     */
    fun getCurrentProgress(): TripProgress? {
        val trip = activeTrip ?: return null

        return TripProgress(
            name = trip.name,
            currentCheckpointIndex = currentCheckpointIndex,
            totalCheckpoints = trip.checkpoints.size,
            distanceToNextCheckpoint = 0f,
            distanceToDestination = 0f,
            isAtCheckpoint = false,
            isAtDestination = false
        )
    }

    /**
     * Set callback for checkpoint reached
     */
    fun setOnCheckpointReached(callback: (Checkpoint) -> Unit) {
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
    fun setOnProgressUpdate(callback: (TripProgress) -> Unit) {
        onProgressUpdate = callback
    }

    /**
     * Get active trip
     */
    fun getActiveTrip(): Trip? = activeTrip
}

/**
 * Trip progress data
 */
data class TripProgress(
    val name: String,
    val currentCheckpointIndex: Int,
    val totalCheckpoints: Int,
    val distanceToNextCheckpoint: Float,
    val distanceToDestination: Float,
    val isAtCheckpoint: Boolean,
    val isAtDestination: Boolean
) {
    fun getNextCheckpointName(): String {
        return if (currentCheckpointIndex < totalCheckpoints) {
            "Checkpoint ${currentCheckpointIndex + 1}"
        } else {
            "Destination"
        }
    }

    fun getProgressPercentage(): Int {
        return if (totalCheckpoints > 0) {
            ((currentCheckpointIndex.toFloat() / totalCheckpoints) * 100).toInt()
        } else {
            0
        }
    }
}