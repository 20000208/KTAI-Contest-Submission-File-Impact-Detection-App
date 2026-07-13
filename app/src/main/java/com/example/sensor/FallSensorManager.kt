package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.acos
import kotlin.math.sqrt

data class ShockEventData(
    val maxAccel: Float,
    val maxGyro: Float,
    val initialTilt: Float,
    val finalTilt: Float,
    val postShockActivity: Float, // Standard deviation of acceleration after shock
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

class FallSensorManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onShockDetected: (ShockEventData) -> Unit,
    private val onSensorValuesChanged: (x: Float, y: Float, z: Float, tilt: Float, rotSpeed: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Current real-time values
    private var currentX = 0f
    private var currentY = 0f
    private var currentZ = 0f
    private var currentTilt = 0f
    private var currentRotSpeed = 0f

    // Sensitivity thresholds (m/s^2)
    private var shockThreshold = 25.0f // Medium default

    // Monitoring state
    private var isMonitoring = false
    private var isAnalyzingShock = false
    private var shockAnalysisJob: Job? = null

    // Rolling history of accelerometer magnitude for post-shock activity calculation
    private val recentAccelMagnitudes = mutableListOf<Float>()
    private val historyLimit = 50

    fun setSensitivity(sensitivity: String) {
        shockThreshold = when (sensitivity.lowercase()) {
            "high" -> 16.0f  // Sensitive (trips easily)
            "low" -> 35.0f   // Hard shock required
            else -> 25.0f    // Medium
        }
        Log.d("FallSensorManager", "Sensitivity set to $sensitivity (Threshold: ${shockThreshold}m/s^2)")
    }

    fun setSensitivityThreshold(threshold: Float) {
        shockThreshold = threshold
        Log.d("FallSensorManager", "Threshold set directly to ${shockThreshold}m/s^2")
    }

    fun start() {
        if (isMonitoring) return
        isMonitoring = true
        recentAccelMagnitudes.clear()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        Log.d("FallSensorManager", "Sensor listeners registered")
    }

    fun stop() {
        if (!isMonitoring) return
        isMonitoring = false
        sensorManager.unregisterListener(this)
        shockAnalysisJob?.cancel()
        isAnalyzingShock = false
        Log.d("FallSensorManager", "Sensor listeners unregistered")
    }

    fun isSensorsAvailable(): Boolean {
        return accelerometer != null && gyroscope != null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        processRawSensorData(event.sensor.type, event.values)
    }

    fun processRawSensorData(sensorType: Int, values: FloatArray) {
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentX = values[0]
                currentY = values[1]
                currentZ = values[2]

                val magnitude = sqrt(currentX * currentX + currentY * currentY + currentZ * currentZ)
                
                // Keep rolling history
                synchronized(recentAccelMagnitudes) {
                    recentAccelMagnitudes.add(magnitude)
                    if (recentAccelMagnitudes.size > historyLimit) {
                        recentAccelMagnitudes.removeAt(0)
                    }
                }

                // Estimate tilt angle relative to Z axis (vertical orientation)
                // acos(z / gravity_magnitude)
                currentTilt = if (magnitude > 0.1f) {
                    val angleRad = acos((currentZ / magnitude).coerceIn(-1.0f, 1.0f))
                    (angleRad * (180.0 / Math.PI)).toFloat()
                } else {
                    0f
                }

                onSensorValuesChanged(currentX, currentY, currentZ, currentTilt, currentRotSpeed)

                // Check for shock threshold
                if (magnitude > shockThreshold && !isAnalyzingShock) {
                    triggerShockAnalysis(magnitude, currentTilt)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = values[0]
                val gy = values[1]
                val gz = values[2]
                currentRotSpeed = (sqrt(gx * gx + gy * gy + gz * gz) * (180.0 / Math.PI)).toFloat() // in deg/s
                onSensorValuesChanged(currentX, currentY, currentZ, currentTilt, currentRotSpeed)
            }
        }
    }

    private fun triggerShockAnalysis(initialShockMagnitude: Float, shockTilt: Float) {
        isAnalyzingShock = true
        val startTime = System.currentTimeMillis()

        shockAnalysisJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                Log.d("FallSensorManager", "Shock detected! Magnitude: $initialShockMagnitude. Analyzing post-shock state...")
                
                var maxAccel = initialShockMagnitude
                var maxGyro = currentRotSpeed
                val postShockBuffer = mutableListOf<Float>()

                // 1. Monitor the immediate shock peak for 150ms
                val peakDuration = 150L
                val checkInterval = 15L
                var elapsed = 0L
                while (elapsed < peakDuration) {
                    delay(checkInterval)
                    elapsed += checkInterval
                    val currentMag = synchronized(recentAccelMagnitudes) { recentAccelMagnitudes.lastOrNull() ?: 9.8f }
                    if (currentMag > maxAccel) maxAccel = currentMag
                    if (currentRotSpeed > maxGyro) maxGyro = currentRotSpeed
                }

                // 2. Collect post-shock activity data for 1.0 second (to detect movement vs immobility)
                val postDuration = 1000L
                elapsed = 0L
                while (elapsed < postDuration) {
                    delay(40L)
                    elapsed += 40L
                    val currentMag = synchronized(recentAccelMagnitudes) { recentAccelMagnitudes.lastOrNull() ?: 9.8f }
                    postShockBuffer.add(currentMag)
                }

                // Calculate post-shock activity (Standard Deviation of acceleration)
                val avg = if (postShockBuffer.isNotEmpty()) postShockBuffer.average() else 9.8
                val variance = if (postShockBuffer.isNotEmpty()) {
                    postShockBuffer.map { (it - avg) * (it - avg) }.sum() / postShockBuffer.size
                } else {
                    0.0
                }
                val stdDev = sqrt(variance).toFloat()

                val finalTilt = currentTilt
                val duration = System.currentTimeMillis() - startTime

                val eventData = ShockEventData(
                    maxAccel = maxAccel,
                    maxGyro = maxGyro,
                    initialTilt = shockTilt,
                    finalTilt = finalTilt,
                    postShockActivity = stdDev,
                    durationMs = duration
                )

                onShockDetected(eventData)
            } catch (e: Exception) {
                Log.e("FallSensorManager", "Error analyzing shock", e)
            } finally {
                isAnalyzingShock = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
