package com.travelapp.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.travelapp.alarm.data.model.*
import com.travelapp.alarm.service.GeofencingManager
import com.travelapp.alarm.service.LocationService

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestPermissions()
        testDataModels()
    }

    private fun requestPermissions() {
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

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationService()
            }
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun testDataModels() {
        Log.d("TravelAlarm", "========== Testing Data Models ==========")

        val home = LatLng(13.0827, 80.2707)
        val station = LatLng(12.9249, 80.1000)
        Log.d("TravelAlarm", "✅ LatLng created: $home")
        Log.d("TravelAlarm", "   Distance: ${(home.distanceTo(station)/1000).toInt()} km")

        val user = User(
            id = "user1",
            name = "Balas",
            phoneNumber = "+91 9876543210",
            email = "balas@example.com"
        )
        Log.d("TravelAlarm", "✅ User created: ${user.name}")

        val contact = Contact(
            id = "1",
            name = "Mom",
            phoneNumber = "+91 9876543210",
            hasApp = false,
            primaryMethod = NotificationMethod.WHATSAPP,
            fallbackMethod = NotificationMethod.SMS
        )
        Log.d("TravelAlarm", "✅ Contact created: ${contact.name}")

        val checkpoint = Checkpoint(
            id = "cp1",
            name = "Tambaram Station",
            location = station,
            radius = 300f,
            notifyOnEntry = true,
            notifyContacts = true,
            notifyTraveler = true
        )
        Log.d("TravelAlarm", "✅ Checkpoint created: ${checkpoint.name}")

        val trip = Trip(
            id = "trip1",
            traveler = user,
            startLocation = station,
            startLocationName = "Tambaram Station",
            originalDestination = home,
            originalDestinationName = "Home",
            currentDestination = home,
            currentDestinationName = "Home",
            pickupPeople = mutableListOf(contact),
            checkpoints = mutableListOf(checkpoint)
        )
        Log.d("TravelAlarm", "✅ Trip created: ${trip.traveler.name} → ${trip.currentDestinationName}")

        trip.start()

        val geofencingManager = GeofencingManager(this)
        geofencingManager.setupTripGeofences(trip)

        Log.d("TravelAlarm", "✅ Geofences registered for trip")
        Log.d("TravelAlarm", "   - Destination: ${trip.currentDestinationName}")
        Log.d("TravelAlarm", "   - Alarm radius: ${trip.alarmRadius}m")
        Log.d("TravelAlarm", "   - Notify radius: ${trip.notificationRadius}m")
        Log.d("TravelAlarm", "   - Checkpoints: ${trip.checkpoints.size}")

        Log.d("TravelAlarm", "========== All Models Working! ==========")
    }
}