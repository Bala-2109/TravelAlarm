package com.travelapp.alarm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.travelapp.alarm.MainActivity
import com.travelapp.alarm.R
import com.travelapp.alarm.data.model.LatLng
import com.travelapp.alarm.data.model.Trip

/**
 * Foreground service for background location tracking
 */
class LocationService : Service() {
    
    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"
        
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
        const val EXTRA_UPDATE_INTERVAL = "EXTRA_UPDATE_INTERVAL"
        
        // Default update intervals
        const val DEFAULT_UPDATE_INTERVAL = 30000L // 30 seconds
        const val FASTEST_UPDATE_INTERVAL = 10000L // 10 seconds
    }
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false
    private var currentTripId: String? = null
    private var updateInterval = DEFAULT_UPDATE_INTERVAL
    
    // Listeners for location updates
    private val locationListeners = mutableListOf<(Location) -> Unit>()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService created")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentTripId = intent.getStringExtra(EXTRA_TRIP_ID)
                updateInterval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL)
                startTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
            ACTION_UPDATE_INTERVAL -> {
                updateInterval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL)
                if (isTracking) {
                    // Restart tracking with new interval
                    stopLocationUpdates()
                    startLocationUpdates()
                }
            }
        }
        
        return START_STICKY // Restart service if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationService destroyed")
        stopLocationUpdates()
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
            }
        }
    }
    
    private fun startTracking() {
        Log.d(TAG, "Starting location tracking for trip: $currentTripId")
        
        val notification = createNotification("Tracking your journey...")
        startForeground(NOTIFICATION_ID, notification)
        
        isTracking = true
        startLocationUpdates()
    }
    
    private fun stopTracking() {
        Log.d(TAG, "Stopping location tracking")
        
        isTracking = false
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                updateInterval
            ).apply {
                setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                setMinUpdateDistanceMeters(10f) // Update if moved 10 meters
                setWaitForAccurateLocation(true)
            }.build()
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            Log.d(TAG, "Location updates started (interval: ${updateInterval}ms)")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for location updates", e)
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }
    
    private fun handleLocationUpdate(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        
        Log.d(TAG, "Location update: $latLng, Accuracy: ${location.accuracy}m, Speed: ${location.speed}m/s")
        
        // Update notification
        updateNotification(latLng, location.speed, location.accuracy)
        
        // Notify all listeners
        locationListeners.forEach { listener ->
            listener(location)
        }
        
        // TODO: Update trip in database/Firebase
        // TODO: Check geofences
        // TODO: Update battery level
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound
            ).apply {
                description = "Ongoing location tracking for active trips"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TravelAlarm Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Can't be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(location: LatLng, speed: Float, accuracy: Float) {
        val speedKmh = (speed * 3.6).toInt() // m/s to km/h
        val contentText = "Speed: ${speedKmh} km/h • Accuracy: ${accuracy.toInt()}m"
        
        val notification = createNotification(contentText)
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Add a listener for location updates
     */
    fun addLocationListener(listener: (Location) -> Unit) {
        locationListeners.add(listener)
    }
    
    /**
     * Remove a location listener
     */
    fun removeLocationListener(listener: (Location) -> Unit) {
        locationListeners.remove(listener)
    }
}
