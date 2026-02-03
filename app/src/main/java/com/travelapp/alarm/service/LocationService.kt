package com.travelapp.alarm.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.travelapp.alarm.R

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val TAG = "LocationService"

    private var updateCounter = 0
    private var lastLocation: Location? = null

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 seconds
        private const val FASTEST_UPDATE_INTERVAL = 15000L // 15 seconds
        private const val MIN_DISTANCE_METERS = 10f
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 LOCATION SERVICE CREATED")
        Log.d(TAG, "   Update interval: ${LOCATION_UPDATE_INTERVAL / 1000} seconds")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateCounter++

                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val speed = if (location.hasSpeed()) location.speed else 0f
        val speedKmh = (speed * 3.6).toInt()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 LOCATION UPDATE #$updateCounter")
        Log.d(TAG, "   Coordinates: ${location.latitude}, ${location.longitude}")
        Log.d(TAG, "   Speed: $speedKmh km/h (${String.format("%.2f", speed)} m/s)")
        Log.d(TAG, "   Accuracy: ${location.accuracy}m")
        Log.d(TAG, "   Time: ${System.currentTimeMillis()}")

        lastLocation?.let { last ->
            val distance = last.distanceTo(location)
            Log.d(TAG, "   Distance from last: ${distance.toInt()}m")
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        lastLocation = location
        updateNotification(location, speedKmh)
    }

    private fun startLocationUpdates() {
        Log.d(TAG, "🎬 Starting location updates...")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ Location permission not granted!")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL) // 15 seconds minimum
            setMinUpdateDistanceMeters(MIN_DISTANCE_METERS) // 10 meters
            setWaitForAccurateLocation(false)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL) // Don't batch updates
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "✅ Location tracking started successfully!")
        Log.d(TAG, "   Update interval: ${LOCATION_UPDATE_INTERVAL / 1000} seconds")
        Log.d(TAG, "   Fastest interval: ${FASTEST_UPDATE_INTERVAL / 1000} seconds")
        Log.d(TAG, "   Min distance: ${MIN_DISTANCE_METERS.toInt()} meters")
        Log.d(TAG, "   Priority: High accuracy")
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("TravelAlarm Active")
        .setContentText("Tracking your location every 30 seconds...")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun updateNotification(location: Location, speedKmh: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TravelAlarm - Tracking")
            .setContentText("Speed: $speedKmh km/h | Updates: $updateCounter")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location tracking is active"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "🛑 Location service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}