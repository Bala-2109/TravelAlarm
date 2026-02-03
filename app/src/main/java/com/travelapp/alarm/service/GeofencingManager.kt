package com.travelapp.alarm.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.travelapp.alarm.data.model.LatLng
import com.travelapp.alarm.data.model.Trip

class GeofencingManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val TAG = "GeofencingManager"

    companion object {
        const val GEOFENCE_DESTINATION_ALARM = "geofence_destination_alarm"
        const val GEOFENCE_DESTINATION_NOTIFY = "geofence_destination_notify"
        const val GEOFENCE_CHECKPOINT_PREFIX = "geofence_checkpoint_"
    }

    fun setupTripGeofences(trip: Trip) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 SETTING UP GEOFENCES FOR TRIP: ${trip.id}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val geofences = mutableListOf<Geofence>()

        val alarmGeofence = createGeofence(
            id = "${GEOFENCE_DESTINATION_ALARM}_${trip.id}",
            location = trip.currentDestination,
            radius = trip.alarmRadius,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        )
        geofences.add(alarmGeofence)
        Log.d(TAG, "✅ Created ALARM geofence: ${trip.currentDestination} (${trip.alarmRadius}m)")

        val notifyGeofence = createGeofence(
            id = "${GEOFENCE_DESTINATION_NOTIFY}_${trip.id}",
            location = trip.currentDestination,
            radius = trip.notificationRadius,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        )
        geofences.add(notifyGeofence)
        Log.d(TAG, "✅ Created NOTIFY geofence: ${trip.currentDestination} (${trip.notificationRadius}m)")

        trip.checkpoints.forEach { checkpoint ->
            if (checkpoint.notifyOnEntry && !checkpoint.hasBeenReached) {
                val checkpointGeofence = createGeofence(
                    id = "${GEOFENCE_CHECKPOINT_PREFIX}${checkpoint.id}",
                    location = checkpoint.location,
                    radius = checkpoint.radius,
                    transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
                )
                geofences.add(checkpointGeofence)
                Log.d(TAG, "✅ Created CHECKPOINT geofence: ${checkpoint.name} at ${checkpoint.location} (${checkpoint.radius}m)")
            }
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Total geofences to add: ${geofences.size}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        addGeofences(geofences, trip.id)
    }

    private fun createGeofence(
        id: String,
        location: LatLng,
        radius: Float,
        transitionTypes: Int = Geofence.GEOFENCE_TRANSITION_ENTER
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                location.latitude,
                location.longitude,
                radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5000)
            .build()
    }

    private fun addGeofences(geofences: List<Geofence>, tripId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔐 CHECKING PERMISSIONS...")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val hasFineLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "ACCESS_FINE_LOCATION: ${if (hasFineLocation) "✅ GRANTED" else "❌ DENIED"}")

        if (!hasFineLocation) {
            Log.e(TAG, "❌❌❌ Cannot add geofences - FINE_LOCATION permission not granted!")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "ACCESS_BACKGROUND_LOCATION: ${if (hasBackgroundLocation) "✅ GRANTED" else "⚠️ NOT GRANTED"}")
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ Permissions OK! Adding ${geofences.size} geofences...")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()

        Log.d(TAG, "📤 Sending geofences to Google Play Services...")

        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅✅✅ GEOFENCES SUCCESSFULLY ADDED!")
                Log.d(TAG, "   Trip ID: $tripId")
                Log.d(TAG, "   Count: ${geofences.size}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                geofences.forEachIndexed { index, geofence ->
                    Log.d(TAG, "   ${index + 1}. ${geofence.requestId}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌❌❌ FAILED TO ADD GEOFENCES!")
                Log.e(TAG, "   Error: ${e.message}")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                e.printStackTrace()
            }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "✅ All geofences removed")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove geofences: ${e.message}")
            }
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}