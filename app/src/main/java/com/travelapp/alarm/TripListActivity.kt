package com.travelapp.alarm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.travelapp.alarm.adapter.TripAdapter
import com.travelapp.alarm.data.model.Trip
import com.travelapp.alarm.manager.TripManager
import com.travelapp.alarm.service.GeofencingManager

class TripListActivity : AppCompatActivity() {

    private lateinit var tripManager: TripManager
    private lateinit var geofencingManager: GeofencingManager
    private lateinit var tripAdapter: TripAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var btnCreateTrip: Button

    companion object {
        private const val TAG = "TripListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_list)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📋 TRIP LIST ACTIVITY STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        tripManager = TripManager.getInstance(this)
        geofencingManager = GeofencingManager.getInstance(this)

        initializeViews()
        setupRecyclerView()
        loadTrips()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewTrips)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        btnCreateTrip = findViewById(R.id.btnCreateTrip)

        btnCreateTrip.setOnClickListener {
            openCreateTrip()
        }

        Log.d(TAG, "✅ Views initialized")
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(
            onTripClick = { trip ->
                onTripClicked(trip)
            },
            onStartClick = { trip ->
                onStartTrip(trip)
            },
            onDeleteClick = { trip ->
                onDeleteTrip(trip)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TripListActivity)
            adapter = tripAdapter
        }

        Log.d(TAG, "✅ RecyclerView setup complete")
    }

    private fun loadTrips() {
        val trips = tripManager.getAllTrips()
        val activeTripId = tripManager.getActiveTripId()

        Log.d(TAG, "📊 Loading trips - Count: ${trips.size}")

        if (trips.isEmpty()) {
            showEmptyState()
        } else {
            showTripList(trips)
        }

        // Update adapter with active trip ID
        tripAdapter.setActiveTripId(activeTripId)
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE

        Log.d(TAG, "📭 No trips - Showing empty state")
    }

    private fun showTripList(trips: List<Trip>) {
        recyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        tripAdapter.submitList(trips)

        Log.d(TAG, "✅ ${trips.size} trips loaded")
    }

    private fun onTripClicked(trip: Trip) {
        Log.d(TAG, "👆 Trip clicked: ${trip.tripName}")

        AlertDialog.Builder(this)
            .setTitle(trip.tripName)
            .setMessage("Destination: ${trip.destination}\nCheckpoints: ${trip.checkpoints.size}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun onStartTrip(trip: Trip) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 START TRIP: ${trip.tripName}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Set as active trip
        val success = tripManager.setActiveTrip(trip.id)

        if (success) {
            // Create geofences for the trip
            Log.d(TAG, "🎯 Creating geofences...")
            val geofencesCreated = geofencingManager.createGeofencesForTrip(trip)

            if (geofencesCreated) {
                AlertDialog.Builder(this)
                    .setTitle("Trip Started!")
                    .setMessage(
                        "${trip.tripName} is now active.\n\n" +
                                "✅ Geofences created: ${trip.checkpoints.size + 1}\n" +
                                "🔔 You'll be alerted when you reach checkpoints\n\n" +
                                "Start location tracking to monitor your progress!"
                    )
                    .setPositiveButton("OK") { _, _ ->
                        loadTrips() // Refresh list
                    }
                    .show()

                Log.d(TAG, "✅ Trip activated with geofences")
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage(
                        "Trip activated but geofences may not work.\n\n" +
                                "Please check location permissions."
                    )
                    .setPositiveButton("OK") { _, _ ->
                        loadTrips()
                    }
                    .show()

                Log.w(TAG, "⚠️ Trip activated but geofences failed")
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to start trip")
                .setPositiveButton("OK", null)
                .show()

            Log.e(TAG, "❌ Failed to activate trip")
        }
    }

    private fun onDeleteTrip(trip: Trip) {
        Log.d(TAG, "🗑️ Delete request for: ${trip.tripName}")

        AlertDialog.Builder(this)
            .setTitle("Delete Trip?")
            .setMessage("Are you sure you want to delete \"${trip.tripName}\"?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip(trip: Trip) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🗑️ DELETING TRIP: ${trip.tripName}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Remove geofences if this is the active trip
        if (tripManager.getActiveTripId() == trip.id) {
            Log.d(TAG, "🗑️ Removing geofences for active trip")
            geofencingManager.removeGeofencesForTrip(trip.id)
        }

        // Delete trip
        val success = tripManager.deleteTrip(trip.id)

        if (success) {
            Log.d(TAG, "✅ Trip deleted successfully")
            Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show()
            loadTrips() // Reload list
        } else {
            Log.e(TAG, "❌ Failed to delete trip")
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to delete trip")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun openCreateTrip() {
        Log.d(TAG, "🗺️ Opening CreateTripActivity")
        val intent = Intent(this, CreateTripActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 Activity resumed - Reloading trips")
        loadTrips()
    }

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 TripListActivity DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        super.onDestroy()
    }
}