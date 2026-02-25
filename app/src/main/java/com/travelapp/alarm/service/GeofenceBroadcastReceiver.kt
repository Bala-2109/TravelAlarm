package com.travelapp.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.travelapp.alarm.AlarmActivity
import com.travelapp.alarm.manager.TripManager

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📡 GEOFENCE EVENT RECEIVED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "❌ Geofencing event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "✅ GEOFENCE ENTERED")

            // Get triggering geofences
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            if (triggeringGeofences != null && triggeringGeofences.isNotEmpty()) {
                for (geofence in triggeringGeofences) {
                    handleGeofenceEnter(context, geofence.requestId)
                }
            }
        }
    }

    private fun handleGeofenceEnter(context: Context, geofenceId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚨 GEOFENCE ENTERED: $geofenceId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Parse geofence ID to determine type
        val isDestination = geofenceId.endsWith("_destination")
        val tripId = geofenceId.substringBefore("_checkpoint").substringBefore("_destination")

        // Get trip details
        val tripManager = TripManager.getInstance(context)
        val trip = tripManager.getTripById(tripId)

        if (trip == null) {
            Log.e(TAG, "❌ Trip not found for ID: $tripId")
            return
        }

        // Determine checkpoint name
        val checkpointName = if (isDestination) {
            trip.currentDestinationName
        } else {
            val checkpointIndex = geofenceId.substringAfterLast("_").toIntOrNull() ?: 0
            if (checkpointIndex < trip.checkpoints.size) {
                trip.checkpoints[checkpointIndex].name
            } else {
                "Unknown Checkpoint"
            }
        }

        Log.d(TAG, "📍 Location: $checkpointName")
        Log.d(TAG, "🗺️ Trip: ${trip.tripName}")
        Log.d(TAG, "🎯 Type: ${if (isDestination) "DESTINATION" else "CHECKPOINT"}")

        // Launch alarm activity
        launchAlarmActivity(context, trip.tripName, checkpointName, isDestination)
    }

    private fun launchAlarmActivity(
        context: Context,
        tripName: String,
        locationName: String,
        isDestination: Boolean
    ) {
        Log.d(TAG, "🚨 Launching alarm activity...")

        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("TRIP_NAME", tripName)
            putExtra("LOCATION_NAME", locationName)
            putExtra("IS_DESTINATION", isDestination)
        }

        try {
            context.startActivity(intent)
            Log.d(TAG, "✅ Alarm activity launched")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch alarm: ${e.message}")
            e.printStackTrace()
        }
    }
}
