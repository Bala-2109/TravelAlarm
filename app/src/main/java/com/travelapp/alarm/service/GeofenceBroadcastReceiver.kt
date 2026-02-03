package com.travelapp.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📩 BROADCAST RECEIVED!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "❌ GeofencingEvent is NULL!")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ GEOFENCING ERROR: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.e(TAG, "❌ No triggering geofences")
            return
        }

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggeringGeofences.forEach { geofence ->
                    handleGeofenceEnter(context, geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Geofence exit detected")
            }
            else -> {
                Log.w(TAG, "Unknown geofence transition: $geofenceTransition")
            }
        }
    }

    private fun handleGeofenceEnter(context: Context, geofenceId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🎯 GEOFENCE ENTERED: $geofenceId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        when {
            geofenceId.contains(GeofencingManager.GEOFENCE_DESTINATION_ALARM) -> {
                Log.d(TAG, "🔔 DESTINATION ALARM ZONE REACHED!")
                AlarmHandler.triggerDestinationAlarm(context, geofenceId)
            }

            geofenceId.contains(GeofencingManager.GEOFENCE_DESTINATION_NOTIFY) -> {
                Log.d(TAG, "📍 DESTINATION NOTIFICATION ZONE REACHED!")
                AlarmHandler.triggerDestinationNotification(context, geofenceId)
            }

            geofenceId.startsWith(GeofencingManager.GEOFENCE_CHECKPOINT_PREFIX) -> {
                val checkpointId = geofenceId.removePrefix(GeofencingManager.GEOFENCE_CHECKPOINT_PREFIX)
                Log.d(TAG, "✅ CHECKPOINT REACHED: $checkpointId")
                AlarmHandler.triggerCheckpointAlarm(context, checkpointId)
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown geofence type: $geofenceId")
            }
        }
    }
}