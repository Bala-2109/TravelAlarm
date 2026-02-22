package com.travelapp.alarm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.travelapp.alarm.adapter.TripAdapter
import com.travelapp.alarm.data.model.Trip
import com.travelapp.alarm.manager.TripManager

class TripListActivity : AppCompatActivity() {

    private lateinit var tripManager: TripManager
    private lateinit var tripAdapter: TripAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
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

        initializeViews()
        setupRecyclerView()
        loadTrips()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewTrips)
        emptyState = findViewById(R.id.tvEmptyState)
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

        Log.d(TAG, "📊 Loading trips - Count: ${trips.size}")

        if (trips.isEmpty()) {
            showEmptyState()
        } else {
            showTripList(trips)
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE

        Log.d(TAG, "📭 No trips - Showing empty state")
    }

    private fun showTripList(trips: List<Trip>) {
        recyclerView.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        tripAdapter.submitList(trips)

        Log.d(TAG, "✅ ${trips.size} trips loaded")
    }

    private fun onTripClicked(trip: Trip) {
        Log.d(TAG, "👆 Trip clicked: ${trip.name}")
        // TODO: Open TripDetailsActivity
        AlertDialog.Builder(this)
            .setTitle(trip.name)
            .setMessage(
                "Destination: ${trip.originalDestinationName}\n" +
                        "Checkpoints: ${trip.checkpoints.size}\n" +
                        "Traveler: ${trip.traveler}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun onStartTrip(trip: Trip) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 START TRIP: ${trip.name}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Set as active trip
        val success = tripManager.setActiveTrip(trip.id)

        if (success) {
            AlertDialog.Builder(this)
                .setTitle("Trip Started!")
                .setMessage("${trip.name} is now active.\n\nLocation tracking will monitor your progress to the destination.")
                .setPositiveButton("OK") { _, _ ->
                    // Refresh list to show active state
                    loadTrips()
                }
                .show()

            Log.d(TAG, "✅ Trip activated successfully")
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
        Log.d(TAG, "🗑️ Delete request for: ${trip.name}")

        AlertDialog.Builder(this)
            .setTitle("Delete Trip?")
            .setMessage("Are you sure you want to delete \"${trip.name}\"?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip(trip: Trip) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🗑️ DELETING TRIP: ${trip.name}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val success = tripManager.deleteTrip(trip.id)

        if (success) {
            Log.d(TAG, "✅ Trip deleted successfully")
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
