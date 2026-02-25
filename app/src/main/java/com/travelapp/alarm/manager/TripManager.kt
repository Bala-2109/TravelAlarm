package com.travelapp.alarm.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.travelapp.alarm.data.model.Contact
import com.travelapp.alarm.data.model.Trip

class TripManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "TripManager"
        private const val PREFS_NAME = "travel_alarm_trips"
        private const val KEY_TRIPS = "trips"
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_ACTIVE_TRIP_ID = "active_trip_id"

        @Volatile
        private var instance: TripManager? = null

        fun getInstance(context: Context): TripManager {
            return instance ?: synchronized(this) {
                instance ?: TripManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Save a trip
     */
    fun saveTrip(trip: Trip): Boolean {
        return try {
            val trips = getAllTrips().toMutableList()

            // Check if trip already exists
            val existingIndex = trips.indexOfFirst { it.id == trip.id }
            if (existingIndex != -1) {
                trips[existingIndex] = trip
                Log.d(TAG, "Updated existing trip: ${trip.tripName}")
            } else {
                trips.add(trip)
                Log.d(TAG, "Added new trip: ${trip.tripName}")
            }

            val json = gson.toJson(trips)
            prefs.edit().putString(KEY_TRIPS, json).apply()

            Log.d(TAG, "✅ Trip saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving trip: ${e.message}")
            false
        }
    }

    /**
     * Get all trips
     */
    fun getAllTrips(): List<Trip> {
        return try {
            val json = prefs.getString(KEY_TRIPS, null) ?: return emptyList()
            val type = object : TypeToken<List<Trip>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading trips: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get trip by ID
     */
    fun getTripById(tripId: String): Trip? {
        return try {
            getAllTrips().find { it.id == tripId }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting trip by ID: ${e.message}")
            null
        }
    }

    /**
     * Delete a trip
     */
    fun deleteTrip(tripId: String): Boolean {
        return try {
            val trips = getAllTrips().toMutableList()
            val removed = trips.removeIf { it.id == tripId }

            if (removed) {
                val json = gson.toJson(trips)
                prefs.edit().putString(KEY_TRIPS, json).apply()

                // Clear active trip if this was it
                if (getActiveTripId() == tripId) {
                    clearActiveTrip()
                }

                Log.d(TAG, "✅ Trip deleted successfully")
            }

            removed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting trip: ${e.message}")
            false
        }
    }

    /**
     * Set active trip
     */
    fun setActiveTrip(tripId: String): Boolean {
        return try {
            prefs.edit().putString(KEY_ACTIVE_TRIP_ID, tripId).apply()
            Log.d(TAG, "✅ Active trip set: $tripId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting active trip: ${e.message}")
            false
        }
    }

    /**
     * Get active trip ID
     */
    fun getActiveTripId(): String? {
        return prefs.getString(KEY_ACTIVE_TRIP_ID, null)
    }

    /**
     * Get active trip
     */
    fun getActiveTrip(): Trip? {
        val activeTripId = getActiveTripId() ?: return null
        return getTripById(activeTripId)
    }

    /**
     * Clear active trip
     */
    fun clearActiveTrip() {
        prefs.edit().remove(KEY_ACTIVE_TRIP_ID).apply()
        Log.d(TAG, "✅ Active trip cleared")
    }

    /**
     * Complete a trip
     */
    fun completeTrip(tripId: String): Boolean {
        clearActiveTrip()
        Log.d(TAG, "✅ Trip completed: $tripId")
        return true
    }

    /**
     * Save a contact
     */
    fun saveContact(contact: Contact): Boolean {
        return try {
            val contacts = getAllContacts().toMutableList()

            val existingIndex = contacts.indexOfFirst { it.id == contact.id }
            if (existingIndex != -1) {
                contacts[existingIndex] = contact
            } else {
                contacts.add(contact)
            }

            val json = gson.toJson(contacts)
            prefs.edit().putString(KEY_CONTACTS, json).apply()

            Log.d(TAG, "✅ Contact saved: ${contact.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving contact: ${e.message}")
            false
        }
    }

    /**
     * Get all contacts
     */
    fun getAllContacts(): List<Contact> {
        return try {
            val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading contacts: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete a contact
     */
    fun deleteContact(contactId: String): Boolean {
        return try {
            val contacts = getAllContacts().toMutableList()
            val removed = contacts.removeIf { it.id == contactId }

            if (removed) {
                val json = gson.toJson(contacts)
                prefs.edit().putString(KEY_CONTACTS, json).apply()
                Log.d(TAG, "✅ Contact deleted")
            }

            removed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting contact: ${e.message}")
            false
        }
    }

    /**
     * Export all data
     */
    fun exportData(): String {
        val data = mapOf(
            "trips" to getAllTrips(),
            "contacts" to getAllContacts(),
            "activeTrip" to getActiveTripId()
        )
        return gson.toJson(data)
    }

    /**
     * Import data
     */
    fun importData(json: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, type)

            // Import trips
            val tripsJson = gson.toJson(data["trips"])
            prefs.edit().putString(KEY_TRIPS, tripsJson).apply()

            // Import contacts
            val contactsJson = gson.toJson(data["contacts"])
            prefs.edit().putString(KEY_CONTACTS, contactsJson).apply()

            Log.d(TAG, "✅ Data imported successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error importing data: ${e.message}")
            false
        }
    }

    /**
     * Get statistics
     */
    fun getStatistics(): Map<String, Int> {
        return mapOf(
            "totalTrips" to getAllTrips().size,
            "totalContacts" to getAllContacts().size,
            "hasActiveTrip" to if (getActiveTrip() != null) 1 else 0
        )
    }
}