package com.travelapp.alarm.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import com.travelapp.alarm.MainActivity
import com.travelapp.alarm.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_service_channel"

        // Location update settings
        private const val LOCATION_UPDATE_INTERVAL = 30000L  // 30 seconds
        private const val FASTEST_UPDATE_INTERVAL = 15000L   // 15 seconds
        private const val MIN_DISTANCE_METERS = 10f          // 10 meters

        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        var isRunning = false
            private set
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastLocation: Location? = null
    private var updateCount = 0
    private var startTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🟢 LocationService CREATED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📥 onStartCommand called")
        Log.d(TAG, "   Action: ${intent?.action ?: "NULL"}")

        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "▶️ Starting location service...")
                startLocationUpdates()
            }
            ACTION_STOP -> {
                Log.d(TAG, "⏹️ Stopping location service...")
                stopLocationUpdates()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun setupLocationCallback() {
        Log.d(TAG, "🔧 Setting up location callback...")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateCount++
                val currentTime = System.currentTimeMillis()
                val elapsedTime = (currentTime - startTime) / 1000

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📍 LOCATION UPDATE #$updateCount")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "📊 Location Details:")
                    Log.d(TAG, "   Latitude:  ${location.latitude}")
                    Log.d(TAG, "   Longitude: ${location.longitude}")
                    Log.d(TAG, "   Accuracy:  ${location.accuracy} meters")
                    Log.d(TAG, "   Provider:  ${location.provider}")
                    Log.d(TAG, "   Altitude:  ${location.altitude} meters")
                    Log.d(TAG, "   Speed:     ${location.speed} m/s")
                    Log.d(TAG, "   Bearing:   ${location.bearing}°")
                    Log.d(TAG, "   Time:      ${formatTime(location.time)}")

                    // Calculate distance from last location
                    lastLocation?.let { last ->
                        val distance = last.distanceTo(location)
                        Log.d(TAG, "   Distance from last: $distance meters")
                    }

                    Log.d(TAG, "⏱️ Timing Info:")
                    Log.d(TAG, "   Update interval: 30 seconds")
                    Log.d(TAG, "   Elapsed time: $elapsedTime seconds")
                    Log.d(TAG, "   Total updates: $updateCount")

                    serviceScope.launch {
                        handleLocationUpdate(location)
                    }

                    lastLocation = location
                } ?: run {
                    Log.e(TAG, "❌ Location is NULL!")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d(TAG, "📡 Location Availability Changed:")
                Log.d(TAG, "   Available: ${availability.isLocationAvailable}")
            }
        }

        Log.d(TAG, "✅ Location callback setup complete")
    }

    private fun startLocationUpdates() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 STARTING LOCATION UPDATES")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Check permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ LOCATION PERMISSION NOT GRANTED!")
            Log.e(TAG, "   Cannot start location updates without permission")
            return
        }

        Log.d(TAG, "✅ Location permission granted")

        // Create location request
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            setWaitForAccurateLocation(false)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL)
        }.build()

        Log.d(TAG, "📋 Location Request Settings:")
        Log.d(TAG, "   Priority: HIGH_ACCURACY")
        Log.d(TAG, "   Update Interval: $LOCATION_UPDATE_INTERVAL ms (30 sec)")
        Log.d(TAG, "   Fastest Interval: $FASTEST_UPDATE_INTERVAL ms (15 sec)")
        Log.d(TAG, "   Min Distance: $MIN_DISTANCE_METERS meters")

        // Start foreground service
        val notification = createNotification("Starting location tracking...")
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Started as foreground service")

        // Request location updates
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                isRunning = true
                startTime = System.currentTimeMillis()
                updateCount = 0

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ LOCATION UPDATES STARTED SUCCESSFULLY!")
                Log.d(TAG, "⏰ Waiting for first update (max 30 seconds)...")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Get last known location
                getLastKnownLocation()

            }.addOnFailureListener { e ->
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ FAILED TO START LOCATION UPDATES!")
                Log.e(TAG, "   Error: ${e.message}")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception starting location updates: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        Log.d(TAG, "🔍 Checking for last known location...")

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📍 LAST KNOWN LOCATION:")
                Log.d(TAG, "   Latitude:  ${it.latitude}")
                Log.d(TAG, "   Longitude: ${it.longitude}")
                Log.d(TAG, "   Accuracy:  ${it.accuracy} meters")
                Log.d(TAG, "   Age:       ${(System.currentTimeMillis() - it.time) / 1000} seconds old")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                lastLocation = it
            } ?: run {
                Log.w(TAG, "⚠️ No last known location available")
                Log.d(TAG, "   Waiting for GPS to acquire position...")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to get last known location: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🛑 STOPPING LOCATION UPDATES")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isRunning = false

        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        Log.d(TAG, "📊 Session Summary:")
        Log.d(TAG, "   Total updates: $updateCount")
        Log.d(TAG, "   Total time: $totalTime seconds")
        Log.d(TAG, "   Average interval: ${if (updateCount > 1) totalTime / updateCount else 0} seconds")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private suspend fun handleLocationUpdate(location: Location) {
        Log.d(TAG, "🔄 Processing location update...")

        // TODO: Add your location processing logic here
        // Example: Check geofences, update trip progress, etc.

        updateNotification(location)

        Log.d(TAG, "✅ Location update processed successfully")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun updateNotification(currentLocation: Location) {
        val notification = createNotification(
            "Tracking active • Updates: $updateCount\n" +
                    "Lat: ${String.format("%.4f", currentLocation.latitude)}, " +
                    "Lng: ${String.format("%.4f", currentLocation.longitude)}"
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "🔔 Notification updated")
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Travel Alarm Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location for travel alarms"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "✅ Notification channel created")
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 LocationService DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }
}