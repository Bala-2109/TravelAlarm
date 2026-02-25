package com.travelapp.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.travelapp.alarm.data.model.Trip
import com.travelapp.alarm.manager.TripManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.UUID

class CreateTripActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var tripManager: TripManager

    private lateinit var etTripName: EditText
    private lateinit var btnSetDestination: Button
    private lateinit var btnAddCheckpoint: Button
    private lateinit var btnSaveTrip: Button
    private lateinit var btnCancel: Button

    private var destinationMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()

    private var isSettingDestination = false
    private var isAddingCheckpoint = false

    private var destinationGeoPoint: GeoPoint? = null
    private val checkpoints = mutableListOf<Trip.Checkpoint>()

    companion object {
        private const val TAG = "CreateTripActivity"
        private const val DEFAULT_ZOOM = 15.0
        private val DEFAULT_LOCATION = GeoPoint(13.0827, 80.2707)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🗺️ CREATE TRIP ACTIVITY STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )

        setContentView(R.layout.activity_create_trip)

        tripManager = TripManager.getInstance(this)

        initializeViews()
        setupMap()
        setupButtonListeners()
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        etTripName = findViewById(R.id.etTripName)
        btnSetDestination = findViewById(R.id.btnSetDestination)
        btnAddCheckpoint = findViewById(R.id.btnAddCheckpoint)
        btnSaveTrip = findViewById(R.id.btnSaveTrip)
        btnCancel = findViewById(R.id.btnCancel)

        btnSaveTrip.isEnabled = false

        Log.d(TAG, "✅ Views initialized")
    }

    private fun setupMap() {
        Log.d(TAG, "🗺️ Setting up OpenStreetMap...")

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(DEFAULT_LOCATION)

        if (hasLocationPermission()) {
            myLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(this),
                mapView
            )
            myLocationOverlay.enableMyLocation()
            mapView.overlays.add(myLocationOverlay)

            Log.d(TAG, "✅ My location overlay enabled")
        }

        val mapEventsOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: android.view.MotionEvent,
                mapView: MapView
            ): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                onMapClicked(geoPoint)
                return true
            }
        }
        mapView.overlays.add(0, mapEventsOverlay)

        Log.d(TAG, "✅ Map setup complete")
    }

    private fun setupButtonListeners() {
        btnSetDestination.setOnClickListener {
            Log.d(TAG, "📍 Set destination mode activated")
            isSettingDestination = true
            isAddingCheckpoint = false
            Toast.makeText(this, "Tap on map to set destination", Toast.LENGTH_SHORT).show()

            btnSetDestination.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            btnAddCheckpoint.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        btnAddCheckpoint.setOnClickListener {
            if (destinationGeoPoint == null) {
                Toast.makeText(this, "Please set destination first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "📌 Add checkpoint mode activated")
            isSettingDestination = false
            isAddingCheckpoint = true
            Toast.makeText(this, "Tap on map to add checkpoint", Toast.LENGTH_SHORT).show()

            btnAddCheckpoint.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            btnSetDestination.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        btnSaveTrip.setOnClickListener {
            saveTrip()
        }

        btnCancel.setOnClickListener {
            Log.d(TAG, "❌ Trip creation cancelled")
            finish()
        }
    }

    private fun onMapClicked(geoPoint: GeoPoint) {
        Log.d(TAG, "🗺️ Map clicked at: ${geoPoint.latitude}, ${geoPoint.longitude}")

        when {
            isSettingDestination -> setDestination(geoPoint)
            isAddingCheckpoint -> addCheckpoint(geoPoint)
            else -> Toast.makeText(this, "Select 'Set Destination' or 'Add Checkpoint' first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setDestination(geoPoint: GeoPoint) {
        if (destinationMarker != null) {
            mapView.overlays.remove(destinationMarker)
        }

        destinationMarker = Marker(mapView).apply {
            position = geoPoint
            title = "Destination"
            snippet = "Final destination"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@CreateTripActivity, org.osmdroid.library.R.drawable.marker_default)
        }

        mapView.overlays.add(destinationMarker)
        mapView.invalidate()

        destinationGeoPoint = geoPoint

        Log.d(TAG, "✅ Destination set at: ${geoPoint.latitude}, ${geoPoint.longitude}")
        Toast.makeText(this, "Destination set!", Toast.LENGTH_SHORT).show()

        isSettingDestination = false
        btnSetDestination.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

        updateSaveButtonState()
    }

    private fun addCheckpoint(geoPoint: GeoPoint) {
        val marker = Marker(mapView).apply {
            position = geoPoint
            title = "Checkpoint ${checkpoints.size + 1}"
            snippet = "Tap to remove"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@CreateTripActivity, org.osmdroid.library.R.drawable.marker_default)

            setOnMarkerClickListener { clickedMarker, _ ->
                removeCheckpoint(clickedMarker)
                true
            }
        }

        mapView.overlays.add(marker)
        checkpointMarkers.add(marker)
        mapView.invalidate()

        val checkpoint = Trip.Checkpoint(
            name = "Checkpoint ${checkpoints.size + 1}",
            latitude = geoPoint.latitude,
            longitude = geoPoint.longitude,
            isReached = false,
            checkpointName = "Checkpoint ${checkpoints.size + 1}"
        )
        checkpoints.add(checkpoint)

        Log.d(TAG, "✅ Checkpoint ${checkpoints.size} added at: ${geoPoint.latitude}, ${geoPoint.longitude}")
        Toast.makeText(this, "Checkpoint ${checkpoints.size} added!", Toast.LENGTH_SHORT).show()

        isAddingCheckpoint = false
        btnAddCheckpoint.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
    }

    private fun removeCheckpoint(marker: Marker) {
        val index = checkpointMarkers.indexOf(marker)
        if (index >= 0 && index < checkpoints.size) {
            checkpoints.removeAt(index)
            checkpointMarkers.removeAt(index)
            mapView.overlays.remove(marker)
            mapView.invalidate()

            renumberCheckpoints()

            Log.d(TAG, "🗑️ Checkpoint removed")
            Toast.makeText(this, "Checkpoint removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renumberCheckpoints() {
        checkpointMarkers.forEachIndexed { index, marker ->
            marker.title = "Checkpoint ${index + 1}"
        }

        val updatedCheckpoints = mutableListOf<Trip.Checkpoint>()
        checkpoints.forEachIndexed { index, checkpoint ->
            updatedCheckpoints.add(
                checkpoint.copy(
                    name = "Checkpoint ${index + 1}",
                    checkpointName = "Checkpoint ${index + 1}"
                )
            )
        }
        checkpoints.clear()
        checkpoints.addAll(updatedCheckpoints)

        mapView.invalidate()
    }

    private fun updateSaveButtonState() {
        val hasName = etTripName.text.toString().isNotBlank()
        val hasDestination = destinationGeoPoint != null

        btnSaveTrip.isEnabled = hasName && hasDestination
    }

    private fun saveTrip() {
        val name = etTripName.text.toString().trim()

        if (name.isBlank()) {
            Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
            return
        }

        if (destinationGeoPoint == null) {
            Toast.makeText(this, "Please set destination", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💾 SAVING TRIP")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Trip Name: $name")
        Log.d(TAG, "Destination: ${destinationGeoPoint!!.latitude}, ${destinationGeoPoint!!.longitude}")
        Log.d(TAG, "Checkpoints: ${checkpoints.size}")

        val startLocationGeoPoint = myLocationOverlay.myLocation ?: DEFAULT_LOCATION

        val trip = Trip(
            id = UUID.randomUUID().toString(),
            tripName = name,
            startLocation = "${startLocationGeoPoint.latitude},${startLocationGeoPoint.longitude}",
            destination = "${destinationGeoPoint!!.latitude},${destinationGeoPoint!!.longitude}",
            currentDestinationName = "Destination",
            latitude = destinationGeoPoint!!.latitude,
            longitude = destinationGeoPoint!!.longitude,
            checkpoints = checkpoints,
            enabled = true
        )

        tripManager.saveTrip(trip)

        Log.d(TAG, "✅ Trip saved successfully!")
        Toast.makeText(this, "Trip created successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
