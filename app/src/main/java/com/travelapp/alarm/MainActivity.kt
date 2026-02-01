package com.travelapp.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.travelapp.alarm.data.model.*
import com.travelapp.alarm.service.BatteryMonitor
import com.travelapp.alarm.service.GeofencingManager
import com.travelapp.alarm.service.LocationService

class MainActivity : AppCompatActivity() {

    // Modern way to handle permissions in Android
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        // Notifications are only required for Android 13+
        val postNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (fineLocationGranted && postNotificationGranted) {
            Log.d("TravelAlarm", "✅ Foreground permissions granted")
            checkBackgroundLocationPermission()
        } else {
            Log.e("TravelAlarm", "❌ Essential permissions denied")
            Toast.makeText(this, "Location and Notifications are required for alarms.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Adjust for system bars (status bar/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Run your internal model tests
        testDataModels()
        testLocationServices()

        // Start the permission sequence
        initiatePermissionRequest()
    }

    private fun initiatePermissionRequest() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkBackgroundLocationPermission() {
        // Background location must be requested AFTER foreground location is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                Log.d("TravelAlarm", "Requesting Background Location Permission")
                Toast.makeText(this, "Please select 'Allow all the time' for background tracking", Toast.LENGTH_LONG).show()

                val bgLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        startLocationTracking()
                    } else {
                        Log.e("TravelAlarm", "❌ Background location denied")
                    }
                }
                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                startLocationTracking()
            }
        } else {
            startLocationTracking()
        }
    }

    private fun startLocationTracking() {
        Log.d("TravelAlarm", "🚀 Starting LocationService...")

        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START_TRACKING
            putExtra(LocationService.EXTRA_TRIP_ID, "test_trip_001")
            putExtra(LocationService.EXTRA_UPDATE_INTERVAL, 30000L)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d("TravelAlarm", "✅ Location service started successfully!")
        } catch (e: Exception) {
            Log.e("TravelAlarm", "❌ Failed to start service: ${e.message}")
        }
    }

    // --- YOUR EXISTING TEST METHODS ---
    private fun testDataModels() {
        Log.d("TravelAlarm", "========== Testing Data Models ==========")
        val home = LatLng(13.0827, 80.2707)
        val station = LatLng(12.9249, 80.1000)
        Log.d("TravelAlarm", "✅ LatLng created: $home")

        val user = User(id = "user1", name = "Balas", phoneNumber = "+91 9876543210", email = "balas@example.com")
        Log.d("TravelAlarm", "✅ User created: ${user.name}")

        val contact = Contact(id = "1", name = "Mom", phoneNumber = "+91 9876543210", hasApp = false,
            primaryMethod = NotificationMethod.WHATSAPP, fallbackMethod = NotificationMethod.SMS)
        Log.d("TravelAlarm", "✅ Contact created: ${contact.name}")

        val checkpoint = Checkpoint(id = "cp1", name = "Tambaram Station", location = station, radius = 300f,
            notifyOnEntry = true, notifyContacts = true, notifyTraveler = true)
        Log.d("TravelAlarm", "✅ Checkpoint created: ${checkpoint.name}")

        val trip = Trip(id = "trip1", traveler = user, startLocation = station, startLocationName = "Tambaram Station",
            originalDestination = home, originalDestinationName = "Home", currentDestination = home,
            currentDestinationName = "Home", pickupPeople = mutableListOf(contact), checkpoints = mutableListOf(checkpoint))

        trip.start()
        Log.d("TravelAlarm", "✅ Trip created and started: Status ${trip.status}")
        Log.d("TravelAlarm", "========== All Models Working Perfectly! ==========")
    }

    private fun testLocationServices() {
        Log.d("TravelAlarm", "========== Testing Location Services ==========")
        try {
            val geofencingManager = GeofencingManager(this)
            Log.d("TravelAlarm", "✅ GeofencingManager created")
            val settings = AppSettings()
            val batteryMonitor = BatteryMonitor(this, settings)
            Log.d("TravelAlarm", "✅ BatteryMonitor created")
        } catch (e: Exception) {
            Log.e("TravelAlarm", "❌ Service test failed: ${e.message}")
        }
        Log.d("TravelAlarm", "========== Services Ready! ==========")
    }
}