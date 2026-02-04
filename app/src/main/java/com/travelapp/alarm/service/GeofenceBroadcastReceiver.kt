package com.travelapp.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.travelapp.alarm.data.model.*

/**
 * Receives geofence transition events and triggers appropriate notifications
 */
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
                Log.d(TAG, "🚶 Geofence exit detected")
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown geofence transition: $geofenceTransition")
            }
        }
    }

    private fun handleGeofenceEnter(context: Context, geofenceId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🎯 GEOFENCE ENTERED: $geofenceId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        when {
            // Destination alarm zone
            geofenceId.contains(GeofencingManager.GEOFENCE_DESTINATION_ALARM) -> {
                Log.d(TAG, "🔔 DESTINATION ALARM ZONE REACHED!")

                // Trigger alarm
                AlarmHandler.triggerDestinationAlarm(context, geofenceId)

                // Send notifications to contacts
                sendContactNotifications(
                    context,
                    eventType = "DESTINATION_REACHED",
                    message = "🎯 Traveler has arrived at destination!"
                )
            }

            // Destination notification zone
            geofenceId.contains(GeofencingManager.GEOFENCE_DESTINATION_NOTIFY) -> {
                Log.d(TAG, "📍 DESTINATION NOTIFICATION ZONE REACHED!")

                // Trigger notification
                AlarmHandler.triggerDestinationNotification(context, geofenceId)

                // Send notifications to contacts
                sendContactNotifications(
                    context,
                    eventType = "DESTINATION_NEARBY",
                    message = "📍 Traveler is approaching destination!"
                )
            }

            // Checkpoint
            geofenceId.startsWith(GeofencingManager.GEOFENCE_CHECKPOINT_PREFIX) -> {
                val checkpointId = geofenceId.removePrefix(GeofencingManager.GEOFENCE_CHECKPOINT_PREFIX)
                Log.d(TAG, "✅ CHECKPOINT REACHED: $checkpointId")

                // Trigger checkpoint alarm
                AlarmHandler.triggerCheckpointAlarm(context, checkpointId)

                // Send notifications to contacts
                sendContactNotifications(
                    context,
                    eventType = "CHECKPOINT_REACHED",
                    message = "✅ Traveler passed checkpoint: $checkpointId"
                )
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown geofence type: $geofenceId")
            }
        }
    }

    /**
     * Send notifications to all contacts
     * In a real app, this would fetch the actual trip and contacts from database
     * For now, we'll create a sample notification
     */
    private fun sendContactNotifications(
        context: Context,
        eventType: String,
        message: String
    ) {
        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════╗")
        Log.d(TAG, "║   📢 SENDING CONTACT NOTIFICATIONS     ║")
        Log.d(TAG, "╚════════════════════════════════════════╝")
        Log.d(TAG, "Event: $eventType")
        Log.d(TAG, "Message: $message")
        Log.d(TAG, "")

        try {
            // Create notification manager
            val notificationManager = NotificationManager(context)

            // In a real app, fetch the current trip and contacts from database/repository
            // For now, create sample data to demonstrate the notification system

            val sampleUser = User(
                id = "user1",
                name = "Balas",
                phoneNumber = "+91 9876543210"
            )

            val sampleContact = Contact(
                id = "contact1",
                name = "Mom",
                phoneNumber = "+91 9876543210", // Replace with real phone number for testing
                primaryMethod = NotificationMethod.WHATSAPP,
                fallbackMethod = NotificationMethod.SMS,
                autoFallback = true
            )

            val sampleTrip = Trip(
                id = "trip1",
                traveler = sampleUser,
                startLocation = LatLng(12.9249, 80.1000),
                startLocationName = "Tambaram",
                originalDestination = LatLng(13.0827, 80.2707),
                originalDestinationName = "Home",
                currentDestination = LatLng(13.0827, 80.2707),
                currentDestinationName = "Home",
                pickupPeople = mutableListOf(sampleContact)
            )

            // Create custom message based on event type
            val customMessage = when (eventType) {
                "CHECKPOINT_REACHED" ->
                    "✅ ${sampleTrip.traveler.name} has passed a checkpoint on their way to ${sampleTrip.currentDestinationName}."
                "DESTINATION_NEARBY" ->
                    "📍 ${sampleTrip.traveler.name} is approaching ${sampleTrip.currentDestinationName}. Almost there!"
                "DESTINATION_REACHED" ->
                    "🎯 ${sampleTrip.traveler.name} has arrived at ${sampleTrip.currentDestinationName}!"
                else -> message
            }

            Log.d(TAG, "📱 Sending notifications to contacts...")

            // Send notification to each contact
            sampleTrip.pickupPeople.forEach { contact ->
                val success = notificationManager.notifyContact(
                    contact = contact,
                    message = customMessage,
                    trip = sampleTrip,
                    eventType = eventType
                )

                if (success) {
                    Log.d(TAG, "✅ Notified: ${contact.name} via ${contact.primaryMethod}")
                } else {
                    Log.w(TAG, "⚠️ Failed to notify: ${contact.name}")
                }
            }

            Log.d(TAG, "")
            Log.d(TAG, "✅ Contact notifications sent")
            Log.d(TAG, "")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notifications: ${e.message}")
            e.printStackTrace()
        }
    }
}