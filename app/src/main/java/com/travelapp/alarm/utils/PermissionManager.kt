package com.travelapp.alarm.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    companion object {
        private const val TAG = "PermissionManager"
        private const val PERMISSION_REQUEST_CODE = 1001

        // All required permissions
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Callback interface for permission results
    interface PermissionCallback {
        fun onPermissionResult(allGranted: Boolean)
    }

    private var permissionCallback: PermissionCallback? = null

    /**
     * Request all required permissions
     */
    fun requestAllPermissions(callback: PermissionCallback) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔐 REQUESTING ALL APP PERMISSIONS")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        this.permissionCallback = callback

        // Check which permissions are not granted
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "✅ All permissions already granted!")
            callback.onPermissionResult(true)
            return
        }

        Log.d(TAG, "📋 Requesting ${permissionsToRequest.size} permissions:")
        permissionsToRequest.forEach { permission ->
            Log.d(TAG, "   - ${permission.substringAfterLast(".")}")
        }

        // Request the permissions
        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Handle permission request results
     * Call this from Activity's onRequestPermissionsResult
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 PERMISSION RESULTS RECEIVED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Check if all permissions were granted
        val allGranted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        // Log individual results
        permissions.forEachIndexed { index, permission ->
            val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
            val status = if (granted) "✅ GRANTED" else "❌ DENIED"
            Log.d(TAG, "$status: ${permission.substringAfterLast(".")}")
        }

        if (allGranted) {
            Log.d(TAG, "✅ ALL PERMISSIONS GRANTED!")
        } else {
            Log.e(TAG, "❌ SOME PERMISSIONS DENIED")

            // Log which permissions were denied
            permissions.forEachIndexed { index, permission ->
                if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "   Denied: ${permission.substringAfterLast(".")}")
                }
            }
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Notify callback
        permissionCallback?.onPermissionResult(allGranted)
        permissionCallback = null
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get list of denied permissions
     */
    fun getDeniedPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && coarseLocation
    }

    /**
     * Check if communication permissions are granted (SMS, Call)
     */
    fun hasCommunicationPermissions(): Boolean {
        val sms = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val call = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return sms && call
    }
}