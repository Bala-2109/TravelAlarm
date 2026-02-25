package com.travelapp.alarm.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.travelapp.alarm.MainActivity
import com.travelapp.alarm.R
import com.travelapp.alarm.tracking.ActiveTripTracker

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var activeTripTracker: ActiveTripTracker

    private var updateCount = 0
    private var sessionStartTime = 0L

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_service_channel"
        private const val LOCATION_UPDATE_INTERVAL = 30000L  // 30 seconds
        private const val FASTEST_UPDATE_INTERVAL = 15000L   // 15 seconds
        private const val MIN_DISTANCE_METERS = 10f          // 10 meters

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🟢 LocationService CREATED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        activeTripTracker = ActiveTripTracker(this)

        setupLocationCallback()
        setupTripCallbacks()
        createNotificationChannel()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }

        Log.d(TAG, "✅ Location callback setup complete")
    }

    private fun setupTripCallbacks() {
        // Callback when checkpoint is reached
        activeTripTracker.setOnCheckpointReached { checkpoint, index ->
            Log.d(TAG, "🎯 CHECKPOINT ${index + 1} REACHED: ${checkpoint.name}")
            showCheckpointNotification(checkpoint.name, index + 1)
        }

        // Callback when destination is reached
        activeTripTracker.setOnDestinationReached {
            Log.d(TAG, "🏁 DESTINATION REACHED!")
            showDestinationNotification()
        }

        Log.d(TAG, "✅ Trip callbacks setup complete")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📥 onStartCommand called")

        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "▶️ Starting location service...")
                startLocationUpdates()

                // Start trip tracking if there's an active trip
                activeTripTracker.startTracking()
            }
            ACTION_STOP -> {
                Log.d(TAG, "⏹️ Stopping location service...")
                stopLocationUpdates()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 STARTING LOCATION UPDATES")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
        }.build()

        Log.d(TAG, "📋 Location Request Settings:")
        Log.d(TAG, "   Priority: HIGH_ACCURACY")
        Log.d(TAG, "   Update Interval: ${LOCATION_UPDATE_INTERVAL}ms (30 sec)")
        Log.d(TAG, "   Fastest Interval: ${FASTEST_UPDATE_INTERVAL}ms (15 sec)")
        Log.d(TAG, "   Min Distance: ${MIN_DISTANCE_METERS}m")

        val notification = createNotification("Starting location tracking...")
        startForeground(NOTIFICATION_ID, notification)

        Log.d(TAG, "✅ Started as foreground service")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )

        sessionStartTime = System.currentTimeMillis()
        updateCount = 0

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ LOCATION UPDATES STARTED SUCCESSFULLY!")
        Log.d(TAG, "⏰ Waiting for first update (max 30 seconds)...")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun onLocationUpdate(location: Location) {
        updateCount++

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 LOCATION UPDATE #$updateCount")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📊 Location Details:")
        Log.d(TAG, "   Latitude:  ${location.latitude}")
        Log.d(TAG, "   Longitude: ${location.longitude}")
        Log.d(TAG, "   Accuracy:  ${location.accuracy}m")

        // Update trip tracking and get progress
        val progress = activeTripTracker.updateLocation(location)

        // Create notification content
        val notificationContent = if (progress != null) {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🗺️ TRIP PROGRESS")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "Trip: ${progress.tripName}")
            Log.d(TAG, "Next: ${progress.nextCheckpointName}")
            Log.d(TAG, "Distance to next: ${progress.formatDistanceToNextCheckpoint()}")
            Log.d(TAG, "Distance to destination: ${progress.formatDistanceToDestination()}")
            Log.d(TAG, "Progress: ${progress.currentCheckpointIndex}/${progress.totalCheckpoints} (${progress.progressPercentage}%)")

            buildString {
                append("🗺️ ${progress.tripName}\n")
                append("📍 Next: ${progress.nextCheckpointName}\n")
                append("📏 Distance: ${progress.formatDistanceToNextCheckpoint()}\n")
                append("🎯 To destination: ${progress.formatDistanceToDestination()}\n")
                append("📊 Progress: ${progress.currentCheckpointIndex}/${progress.totalCheckpoints} (${progress.progressPercentage}%)")
            }
        } else {
            buildString {
                append("No active trip\n")
                append("📍 Lat: ${String.format("%.4f", location.latitude)}\n")
                append("📍 Lng: ${String.format("%.4f", location.longitude)}")
            }
        }

        updateNotification(notificationContent)
        Log.d(TAG, "✅ Location update processed successfully")
    }

    private fun showCheckpointNotification(checkpointName: String, index: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎯 Checkpoint Reached!")
            .setContentText("You've reached $checkpointName (${index})")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Checkpoint ${index}: $checkpointName\n\n" +
                        "Great progress! Keep going to your destination."
            ))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2000 + index, notification)
    }

    private fun showDestinationNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏁 Destination Reached!")
            .setContentText("You've arrived at your destination!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Congratulations! You've successfully reached your destination.\n\n" +
                        "Trip completed!"
            ))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(3000, notification)
    }

    private fun stopLocationUpdates() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🛑 STOPPING LOCATION UPDATES")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        fusedLocationClient.removeLocationUpdates(locationCallback)
        activeTripTracker.stopTracking()

        val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000

        Log.d(TAG, "📊 Session Summary:")
        Log.d(TAG, "   Total updates: $updateCount")
        Log.d(TAG, "   Total time: ${sessionDuration}s")
        if (updateCount > 0) {
            Log.d(TAG, "   Average interval: ${sessionDuration / updateCount}s")
        }

        Log.d(TAG, "✅ Location updates stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location for trip progress"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            Log.d(TAG, "✅ Notification channel created")
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Travel Alarm Active")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true).build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 LocationService DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        super.onDestroy()
    }
}
