package com.travelapp.alarm.data.model

/**
 * Notification method for contacts
 */
enum class NotificationMethod {
    IN_APP,           // Has TravelAlarm app installed
    WHATSAPP,         // WhatsApp (semi-automatic)
    SMS,              // SMS (fully automatic)
    PHONE_CALL,       // Phone call
    EMAIL             // Email notification
}

/**
 * Notification frequency for non-app users
 */
enum class NotificationFrequency {
    EVERY_UPDATE,           // Every location update
    EVERY_3RD_UPDATE,       // Every 3rd update
    EVERY_5TH_UPDATE,       // Every 5th update
    CHECKPOINTS_ONLY,       // Only at checkpoints & major locations
    ARRIVAL_ONLY            // Only when arrived
}

/**
 * Represents a contact who will pick up the traveler
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,

    // App status
    val hasApp: Boolean = false,
    val fcmToken: String? = null,

    // Notification preferences
    val primaryMethod: NotificationMethod = NotificationMethod.WHATSAPP,
    val fallbackMethod: NotificationMethod? = NotificationMethod.SMS,
    val autoFallback: Boolean = true,

    // Method-specific settings
    val whatsappFrequency: NotificationFrequency = NotificationFrequency.CHECKPOINTS_ONLY,
    val smsFrequency: NotificationFrequency = NotificationFrequency.CHECKPOINTS_ONLY,
    val emailFrequency: NotificationFrequency = NotificationFrequency.ARRIVAL_ONLY,

    // Calling preferences
    val enableCalling: Boolean = false,
    val callAttempts: Int = 2,
    val callIntervalSeconds: Int = 30,
    val callOnArrival: Boolean = false,
    val callOnCriticalBattery: Boolean = true,

    // Priority (for calling order)
    val priority: Int = 0,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastNotified: Long? = null
) {
    /**
     * Check if this contact should be notified based on update counter
     */
    fun shouldNotifyOnUpdate(updateCounter: Int, method: NotificationMethod): Boolean {
        val frequency = when (method) {
            NotificationMethod.WHATSAPP -> whatsappFrequency
            NotificationMethod.SMS -> smsFrequency
            NotificationMethod.EMAIL -> emailFrequency
            else -> return true
        }

        return when (frequency) {
            NotificationFrequency.EVERY_UPDATE -> true
            NotificationFrequency.EVERY_3RD_UPDATE -> updateCounter % 3 == 0
            NotificationFrequency.EVERY_5TH_UPDATE -> updateCounter % 5 == 0
            NotificationFrequency.CHECKPOINTS_ONLY -> false
            NotificationFrequency.ARRIVAL_ONLY -> false
        }
    }

    /**
     * Get display name with phone number
     */
    fun getDisplayName(): String {
        return "$name ($phoneNumber)"
    }
}