package com.travelapp.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.travelapp.alarm.data.model.AppSettings

/**
 * Monitors battery level and predicts battery death for trajectory calculation
 */
class BatteryMonitor(
    private val context: Context,
    private val settings: AppSettings
) {
    
    companion object {
        private const val TAG = "BatteryMonitor"
    }
    
    private var currentBatteryLevel: Int = 100
    private var isCharging: Boolean = false
    private var batteryHistory = mutableListOf<BatteryReading>()
    
    // Listeners for battery events
    private val batteryListeners = mutableListOf<BatteryListener>()
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    handleBatteryChanged(intent)
                }
                Intent.ACTION_BATTERY_LOW -> {
                    handleBatteryLow()
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    handleBatteryOkay()
                }
            }
        }
    }
    
    /**
     * Start monitoring battery
     */
    fun startMonitoring() {
        Log.d(TAG, "Starting battery monitoring")
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        
        context.registerReceiver(batteryReceiver, filter)
        
        // Get initial battery status
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let { handleBatteryChanged(it) }
    }
    
    /**
     * Stop monitoring battery
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping battery monitoring")
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, ignore
        }
    }
    
    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        
        if (level >= 0 && scale > 0) {
            val batteryPct = (level * 100 / scale)
            val wasCharging = isCharging
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL
            
            if (batteryPct != currentBatteryLevel || wasCharging != isCharging) {
                val oldLevel = currentBatteryLevel
                currentBatteryLevel = batteryPct
                
                // Record battery reading
                recordBatteryReading(batteryPct)
                
                Log.d(TAG, "Battery: $batteryPct%, Charging: $isCharging")
                
                // Check thresholds
                checkBatteryThresholds(oldLevel, batteryPct)
                
                // Notify listeners
                notifyBatteryChanged(batteryPct, isCharging)
            }
        }
    }
    
    private fun handleBatteryLow() {
        Log.w(TAG, "Battery LOW warning")
        notifyBatteryWarning(currentBatteryLevel, "Battery critically low")
    }
    
    private fun handleBatteryOkay() {
        Log.d(TAG, "Battery OKAY")
    }
    
    private fun recordBatteryReading(level: Int) {
        val reading = BatteryReading(
            timestamp = System.currentTimeMillis(),
            level = level,
            isCharging = isCharging
        )
        
        batteryHistory.add(reading)
        
        // Keep only last 100 readings
        if (batteryHistory.size > 100) {
            batteryHistory.removeAt(0)
        }
    }
    
    private fun checkBatteryThresholds(oldLevel: Int, newLevel: Int) {
        settings.batteryThresholds.forEach { threshold ->
            if (oldLevel > threshold && newLevel <= threshold && !isCharging) {
                Log.w(TAG, "Battery threshold reached: $threshold%")
                notifyBatteryThreshold(threshold)
            }
        }
    }
    
    /**
     * Calculate battery drain rate (% per minute)
     */
    fun calculateBatteryDrainRate(): Double {
        if (batteryHistory.size < 2 || isCharging) {
            return 0.0
        }
        
        // Use last 10 readings for accuracy
        val recentReadings = batteryHistory.takeLast(10)
        if (recentReadings.size < 2) {
            return 0.0
        }
        
        val first = recentReadings.first()
        val last = recentReadings.last()
        
        val batteryDrop = first.level - last.level
        val timeElapsedMinutes = (last.timestamp - first.timestamp) / 60000.0
        
        if (timeElapsedMinutes <= 0) {
            return 0.0
        }
        
        return batteryDrop / timeElapsedMinutes
    }
    
    /**
     * Predict when battery will die (in minutes)
     */
    fun predictBatteryDeathMinutes(): Int? {
        if (isCharging || currentBatteryLevel >= 100) {
            return null // Not applicable when charging or full
        }
        
        val drainRate = calculateBatteryDrainRate()
        if (drainRate <= 0) {
            return null // No drain or insufficient data
        }
        
        val minutesRemaining = currentBatteryLevel / drainRate
        return minutesRemaining.toInt()
    }
    
    /**
     * Get current battery level
     */
    fun getCurrentBatteryLevel(): Int = currentBatteryLevel
    
    /**
     * Check if battery is charging
     */
    fun isCharging(): Boolean = isCharging
    
    /**
     * Add battery listener
     */
    fun addListener(listener: BatteryListener) {
        batteryListeners.add(listener)
    }
    
    /**
     * Remove battery listener
     */
    fun removeListener(listener: BatteryListener) {
        batteryListeners.remove(listener)
    }
    
    private fun notifyBatteryChanged(level: Int, charging: Boolean) {
        batteryListeners.forEach { it.onBatteryChanged(level, charging) }
    }
    
    private fun notifyBatteryThreshold(threshold: Int) {
        batteryListeners.forEach { it.onBatteryThreshold(threshold) }
    }
    
    private fun notifyBatteryWarning(level: Int, message: String) {
        batteryListeners.forEach { it.onBatteryWarning(level, message) }
    }
    
    /**
     * Battery reading data class
     */
    data class BatteryReading(
        val timestamp: Long,
        val level: Int,
        val isCharging: Boolean
    )
    
    /**
     * Battery event listener interface
     */
    interface BatteryListener {
        fun onBatteryChanged(level: Int, isCharging: Boolean)
        fun onBatteryThreshold(threshold: Int)
        fun onBatteryWarning(level: Int, message: String)
    }
}
