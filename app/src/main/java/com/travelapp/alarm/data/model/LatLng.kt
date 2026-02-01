package com.travelapp.alarm.data.model

/**
 * Represents a geographic location with latitude and longitude
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculate distance to another location in meters
     */
    fun distanceTo(other: LatLng): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLng = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    override fun toString(): String {
        return "($latitude, $longitude)"
    }
}