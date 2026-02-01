package com.travelapp.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Receives geofence transition events
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "GeofenceReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }
        
        val geofenceTransition = geofencingEvent.geofenceTransition
        
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                handleGeofenceEnter(context, geofencingEvent)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                handleGeofenceExit(context, geofencingEvent)
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                handleGeofenceDwell(context, geofencingEvent)
            }
        }
    }
    
    private fun handleGeofenceEnter(context: Context, event: GeofencingEvent) {
        val triggeringGeofences = event.triggeringGeofences ?: return
        
        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId
            Log.d(TAG, "Entered geofence: $geofenceId")
            
            when {
                geofenceId.startsWith("alarm_") -> {
                    // Main alarm triggered
                    handleAlarmGeofence(context, geofenceId)
                }
                geofenceId.startsWith("checkpoint_") -> {
                    // Checkpoint reached
                    handleCheckpointGeofence(context, geofenceId)
                }
                geofenceId.startsWith("destination_") -> {
                    // Destination reached
                    handleDestinationGeofence(context, geofenceId)
                }
            }
        }
    }
    
    private fun handleGeofenceExit(context: Context, event: GeofencingEvent) {
        val triggeringGeofences = event.triggeringGeofences ?: return
        
        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Exited geofence: ${geofence.requestId}")
            // Could track when user leaves areas if needed
        }
    }
    
    private fun handleGeofenceDwell(context: Context, event: GeofencingEvent) {
        val triggeringGeofences = event.triggeringGeofences ?: return
        
        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Dwelling in geofence: ${geofence.requestId}")
            // User has been in the geofence for a while
        }
    }
    
    private fun handleAlarmGeofence(context: Context, geofenceId: String) {
        Log.d(TAG, "Alarm geofence triggered: $geofenceId")
        
        // Extract trip ID
        val tripId = geofenceId.removePrefix("alarm_")
        
        // TODO: Trigger alarm sound/vibration
        // TODO: Show notification
        // TODO: Wake up screen
        
        // Start alarm handler service
        val intent = Intent(context, AlarmHandler::class.java).apply {
            action = AlarmHandler.ACTION_ALARM_TRIGGERED
            putExtra(AlarmHandler.EXTRA_TRIP_ID, tripId)
            putExtra(AlarmHandler.EXTRA_TRIGGER_TYPE, "geofence_alarm")
        }
        
        context.startService(intent)
    }
    
    private fun handleCheckpointGeofence(context: Context, geofenceId: String) {
        Log.d(TAG, "Checkpoint geofence triggered: $geofenceId")
        
        // Extract trip ID and checkpoint ID
        // Format: checkpoint_tripId_checkpointId
        val parts = geofenceId.removePrefix("checkpoint_").split("_", limit = 2)
        if (parts.size == 2) {
            val tripId = parts[0]
            val checkpointId = parts[1]
            
            // TODO: Mark checkpoint as reached
            // TODO: Notify contacts if enabled
            // TODO: Show notification to traveler
            
            val intent = Intent(context, AlarmHandler::class.java).apply {
                action = AlarmHandler.ACTION_CHECKPOINT_REACHED
                putExtra(AlarmHandler.EXTRA_TRIP_ID, tripId)
                putExtra(AlarmHandler.EXTRA_CHECKPOINT_ID, checkpointId)
            }
            
            context.startService(intent)
        }
    }
    
    private fun handleDestinationGeofence(context: Context, geofenceId: String) {
        Log.d(TAG, "Destination geofence triggered: $geofenceId")
        
        val tripId = geofenceId.removePrefix("destination_")
        
        // TODO: Mark trip as completed
        // TODO: Notify contacts of arrival
        // TODO: Stop location tracking
        
        val intent = Intent(context, AlarmHandler::class.java).apply {
            action = AlarmHandler.ACTION_DESTINATION_REACHED
            putExtra(AlarmHandler.EXTRA_TRIP_ID, tripId)
        }
        
        context.startService(intent)
    }
}
