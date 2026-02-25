package com.travelapp.alarm.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.travelapp.alarm.data.model.Trip
import com.travelapp.alarm.manager.TripManager

class GeofencingManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val tripManager = TripManager.getInstance(context)

    companion object {
        private const val TAG = "GeofencingManager"
        private const val GEOFENCE_RADIUS_METERS = 100f
        private const val GEOFENCE_EXPIRATION_MILLIS = 24 * 60 * 60 * 1000L // 24 hours

        @Volatile
        private var instance: GeofencingManager? = null

        fun getInstance(context: Context): GeofencingManager {
            return instance ?: synchronized(this) {
                instance ?: GeofencingManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Create geofences for active trip
     */
    fun createGeofencesForTrip(trip: Trip): Boolean {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🎯 CREATING GEOFENCES FOR TRIP")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Trip: ${trip.tripName}")
        Log.d(TAG, "Checkpoints: ${trip.checkpoints.size}")

        // Check permissions
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ Location permission not granted")
            return false
        }

        val geofences = mutableListOf<Geofence>()

        // Create geofence for each checkpoint
        trip.checkpoints.forEachIndexed { index, checkpoint ->
            val geofence = Geofence.Builder()
                .setRequestId("${trip.id}_checkpoint_$index")
                .setCircularRegion(
                    checkpoint.latitude,
                    checkpoint.longitude,
                    GEOFENCE_RADIUS_METERS
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_MILLIS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            geofences.add(geofence)

            Log.d(TAG, "✅ Created geofence for: ${checkpoint.checkpointName}")
            Log.d(TAG, "   Lat: ${checkpoint.latitude}")
            Log.d(TAG, "   Lng: ${checkpoint.longitude}")
            Log.d(TAG, "   Radius: ${GEOFENCE_RADIUS_METERS}m")
        }

        // Create geofence for destination
        val destinationGeofence = Geofence.Builder()
            .setRequestId("${trip.id}_destination")
            .setCircularRegion(
                trip.latitude,
                trip.longitude,
                GEOFENCE_RADIUS_METERS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_MILLIS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        geofences.add(destinationGeofence)

        Log.d(TAG, "✅ Created geofence for: Destination")
        Log.d(TAG, "   Lat: ${trip.latitude}")
        Log.d(TAG, "   Lng: ${trip.longitude}")

        // Create geofencing request
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        // Add geofences
        try {
            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "✅ GEOFENCES ADDED SUCCESSFULLY!")
                    Log.d(TAG, "Total geofences: ${geofences.size}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add geofences: ${e.message}")
                    e.printStackTrace()
                }
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception: ${e.message}")
            return false
        }
    }

    /**
     * Remove all geofences for a trip
     */
    fun removeGeofencesForTrip(tripId: String) {
        Log.d(TAG, "🗑️ Removing geofences for trip: $tripId")

        val geofenceIds = mutableListOf<String>()

        // Remove all checkpoint geofences
        for (i in 0..20) { // Assume max 20 checkpoints
            geofenceIds.add("${tripId}_checkpoint_$i")
        }

        // Remove destination geofence
        geofenceIds.add("${tripId}_destination")

        geofencingClient.removeGeofences(geofenceIds)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Geofences removed successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove geofences: ${e.message}")
            }
    }

    /**
     * Remove all geofences
     */
    fun removeAllGeofences() {
        Log.d(TAG, "🗑️ Removing all geofences")

        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "✅ All geofences removed")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove geofences: ${e.message}")
            }
    }

    /**
     * Get pending intent for geofence broadcasts
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
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}