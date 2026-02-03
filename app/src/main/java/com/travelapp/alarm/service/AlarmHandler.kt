package com.travelapp.alarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.travelapp.alarm.R

object AlarmHandler {

    private const val TAG = "AlarmHandler"
    private const val CHANNEL_ID_ALARM = "alarm_channel"
    private const val NOTIFICATION_ID_ALARM = 2001
    private const val NOTIFICATION_ID_CHECKPOINT = 2002

    fun triggerDestinationAlarm(context: Context, geofenceId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔔 DESTINATION ALARM TRIGGERED!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        vibrateAlarm(context)

        showAlarmNotification(
            context,
            title = "🎯 DESTINATION REACHED!",
            message = "You've arrived! Time to wake up!",
            notificationId = NOTIFICATION_ID_ALARM
        )

        playAlarmSound(context)
    }

    fun triggerDestinationNotification(context: Context, geofenceId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 DESTINATION NOTIFICATION TRIGGERED!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        vibrateNotification(context)

        showAlarmNotification(
            context,
            title = "📍 Almost There!",
            message = "You're approaching your destination",
            notificationId = NOTIFICATION_ID_ALARM
        )
    }

    fun triggerCheckpointAlarm(context: Context, checkpointId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ CHECKPOINT ALARM TRIGGERED!")
        Log.d(TAG, "   Checkpoint ID: $checkpointId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        vibrateNotification(context)

        showAlarmNotification(
            context,
            title = "✅ Checkpoint Reached!",
            message = "You've passed a checkpoint",
            notificationId = NOTIFICATION_ID_CHECKPOINT
        )
    }

    private fun vibrateAlarm(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        Log.d(TAG, "📳 Vibration triggered (alarm pattern)")
    }

    private fun vibrateNotification(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 300, 200, 300)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        Log.d(TAG, "📳 Vibration triggered (notification pattern)")
    }

    private fun playAlarmSound(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone.play()

            Log.d(TAG, "🔊 Alarm sound playing")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                ringtone.stop()
                Log.d(TAG, "🔇 Alarm sound stopped")
            }, 30000)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to play alarm sound: ${e.message}")
        }
    }

    private fun showAlarmNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(context, notificationManager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(notificationId, notification)

        Log.d(TAG, "📬 Notification shown: $title")
    }

    private fun createNotificationChannel(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Location-based alarms and alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(alarmChannel)
        }
    }
}