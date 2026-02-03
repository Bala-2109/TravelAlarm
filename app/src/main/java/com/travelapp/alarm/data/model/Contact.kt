package com.travelapp.alarm.data.model

/**
 * Notification method for contacts
 */
enum class NotificationMethod {
    IN_APP,
    WHATSAPP,
    SMS,
    PHONE_CALL,
    EMAIL
}

/**
 * Notification frequency for non-app users
 */
enum class NotificationFrequency {
    EVERY_UPDATE,
    EVERY_3RD_UPDATE,
    EVERY_5TH_UPDATE,
    CHECKPOINTS_ONLY,
    ARRIVAL_ONLY
}

/**
 * Represents a contact who will pick up the traveler
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,

    val hasApp: Boolean = false,
    val fcmToken: String? = null,

    val primaryMethod: NotificationMethod = NotificationMethod.WHATSAPP,
    val fallbackMethod: NotificationMethod? = NotificationMethod.SMS,
    val autoFallback: Boolean = true,

    val whatsappFrequency: NotificationFrequency = NotificationFrequency.CHECKPOINTS_ONLY,
    val smsFrequency: NotificationFrequency = NotificationFrequency.CHECKPOINTS_ONLY,
    val emailFrequency: NotificationFrequency = NotificationFrequency.ARRIVAL_ONLY,

    val enableCalling: Boolean = false,
    val callAttempts: Int = 2,
    val callIntervalSeconds: Int = 30,
    val callOnArrival: Boolean = false,
    val callOnCriticalBattery: Boolean = true,

    val priority: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val lastNotified: Long? = null
) {
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

    fun getDisplayName(): String {
        return "$name ($phoneNumber)"
    }
}