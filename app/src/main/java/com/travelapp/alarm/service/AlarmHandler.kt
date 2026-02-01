package com.travelapp.alarm.service

import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.travelapp.alarm.R

/**
 * Handles alarm triggers, sounds, and notifications
 */
class AlarmHandler : Service() {
    
    companion object {
        private const val TAG = "AlarmHandler"
        
        const val ACTION_ALARM_TRIGGERED = "ACTION_ALARM_TRIGGERED"
        const val ACTION_CHECKPOINT_REACHED = "ACTION_CHECKPOINT_REACHED"
        const val ACTION_DESTINATION_REACHED = "ACTION_DESTINATION_REACHED"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
        const val EXTRA_CHECKPOINT_ID = "EXTRA_CHECKPOINT_ID"
        const val EXTRA_TRIGGER_TYPE = "EXTRA_TRIGGER_TYPE"
        
        private const val ALARM_NOTIFICATION_ID = 2001
        private const val CHECKPOINT_NOTIFICATION_ID = 2002
        private const val ARRIVAL_NOTIFICATION_ID = 2003
    }
    
    private var vibrator: Vibrator? = null
    private var isAlarming = false
    
    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ALARM_TRIGGERED -> {
                val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
                handleAlarmTrigger(tripId)
            }
            ACTION_CHECKPOINT_REACHED -> {
                val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
                val checkpointId = intent.getStringExtra(EXTRA_CHECKPOINT_ID)
                handleCheckpointReached(tripId, checkpointId)
            }
            ACTION_DESTINATION_REACHED -> {
                val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
                handleDestinationReached(tripId)
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun handleAlarmTrigger(tripId: String?) {
        Log.d(TAG, "Handling alarm trigger for trip: $tripId")
        
        if (isAlarming) {
            Log.d(TAG, "Alarm already ringing")
            return
        }
        
        isAlarming = true
        
        // Vibrate
        startVibration()
        
        // Show notification
        showAlarmNotification(tripId)
        
        // TODO: Play alarm sound
        // TODO: Wake up screen
        // TODO: Update trip status
        // TODO: Notify contacts
    }
    
    private fun handleCheckpointReached(tripId: String?, checkpointId: String?) {
        Log.d(TAG, "Checkpoint reached - Trip: $tripId, Checkpoint: $checkpointId")
        
        // Vibrate briefly
        vibrateOnce()
        
        // Show notification
        showCheckpointNotification(tripId, checkpointId)
        
        // TODO: Mark checkpoint as reached in database
        // TODO: Notify contacts if enabled
        // TODO: Update trip progress
    }
    
    private fun handleDestinationReached(tripId: String?) {
        Log.d(TAG, "Destination reached for trip: $tripId")
        
        // Stop any ongoing alarm
        stopAlarm()
        
        // Vibrate
        vibratePattern()
        
        // Show arrival notification
        showArrivalNotification(tripId)
        
        // TODO: Mark trip as completed
        // TODO: Notify all contacts
        // TODO: Stop location tracking
    }
    
    private fun startVibration() {
        // Continuous vibration pattern: vibrate 1s, pause 0.5s, repeat
        val pattern = longArrayOf(0, 1000, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0) // 0 = repeat
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    private fun vibrateOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
    }
    
    private fun vibratePattern() {
        // Arrival pattern: short-short-long
        val pattern = longArrayOf(0, 200, 100, 200, 100, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, -1) // -1 = no repeat
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
    }
    
    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm")
        isAlarming = false
        stopVibration()
        
        // Dismiss alarm notification
        NotificationManagerCompat.from(this).cancel(ALARM_NOTIFICATION_ID)
    }
    
    private fun showAlarmNotification(tripId: String?) {
        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("🚨 Destination Approaching!")
            .setContentText("You're near your destination. Time to wake up!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()
        
        NotificationManagerCompat.from(this)
            .notify(ALARM_NOTIFICATION_ID, notification)
    }
    
    private fun showCheckpointNotification(tripId: String?, checkpointId: String?) {
        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("✅ Checkpoint Reached")
            .setContentText("You've reached a checkpoint on your journey")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(this)
            .notify(CHECKPOINT_NOTIFICATION_ID, notification)
    }
    
    private fun showArrivalNotification(tripId: String?) {
        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("🎯 Destination Reached!")
            .setContentText("You've arrived at your destination")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(this)
            .notify(ARRIVAL_NOTIFICATION_ID, notification)
    }
}
