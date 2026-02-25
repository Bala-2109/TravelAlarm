package com.travelapp.alarm.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.travelapp.alarm.data.model.Contact
import com.travelapp.alarm.data.model.NotificationMethod
import com.travelapp.alarm.data.model.Trip

/**
 * Manages all external notifications (WhatsApp, SMS, Calls)
 */
class NotificationManager(private val context: Context) {

    private val TAG = "NotificationManager"

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    fun notifyContact(
        contact: Contact,
        message: String,
        trip: Trip,
        eventType: String
    ): Boolean {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📬 NOTIFYING CONTACT: ${contact.name}")
        Log.d(TAG, "   Event: $eventType")
        Log.d(TAG, "   Primary Method: ${contact.primaryMethod}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        var notificationSuccess = false

        notificationSuccess = when (contact.primaryMethod) {
            NotificationMethod.WHATSAPP -> sendWhatsApp(contact, message)
            NotificationMethod.SMS -> sendSMS(contact, message)
            NotificationMethod.PHONE_CALL -> makeCall(contact)
            NotificationMethod.IN_APP -> {
                Log.d(TAG, "   In-app notification (handled elsewhere)")
                true
            }
            else -> {
                Log.w(TAG, "   Unsupported primary method: ${contact.primaryMethod}")
                false
            }
        }

        if (!notificationSuccess && contact.autoFallback && contact.fallbackMethod != null) {
            Log.w(TAG, "⚠️ Primary method failed, trying fallback: ${contact.fallbackMethod}")

            notificationSuccess = when (contact.fallbackMethod) {
                NotificationMethod.SMS -> sendSMS(contact, message)
                NotificationMethod.WHATSAPP -> sendWhatsApp(contact, message)
                NotificationMethod.PHONE_CALL -> makeCall(contact)
                else -> false
            }
        }

        if (notificationSuccess) {
            Log.d(TAG, "✅ Contact notified successfully")
        } else {
            Log.e(TAG, "❌ Failed to notify contact")
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return notificationSuccess
    }

    private fun sendWhatsApp(contact: Contact, message: String): Boolean {
        Log.d(TAG, "📱 Attempting to send WhatsApp message...")

        try {
            if (!isWhatsAppInstalled()) {
                Log.e(TAG, "❌ WhatsApp is not installed")
                return false
            }

            val phoneNumber = contact.phoneNumber
                .replace("+", "")
                .replace(" ", "")
                .replace("-", "")

            Log.d(TAG, "   Phone: $phoneNumber")
            Log.d(TAG, "   Message: $message")

            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(whatsappIntent)

            Log.d(TAG, "✅ WhatsApp intent launched successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send WhatsApp: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            try {
                val pm = context.packageManager
                pm.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun sendSMS(contact: Contact, message: String): Boolean {
        Log.d(TAG, "📨 Attempting to send SMS...")

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "❌ SMS permission not granted")
                return false
            }

            val phoneNumber = contact.phoneNumber
            Log.d(TAG, "   Phone: $phoneNumber")
            Log.d(TAG, "   Message: $message")

            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "✅ SMS sent successfully (1 part)")
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                Log.d(TAG, "✅ SMS sent successfully (${parts.size} parts)")
            }

            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun makeCall(contact: Contact): Boolean {
        Log.d(TAG, "📞 Attempting to make call...")

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "❌ CALL_PHONE permission not granted")
                return false
            }

            val phoneNumber = contact.phoneNumber
            Log.d(TAG, "   Phone: $phoneNumber")

            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(callIntent)

            Log.d(TAG, "✅ Call initiated successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to make call: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun notifyAllContacts(
        trip: Trip,
        eventType: String,
        customMessage: String? = null
    ) {
        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════╗")
        Log.d(TAG, "║   📢 NOTIFYING ALL CONTACTS            ║")
        Log.d(TAG, "╚════════════════════════════════════════╝")
        Log.d(TAG, "Event: $eventType")
        Log.d(TAG, "Contacts: ${trip.pickupPeople.size}")
        Log.d(TAG, "")

        trip.pickupPeople.forEach { contact ->
            val message = customMessage ?: createMessage(trip, eventType)
            notifyContact(contact, message, trip, eventType)
        }

        Log.d(TAG, "")
        Log.d(TAG, "✅ All contacts notified")
        Log.d(TAG, "")
    }

    private fun createMessage(trip: Trip, eventType: String): String {
        return when (eventType) {
            "TRIP_STARTED" -> {
                "🚀 ${trip.traveler.name} has started their journey to ${trip.currentDestinationName}. " +
                        "Track their location in real-time!"
            }
            "CHECKPOINT_REACHED" -> {
                "✅ ${trip.traveler.name} has passed a checkpoint on their way to ${trip.currentDestinationName}."
            }
            "DESTINATION_NEARBY" -> {
                "📍 ${trip.traveler.name} is approaching ${trip.currentDestinationName}. " +
                        "Estimated arrival: ${trip.getETA() ?: "soon"}."
            }
            "DESTINATION_REACHED" -> {
                "🎯 ${trip.traveler.name} has arrived at ${trip.currentDestinationName}!"
            }
            "LOW_BATTERY" -> {
                "🔋 ${trip.traveler.name}'s phone battery is low (${trip.batteryLevel}%). " +
                        "They're currently traveling to ${trip.currentDestinationName}."
            }
            "LOCATION_CHANGED" -> {
                "🔄 ${trip.traveler.name} has changed their destination to ${trip.currentDestinationName}."
            }
            else -> {
                "📱 Update from ${trip.traveler.name}: Currently traveling to ${trip.currentDestinationName}."
            }
        }
    }

    fun shouldNotifyContact(contact: Contact, updateCounter: Int, eventType: String): Boolean {
        val importantEvents = listOf(
            "TRIP_STARTED",
            "DESTINATION_REACHED",
            "CHECKPOINT_REACHED",
            "LOW_BATTERY",
            "LOCATION_CHANGED"
        )

        if (eventType in importantEvents) {
            return true
        }

        return contact.shouldNotifyOnUpdate(updateCounter, contact.primaryMethod)
    }

    fun getNotificationStatus(): NotificationStatus {
        val whatsappInstalled = isWhatsAppInstalled()
        val smsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val callPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return NotificationStatus(
            whatsappAvailable = whatsappInstalled,
            smsAvailable = smsPermission,
            callAvailable = callPermission
        )
    }
}

data class NotificationStatus(
    val whatsappAvailable: Boolean,
    val smsAvailable: Boolean,
    val callAvailable: Boolean
) {
    fun isWhatsAppAvailable() = whatsappAvailable
    fun isSMSAvailable() = smsAvailable
    fun isCallAvailable() = callAvailable

    fun getAvailableMethods(): List<String> {
        val methods = mutableListOf<String>()
        if (whatsappAvailable) methods.add("WhatsApp")
        if (smsAvailable) methods.add("SMS")
        if (callAvailable) methods.add("Call")
        return methods
    }

    fun getAllAvailable() = whatsappAvailable && smsAvailable && callAvailable
    fun getNoneAvailable() = !whatsappAvailable && !smsAvailable && !callAvailable
}
