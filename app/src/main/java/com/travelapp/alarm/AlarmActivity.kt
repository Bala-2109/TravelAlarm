package com.travelapp.alarm

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private lateinit var tvAlarmTitle: TextView
    private lateinit var tvLocationName: TextView
    private lateinit var tvTripName: TextView
    private lateinit var btnDismiss: Button
    private lateinit var btnSnooze: Button

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "AlarmActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚨 ALARM ACTIVITY STARTED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        initializeViews()
        loadExtras()
        startAlarm()
        setupButtons()
    }

    private fun initializeViews() {
        tvAlarmTitle = findViewById(R.id.tvAlarmTitle)
        tvLocationName = findViewById(R.id.tvLocationName)
        tvTripName = findViewById(R.id.tvTripName)
        btnDismiss = findViewById(R.id.btnDismiss)
        btnSnooze = findViewById(R.id.btnSnooze)

        Log.d(TAG, "✅ Views initialized")
    }

    private fun loadExtras() {
        val tripName = intent.getStringExtra("TRIP_NAME") ?: "Unknown Trip"
        val locationName = intent.getStringExtra("LOCATION_NAME") ?: "Unknown Location"
        val isDestination = intent.getBooleanExtra("IS_DESTINATION", false)

        Log.d(TAG, "Trip: $tripName")
        Log.d(TAG, "Location: $locationName")
        Log.d(TAG, "Is Destination: $isDestination")

        // Update UI
        tvAlarmTitle.text = if (isDestination) {
            "🏁 DESTINATION REACHED!"
        } else {
            "📍 CHECKPOINT REACHED!"
        }

        tvLocationName.text = locationName
        tvTripName.text = "Trip: $tripName"
    }

    private fun startAlarm() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔊 STARTING ALARM SOUND & VIBRATION")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Start alarm sound
        startAlarmSound()

        // Start vibration
        startVibration()
    }

    private fun startAlarmSound() {
        try {
            // Get default alarm sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)

                // Set audio attributes for alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                isLooping = true
                prepare()
                start()
            }

            Log.d(TAG, "✅ Alarm sound started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start alarm sound: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create vibration pattern
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(vibrationEffect)
            } else {
                // Fallback for older devices
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

            Log.d(TAG, "✅ Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start vibration: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        btnDismiss.setOnClickListener {
            Log.d(TAG, "🔕 Dismiss button clicked")
            dismissAlarm()
        }

        btnSnooze.setOnClickListener {
            Log.d(TAG, "⏰ Snooze button clicked")
            snoozeAlarm()
        }
    }

    private fun dismissAlarm() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ ALARM DISMISSED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        stopAlarm()
        finish()
    }

    private fun snoozeAlarm() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "⏰ ALARM SNOOZED (10 minutes)")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // TODO: Implement snooze logic
        // For now, just dismiss
        stopAlarm()
        finish()
    }

    private fun stopAlarm() {
        Log.d(TAG, "🔇 Stopping alarm sound & vibration")

        // Stop sound
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d(TAG, "✅ Sound stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping sound: ${e.message}")
        }

        // Stop vibration
        try {
            vibrator?.cancel()
            Log.d(TAG, "✅ Vibration stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping vibration: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔴 AlarmActivity DESTROYED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        stopAlarm()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Prevent back button from dismissing alarm
        Log.d(TAG, "⚠️ Back button pressed - ignored")
    }
}