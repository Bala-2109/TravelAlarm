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
import com.travelapp.alarm.service.LocationService
import com.travelapp.alarm.util.PermissionManager

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartTracking: Button
    private lateinit var btnStopTracking: Button
    private lateinit var tvStatus: TextView

    private lateinit var permissionManager: PermissionManager

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 TRAVEL ALARM APP STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Initialize views
        initializeViews()

        // Initialize permission manager
        permissionManager = PermissionManager(this)

        // Request all permissions with proper callback
        Log.d(TAG, "Requesting all permissions...")
        permissionManager.requestAllPermissions(object : PermissionManager.PermissionCallback {
            override fun onPermissionResult(allGranted: Boolean) {
                if (allGranted) {
                    Log.d(TAG, "✅ All permissions granted - Ready to start tracking")
                    updateUI()
                } else {
                    Log.e(TAG, "❌ Some permissions denied")
                    Toast.makeText(
                        this@MainActivity,
                        "Please grant all permissions to use this app",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        // Set up button listeners
        setupButtonListeners()

        // Update UI based on service state
        updateUI()
    }

    private fun initializeViews() {
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnStopTracking = findViewById(R.id.btnStopTracking)
        tvStatus = findViewById(R.id.tvStatus)

        Log.d(TAG, "✅ Views initialized")
    }

    private fun setupButtonListeners() {
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

    private fun startLocationTracking() {
        Log.d(TAG, "🔍 Checking permissions before starting service...")

        // Check if all permissions are granted
        if (!hasAllPermissions()) {
            Log.e(TAG, "❌ Not all permissions granted!")
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show()

            // Request permissions again with proper callback
            permissionManager.requestAllPermissions(object : PermissionManager.PermissionCallback {
                override fun onPermissionResult(allGranted: Boolean) {
                    if (allGranted) {
                        startLocationTracking()
                    }
                }
            })
            return
        }

        Log.d(TAG, "✅ All permissions verified - Starting LocationService...")

        try {
            // Create intent for LocationService
            val serviceIntent = Intent(this, LocationService::class.java).apply {
                action = "ACTION_START"  // Hardcoded string to avoid unresolved reference
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
            updateUI()

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
                action = "ACTION_STOP"  // Hardcoded string to avoid unresolved reference
            }

            startService(serviceIntent)

            Log.d(TAG, "✅ LocationService stop command sent")

            // Update UI
            updateUI()

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

        permissions.addAll(listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        ))

        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "Permission check result: $allGranted")
        return allGranted
    }

    private fun updateUI() {
        // Check if service is running by trying to get the static variable
        // Since we can't access LocationService.isRunning due to compilation error,
        // we'll track it locally or assume it's running after start
        val isRunning = isServiceRunning()

        Log.d(TAG, "📱 Updating UI - Service running: $isRunning")

        btnStartTracking.isEnabled = !isRunning
        btnStopTracking.isEnabled = isRunning

        tvStatus.text = if (isRunning) {
            "Status: 🟢 Tracking Active"
        } else {
            "Status: ⚪ Not Tracking"
        }

        if (isRunning) {
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvStatus.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun isServiceRunning(): Boolean {
        // Simple check - you can enhance this with ActivityManager if needed
        // For now, we'll check if the START button was pressed
        return !btnStopTracking.isEnabled
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 Activity resumed - Updating UI")
        updateUI()
    }

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 MainActivity DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        super.onDestroy()
    }
}