package com.travelapp.alarm.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.travelapp.alarm.data.model.Checkpoint
import com.travelapp.alarm.data.model.LatLng
import com.travelapp.alarm.data.model.Trip

/**
 * Manages geofences for location-based alarms and checkpoints
 */
class GeofencingManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GeofencingManager"
        const val GEOFENCE_EXPIRATION_DURATION = 12 * 60 * 60 * 1000L // 12 hours
        
        // Geofence IDs
        private const val ALARM_GEOFENCE_PREFIX = "alarm_"
        private const val CHECKPOINT_GEOFENCE_PREFIX = "checkpoint_"
        private const val DESTINATION_GEOFENCE_PREFIX = "destination_"
    }
    
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val activeGeofences = mutableSetOf<String>()
    
    /**
     * Set up geofences for a trip
     */
    @SuppressLint("MissingPermission")
    fun setupTripGeofences(trip: Trip) {
        Log.d(TAG, "Setting up geofences for trip: ${trip.id}")
        
        // Clear existing geofences for this trip
        clearTripGeofences(trip.id)
        
        val geofences = mutableListOf<Geofence>()
        
        // 1. Main alarm geofence (larger radius)
        geofences.add(createGeofence(
            id = "${ALARM_GEOFENCE_PREFIX}${trip.id}",
            location = trip.currentDestination,
            radius = trip.alarmRadius,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        ))
        
        // 2. Destination notification geofence (smaller radius)
        geofences.add(createGeofence(
            id = "${DESTINATION_GEOFENCE_PREFIX}${trip.id}",
            location = trip.currentDestination,
            radius = trip.notificationRadius,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        ))
        
        // 3. Checkpoint geofences
        trip.checkpoints.forEach { checkpoint ->
            if (checkpoint.notifyOnEntry && !checkpoint.hasBeenReached) {
                geofences.add(createCheckpointGeofence(trip.id, checkpoint))
            }
        }
        
        // Add geofences
        if (geofences.isNotEmpty()) {
            addGeofences(geofences)
        }
        
        Log.d(TAG, "Set up ${geofences.size} geofences for trip ${trip.id}")
    }
    
    /**
     * Create a geofence for a checkpoint
     */
    private fun createCheckpointGeofence(tripId: String, checkpoint: Checkpoint): Geofence {
        return createGeofence(
            id = "${CHECKPOINT_GEOFENCE_PREFIX}${tripId}_${checkpoint.id}",
            location = checkpoint.location,
            radius = checkpoint.radius,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        )
    }
    
    /**
     * Create a geofence
     */
    private fun createGeofence(
        id: String,
        location: LatLng,
        radius: Float,
        transitionTypes: Int
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                location.latitude,
                location.longitude,
                radius
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_DURATION)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5000) // 5 seconds
            .build()
    }
    
    /**
     * Add geofences to the system
     */
    @SuppressLint("MissingPermission")
    private fun addGeofences(geofences: List<Geofence>) {
        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
        
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener {
                geofences.forEach { activeGeofences.add(it.requestId) }
                Log.d(TAG, "Successfully added ${geofences.size} geofences")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add geofences", e)
            }
    }
    
    /**
     * Remove geofences for a trip
     */
    fun clearTripGeofences(tripId: String) {
        val geofenceIds = activeGeofences.filter { 
            it.contains(tripId) 
        }
        
        if (geofenceIds.isNotEmpty()) {
            geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener {
                    activeGeofences.removeAll(geofenceIds.toSet())
                    Log.d(TAG, "Removed ${geofenceIds.size} geofences for trip $tripId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to remove geofences for trip $tripId", e)
                }
        }
    }
    
    /**
     * Remove all geofences
     */
    fun clearAllGeofences() {
        if (activeGeofences.isNotEmpty()) {
            geofencingClient.removeGeofences(activeGeofences.toList())
                .addOnSuccessListener {
                    Log.d(TAG, "Removed all ${activeGeofences.size} geofences")
                    activeGeofences.clear()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to remove all geofences", e)
                }
        }
    }
    
    /**
     * Update destination geofences (when user changes destination)
     */
    @SuppressLint("MissingPermission")
    fun updateDestinationGeofences(trip: Trip) {
        Log.d(TAG, "Updating destination geofences for trip: ${trip.id}")
        
        // Remove old destination geofences
        val oldGeofenceIds = listOf(
            "${ALARM_GEOFENCE_PREFIX}${trip.id}",
            "${DESTINATION_GEOFENCE_PREFIX}${trip.id}"
        )
        
        geofencingClient.removeGeofences(oldGeofenceIds)
            .addOnSuccessListener {
                activeGeofences.removeAll(oldGeofenceIds.toSet())
                
                // Add new destination geofences
                val newGeofences = listOf(
                    createGeofence(
                        id = "${ALARM_GEOFENCE_PREFIX}${trip.id}",
                        location = trip.currentDestination,
                        radius = trip.alarmRadius,
                        transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
                    ),
                    createGeofence(
                        id = "${DESTINATION_GEOFENCE_PREFIX}${trip.id}",
                        location = trip.currentDestination,
                        radius = trip.notificationRadius,
                        transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
                    )
                )
                
                addGeofences(newGeofences)
            }
    }
    
    /**
     * Add a checkpoint geofence
     */
    @SuppressLint("MissingPermission")
    fun addCheckpointGeofence(tripId: String, checkpoint: Checkpoint) {
        val geofence = createCheckpointGeofence(tripId, checkpoint)
        addGeofences(listOf(geofence))
    }
    
    /**
     * Remove a checkpoint geofence
     */
    fun removeCheckpointGeofence(tripId: String, checkpointId: String) {
        val geofenceId = "${CHECKPOINT_GEOFENCE_PREFIX}${tripId}_${checkpointId}"
        
        geofencingClient.removeGeofences(listOf(geofenceId))
            .addOnSuccessListener {
                activeGeofences.remove(geofenceId)
                Log.d(TAG, "Removed checkpoint geofence: $geofenceId")
            }
    }
    
    /**
     * Get the PendingIntent for geofence transitions
     */
    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    
    /**
     * Get active geofence count
     */
    fun getActiveGeofenceCount(): Int = activeGeofences.size
    
    /**
     * Check if a geofence is active
     */
    fun isGeofenceActive(geofenceId: String): Boolean = activeGeofences.contains(geofenceId)
}
