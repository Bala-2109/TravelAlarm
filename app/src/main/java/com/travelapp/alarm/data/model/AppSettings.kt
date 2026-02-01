package com.travelapp.alarm.data.model

/**
 * Performance modes for battery optimization
 */
enum class PerformanceMode {
    HIGH_PERFORMANCE,
    BALANCED,
    BATTERY_SAVER,
    ULTRA_SAVER
}

/**
 * Map style options
 */
enum class MapStyle {
    DEFAULT, SATELLITE, TERRAIN, HYBRID, DARK, LIGHT
}

/**
 * Distance and speed units
 */
enum class DistanceUnit {
    KILOMETERS, MILES, METERS
}

enum class SpeedUnit {
    KMH, MPH, MS
}

/**
 * Comprehensive app settings - Everything is customizable!
 */
data class AppSettings(
    // === CORE FEATURES ===
    val locationBasedAlarm: Boolean = true,
    val timeBasedAlarm: Boolean = true,
    val contactNotifications: Boolean = true,
    val liveLocationSharing: Boolean = true,
    val chatMessaging: Boolean = true,
    val checkpoints: Boolean = true,
    val majorLocationDetection: Boolean = true,
    val trajectoryPrediction: Boolean = true,
    val routeTracking: Boolean = true,
    val tripHistory: Boolean = true,

    // === NOTIFICATIONS ===
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val notificationLed: Boolean = true,

    val updateFrequencySeconds: Int = 30,
    val customFrequencyEnabled: Boolean = false,

    val smartNotificationsEnabled: Boolean = true,
    val notifyOnCheckpoints: Boolean = true,
    val notifyOnMajorLocations: Boolean = true,
    val notifyOnLowBattery: Boolean = true,
    val batteryThresholds: List<Int> = listOf(20, 15, 10, 5),
    val notifyOnRouteDeviation: Boolean = true,
    val routeDeviationThresholdMeters: Int = 500,
    val notifyOnSpeedChange: Boolean = true,

    val detectCities: Boolean = true,
    val detectTowns: Boolean = true,
    val detectVillages: Boolean = false,
    val detectStateBorders: Boolean = true,
    val minimumPopulation: Int = 50000,

    val whatsappEnabled: Boolean = true,
    val whatsappIncludeTrackingLink: Boolean = true,
    val whatsappAutoOpen: Boolean = true,
    val whatsappUseEmojis: Boolean = true,

    val smsEnabled: Boolean = true,
    val smsIncludeTrackingLink: Boolean = true,
    val smsShortenLinks: Boolean = true,

    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",

    // === LOCATION & TRACKING ===
    val locationTrackingEnabled: Boolean = true,
    val highAccuracyMode: Boolean = true,
    val backgroundTracking: Boolean = true,
    val minimumUpdateIntervalSeconds: Int = 30,
    val minimumDisplacementMeters: Int = 10,

    val geofencingEnabled: Boolean = true,
    val customRadiusEnabled: Boolean = true,
    val defaultAlarmRadius: Int = 500,
    val defaultNotificationRadius: Int = 200,

    val shareLiveLocation: Boolean = true,
    val shareLocationHistory: Boolean = true,
    val shareSpeed: Boolean = true,
    val shareBearing: Boolean = true,

    val offlineTrackingEnabled: Boolean = true,
    val cacheLocationsOffline: Boolean = true,
    val syncWhenOnline: Boolean = true,

    // === COMMUNICATION ===
    val chatEnabled: Boolean = true,
    val chatNotificationSound: Boolean = true,
    val typingIndicator: Boolean = true,
    val readReceipts: Boolean = true,
    val messageHistoryEnabled: Boolean = true,
    val messageHistoryDays: Int = 30,

    val quickResponsesEnabled: Boolean = true,
    val customQuickResponses: List<String> = listOf(
        "On my way!",
        "Running 10 minutes late",
        "Can't make it, sorry",
        "Where exactly?"
    ),

    val locationChangeNotifications: Boolean = true,
    val requireLocationChangeConfirmation: Boolean = true,
    val maxLocationChanges: Int = 10,
    val automaticLocationSuggestions: Boolean = true,

    val automaticCallingEnabled: Boolean = false,
    val callAttempts: Int = 2,
    val callIntervalSeconds: Int = 30,
    val callOnArrival: Boolean = false,
    val callOnCriticalBattery: Boolean = true,

    // === BATTERY & PERFORMANCE ===
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val batterySaverMode: Boolean = false,
    val autoEnableBatterySaver: Boolean = true,
    val batterySaverThreshold: Int = 20,
    val aggressiveBatterySaver: Boolean = false,
    val aggressiveBatterySaverThreshold: Int = 10,

    val reduceAccuracyOnLowBattery: Boolean = true,
    val reduceUpdatesOnLowBattery: Boolean = true,
    val lowBatteryUpdateInterval: Int = 120,
    val allowBackgroundExecution: Boolean = true,
    val wakeLockEnabled: Boolean = true,

    val batteryDrainPrediction: Boolean = true,
    val showBatteryEstimate: Boolean = true,
    val warnBeforeLowBattery: Boolean = true,
    val batteryWarningThreshold: Int = 25,

    // === PRIVACY & SECURITY ===
    val collectLocationHistory: Boolean = true,
    val collectAnalytics: Boolean = false,
    val collectCrashReports: Boolean = true,
    val autoDeleteHistory: Boolean = true,
    val historyRetentionDays: Int = 30,

    val requirePinToStart: Boolean = false,
    val requirePinToModify: Boolean = false,
    val biometricAuth: Boolean = false,
    val encryptLocalData: Boolean = false,

    // === DISPLAY & UI ===
    val darkMode: Boolean = false,
    val autoDarkMode: Boolean = true,
    val mapStyle: MapStyle = MapStyle.DEFAULT,
    val showTrafficLayer: Boolean = false,
    val showCheckpointMarkers: Boolean = true,
    val showRoutePolyline: Boolean = true,
    val animateMarkers: Boolean = true,

    val showSpeedometer: Boolean = true,
    val showBatteryPercentage: Boolean = true,
    val showETACountdown: Boolean = true,
    val showDistanceRemaining: Boolean = true,

    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,

    val language: String = "en",
    val autoDetectLanguage: Boolean = true,

    val lastModified: Long = System.currentTimeMillis(),
    val version: Int = 1
) {

    /**
     * Get estimated battery drain per hour
     */
    fun getEstimatedBatteryDrainPerHour(): Int {
        var drain = when (performanceMode) {
            PerformanceMode.HIGH_PERFORMANCE -> 15
            PerformanceMode.BALANCED -> 8
            PerformanceMode.BATTERY_SAVER -> 5
            PerformanceMode.ULTRA_SAVER -> 3
        }

        if (highAccuracyMode) drain += 2
        if (liveLocationSharing) drain += 1
        if (backgroundTracking) drain += 2

        if (batterySaverMode) drain = (drain * 0.7).toInt()

        return drain.coerceAtLeast(1)
    }

    /**
     * Check if a feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return when (feature) {
            "location_alarm" -> locationBasedAlarm
            "checkpoints" -> checkpoints
            "chat" -> chatMessaging
            "trajectory" -> trajectoryPrediction
            "major_locations" -> majorLocationDetection
            else -> true
        }
    }

    /**
     * Get update interval based on battery level
     */
    fun getUpdateInterval(batteryLevel: Int): Int {
        return when {
            batterySaverMode && batteryLevel <= batterySaverThreshold -> lowBatteryUpdateInterval
            aggressiveBatterySaver && batteryLevel <= aggressiveBatterySaverThreshold -> lowBatteryUpdateInterval * 2
            else -> updateFrequencySeconds
        }
    }
}