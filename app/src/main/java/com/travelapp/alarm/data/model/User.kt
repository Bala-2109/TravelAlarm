package com.travelapp.alarm.data.model

data class User(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,

    val profileImageUrl: String? = null,
    val bio: String? = null,

    val preferredNotificationMethod: NotificationMethod = NotificationMethod.WHATSAPP,
    val defaultSettings: AppSettings = AppSettings(),

    val savedContacts: MutableList<Contact> = mutableListOf(),
    val savedLocations: MutableList<PresetLocation> = mutableListOf(),
    val savedPresets: MutableList<ConfigurationPreset> = mutableListOf(),

    val tripHistory: MutableList<String> = mutableListOf(),

    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false
) {
    fun getDisplayName(): String {
        return name
    }

    fun getFormattedPhone(): String {
        return phoneNumber
    }

    fun addTripToHistory(tripId: String) {
        if (!tripHistory.contains(tripId)) {
            tripHistory.add(0, tripId)
        }
    }

    fun getRecentTrips(): List<String> {
        return tripHistory.take(10)
    }
}

data class PresetLocation(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,

    val defaultAlarmRadius: Float = 500f,
    val defaultNotificationRadius: Float = 200f,
    val defaultContacts: List<String> = emptyList(),
    val icon: String = "📍",

    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayText(): String {
        return "$icon $name"
    }
}

data class ConfigurationPreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val settings: AppSettings,
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val usageCount: Int = 0
) {
    companion object {
        fun getBuiltInPresets(): List<ConfigurationPreset> {
            return listOf(
                ConfigurationPreset(
                    id = "preset_train",
                    name = "Long Distance Train",
                    description = "High accuracy, frequent updates, all features",
                    icon = "🚄",
                    isBuiltIn = true,
                    settings = AppSettings(
                        performanceMode = PerformanceMode.BALANCED,
                        updateFrequencySeconds = 30,
                        notifyOnMajorLocations = true,
                        detectCities = true,
                        detectTowns = true
                    )
                ),
                ConfigurationPreset(
                    id = "preset_car",
                    name = "Short City Drive",
                    description = "High accuracy, minimal notifications",
                    icon = "🚗",
                    isBuiltIn = true,
                    settings = AppSettings(
                        performanceMode = PerformanceMode.HIGH_PERFORMANCE,
                        updateFrequencySeconds = 60,
                        notifyOnMajorLocations = false,
                        notifyOnCheckpoints = true
                    )
                ),
                ConfigurationPreset(
                    id = "preset_battery",
                    name = "Battery Saver",
                    description = "Minimal battery usage, essential features only",
                    icon = "🔋",
                    isBuiltIn = true,
                    settings = AppSettings(
                        performanceMode = PerformanceMode.BATTERY_SAVER,
                        updateFrequencySeconds = 120,
                        highAccuracyMode = false,
                        batterySaverMode = true
                    )
                ),
                ConfigurationPreset(
                    id = "preset_night",
                    name = "Night Travel",
                    description = "Dark mode, quiet notifications",
                    icon = "🌙",
                    isBuiltIn = true,
                    settings = AppSettings(
                        darkMode = true,
                        mapStyle = MapStyle.DARK,
                        notificationSound = false,
                        notificationVibration = true
                    )
                )
            )
        }
    }
}