package com.travelapp.alarm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.travelapp.alarm.manager.TripManager

class MainActivity : AppCompatActivity() {

    private lateinit var tripManager: TripManager

    private lateinit var btnCreateTrip: Button
    private lateinit var btnViewTrips: Button
    private lateinit var tvTripInfo: TextView

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
        tvTripInfo = findViewById(R.id.tvTripInfo)

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
            viewTrips()
        }
    }

    private fun openCreateTrip() {
        Log.d(TAG, "🗺️ Opening CreateTripActivity...")
        val intent = Intent(this, CreateTripActivity::class.java)
        startActivity(intent)
    }

    private fun viewTrips() {
        val trips = tripManager.getAllTrips()

        if (trips.isEmpty()) {
            Toast.makeText(this, "No trips yet. Create your first trip!", Toast.LENGTH_SHORT).show()
        } else {
            // Show simple info about trips
            val tripList = trips.joinToString("\n") { "• ${it.name}" }
            Toast.makeText(this, "Your Trips:\n$tripList", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTripInfo() {
        val trips = tripManager.getAllTrips()
        val activeTrip = tripManager.getActiveTrip()

        tvTripInfo.text = when {
            activeTrip != null -> {
                "Active Trip: ${activeTrip.name}\n" +
                        "Checkpoints: ${activeTrip.checkpoints.size}\n" +
                        "Total Trips: ${trips.size}"
            }
            trips.isNotEmpty() -> {
                "You have ${trips.size} trip(s)\n" +
                        "No active trip\n" +
                        "Tap 'View Trips' to see all"
            }
            else -> {
                "No trips yet\n" +
                        "Create your first trip!\n" +
                        "Tap 'Create New Trip' to start"
            }
        }

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