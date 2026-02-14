package com.travelapp.alarm.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.travelapp.alarm.data.model.Contact
import com.travelapp.alarm.data.model.Trip

class TripManager(context: Context) {

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

    // ==================== TRIP MANAGEMENT ====================

    fun getAllTrips(): List<Trip> {
        return try {
            val json = prefs.getString(KEY_TRIPS, null) ?: return emptyList()
            val type = object : TypeToken<List<Trip>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trips: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveTrip(trip: Trip): Boolean {
        return try {
            val trips = getAllTrips().toMutableList()

            // Remove existing trip with same ID
            trips.removeAll { it.id == trip.id }

            // Add new/updated trip
            trips.add(trip)

            // Save to preferences
            val json = gson.toJson(trips)
            val success = prefs.edit().putString(KEY_TRIPS, json).commit()

            if (success) {
                Log.d(TAG, "✅ Trip saved: ${trip.name}")
            } else {
                Log.e(TAG, "❌ Failed to commit trip to SharedPreferences")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving trip: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun deleteTrip(tripId: String): Boolean {
        return try {
            val trips = getAllTrips().toMutableList()
            val removed = trips.removeAll { it.id == tripId }

            if (removed) {
                val json = gson.toJson(trips)
                prefs.edit().putString(KEY_TRIPS, json).apply()

                // If this was the active trip, clear it
                if (getActiveTripId() == tripId) {
                    clearActiveTrip()
                }

                Log.d(TAG, "✅ Trip deleted: $tripId")
                true
            } else {
                Log.w(TAG, "⚠️ Trip not found for deletion: $tripId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting trip: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getTripById(tripId: String): Trip? {
        return try {
            getAllTrips().find { it.id == tripId }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting trip by ID: ${e.message}")
            null
        }
    }

    fun setActiveTrip(tripId: String): Boolean {
        return try {
            val trip = getTripById(tripId)
            if (trip != null) {
                prefs.edit().putString(KEY_ACTIVE_TRIP_ID, tripId).apply()
                Log.d(TAG, "✅ Active trip set: ${trip.name}")
                true
            } else {
                Log.e(TAG, "❌ Trip not found: $tripId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting active trip: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getActiveTrip(): Trip? {
        return try {
            val tripId = getActiveTripId() ?: return null
            getTripById(tripId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active trip: ${e.message}")
            null
        }
    }

    fun getActiveTripId(): String? {
        return try {
            prefs.getString(KEY_ACTIVE_TRIP_ID, null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active trip ID: ${e.message}")
            null
        }
    }

    fun clearActiveTrip() {
        try {
            prefs.edit().remove(KEY_ACTIVE_TRIP_ID).apply()
            Log.d(TAG, "✅ Active trip cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing active trip: ${e.message}")
        }
    }

    fun completeTrip(tripId: String): Boolean {
        return try {
            val trip = getTripById(tripId) ?: return false
            clearActiveTrip()
            Log.d(TAG, "✅ Trip completed: ${trip.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error completing trip: ${e.message}")
            false
        }
    }

    fun startTrip(tripId: String): Boolean {
        return try {
            setActiveTrip(tripId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting trip: ${e.message}")
            false
        }
    }

    // ==================== CONTACT MANAGEMENT ====================

    fun getAllContacts(): List<Contact> {
        return try {
            val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading contacts: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveContact(contact: Contact): Boolean {
        return try {
            val contacts = getAllContacts().toMutableList()

            // Remove existing contact with same ID
            contacts.removeAll { it.id == contact.id }

            // Add new/updated contact
            contacts.add(contact)

            // Save to preferences
            val json = gson.toJson(contacts)
            val success = prefs.edit().putString(KEY_CONTACTS, json).commit()

            if (success) {
                Log.d(TAG, "✅ Contact saved: ${contact.name}")
            } else {
                Log.e(TAG, "❌ Failed to commit contact to SharedPreferences")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving contact: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun deleteContact(contactId: String): Boolean {
        return try {
            val contacts = getAllContacts().toMutableList()
            val removed = contacts.removeAll { it.id == contactId }

            if (removed) {
                val json = gson.toJson(contacts)
                prefs.edit().putString(KEY_CONTACTS, json).apply()
                Log.d(TAG, "✅ Contact deleted: $contactId")
                true
            } else {
                Log.w(TAG, "⚠️ Contact not found for deletion: $contactId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting contact: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getContactById(contactId: String): Contact? {
        return try {
            getAllContacts().find { it.id == contactId }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting contact by ID: ${e.message}")
            null
        }
    }

    fun getContactsForTrip(trip: Trip): List<Contact> {
        return try {
            getAllContacts()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting contacts for trip: ${e.message}")
            emptyList()
        }
    }

    // ==================== UTILITY METHODS ====================

    fun clearAllData() {
        try {
            prefs.edit()
                .remove(KEY_TRIPS)
                .remove(KEY_CONTACTS)
                .remove(KEY_ACTIVE_TRIP_ID)
                .apply()
            Log.d(TAG, "✅ All data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing data: ${e.message}")
        }
    }

    fun exportData(): String {
        return try {
            val data = mapOf(
                "trips" to getAllTrips(),
                "contacts" to getAllContacts()
            )
            gson.toJson(data)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error exporting data: ${e.message}")
            "{}"
        }
    }

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
            e.printStackTrace()
            false
        }
    }

    fun getStatistics(): Map<String, Any> {
        return try {
            val trips = getAllTrips()

            mapOf(
                "totalTrips" to trips.size,
                "totalContacts" to getAllContacts().size,
                "totalCheckpoints" to trips.sumOf { it.checkpoints.size },
                "activeTrip" to (getActiveTrip() != null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting statistics: ${e.message}")
            mapOf(
                "totalTrips" to 0,
                "totalContacts" to 0,
                "totalCheckpoints" to 0,
                "activeTrip" to false
            )
        }
    }
}