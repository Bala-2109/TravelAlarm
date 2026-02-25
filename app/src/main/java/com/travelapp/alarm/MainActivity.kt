package com.travelapp.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.travelapp.alarm.manager.TripManager
import com.travelapp.alarm.service.LocationService

class MainActivity : AppCompatActivity() {

    private lateinit var tripManager: TripManager

    private lateinit var btnCreateTrip: Button
    private lateinit var btnViewTrips: Button
    private lateinit var btnStartTracking: Button
    private lateinit var btnStopTracking: Button
    private lateinit var tvTripInfo: TextView
    private lateinit var tvTrackingStatus: TextView

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 TRAVEL ALARM APP STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Initialize TripManager
        tripManager = TripManager.getInstance(this)

        // Initialize views
        initializeViews()

        // Setup button listeners
        setupButtonListeners()

        // Update trip info
        updateTripInfo()
    }

    private fun initializeViews() {
        btnCreateTrip = findViewById(R.id.btnCreateTrip)
        btnViewTrips = findViewById(R.id.btnViewTrips)
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnStopTracking = findViewById(R.id.btnStopTracking)
        tvTripInfo = findViewById(R.id.tvTripInfo)
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus)

        // Initially disable stop button
        btnStopTracking.isEnabled = false

        Log.d(TAG, "✅ Views initialized")
    }

    private fun setupButtonListeners() {
        btnCreateTrip.setOnClickListener {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "👆 CREATE TRIP BUTTON CLICKED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            openCreateTrip()
        }

        btnViewTrips.setOnClickListener {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "👆 VIEW TRIPS BUTTON CLICKED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            openViewTrips()
        }

        btnStartTracking.setOnClickListener {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "👆 START TRACKING BUTTON CLICKED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            startLocationTracking()
        }

        btnStopTracking.setOnClickListener {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "👆 STOP TRACKING BUTTON CLICKED")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            stopLocationTracking()
        }
    }

    private fun openCreateTrip() {
        Log.d(TAG, "🗺️ Opening CreateTripActivity...")
        try {
            val intent = Intent(this, CreateTripActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "✅ CreateTripActivity started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening CreateTripActivity: ${e.message}")
            Toast.makeText(this, "Error opening trip creation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openViewTrips() {
        Log.d(TAG, "📋 Opening TripListActivity...")

        try {
            val intent = Intent(this, TripListActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "✅ TripListActivity started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening TripListActivity: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startLocationTracking() {
        Log.d(TAG, "🔍 Checking permissions before starting service...")

        // Check if all permissions are granted
        if (!hasAllPermissions()) {
            Log.e(TAG, "❌ Not all permissions granted!")
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "✅ All permissions verified - Starting LocationService...")

        try {
            // Create intent for LocationService
            val serviceIntent = Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }

            // Start the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "📱 Starting foreground service (Android O+)")
                startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "📱 Starting service (Android < O)")
                startService(serviceIntent)
            }

            Log.d(TAG, "✅ LocationService start command sent")

            // Update UI
            updateTrackingUI(true)

            Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start LocationService: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Failed to start tracking: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopLocationTracking() {
        Log.d(TAG, "🛑 Stopping LocationService...")

        try {
            val serviceIntent = Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
            }

            startService(serviceIntent)

            Log.d(TAG, "✅ LocationService stop command sent")

            // Update UI
            updateTrackingUI(false)

            Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop LocationService: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "Permission check result: $allGranted")
        return allGranted
    }

    private fun updateTrackingUI(isTracking: Boolean) {
        btnStartTracking.isEnabled = !isTracking
        btnStopTracking.isEnabled = isTracking

        tvTrackingStatus.text = if (isTracking) {
            "Status: 🟢 Tracking Active"
        } else {
            "Status: ⚪ Not Tracking"
        }

        if (isTracking) {
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvTrackingStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun updateTripInfo() {
        val trips = tripManager.getAllTrips()
        val activeTrip = tripManager.getActiveTrip()

        val tripInfoText = when {
            activeTrip != null -> """
                Active Trip: ${activeTrip.tripName}
                Checkpoints: ${activeTrip.checkpoints.size}
                Total Trips: ${trips.size}
            """.trimIndent()
            trips.isNotEmpty() -> """
                You have ${trips.size} trip(s)
                No active trip
                Tap 'View Trips' to see all
            """.trimIndent()
            else -> """
                No trips yet
                Create your first trip!
                Tap 'Create New Trip' to start
            """.trimIndent()
        }

        tvTripInfo.text = tripInfoText

        Log.d(TAG, "📊 Trip info updated - Total trips: ${trips.size}, Active: ${activeTrip != null}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 Activity resumed - Updating trip info")
        updateTripInfo()
    }

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 MainActivity DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        super.onDestroy()
    }
}
