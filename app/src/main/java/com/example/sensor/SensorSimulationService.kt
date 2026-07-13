package com.example.sensor

import android.hardware.Sensor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class SensorSimulationService(
    private val sensorManager: FallSensorManager,
    private val scope: CoroutineScope
) {
    private var simulationJob: Job? = null
    
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating = _isSimulating.asStateFlow()

    private val _simulationPhase = MutableStateFlow("IDLE")
    val simulationPhase = _simulationPhase.asStateFlow()

    fun startSimulation(targetEmergencyThreshold: Float, onComplete: () -> Unit = {}) {
        if (_isSimulating.value) return
        _isSimulating.value = true

        simulationJob = scope.launch(Dispatchers.Default) {
            try {
                Log.d("SensorSimulationService", "Starting real-time sensor simulation stream...")
                
                val intervalMs = 40L
                val random = Random(System.currentTimeMillis())

                // Phase 1: Normal Walking (100 ms)
                _simulationPhase.value = "WALKING"
                var elapsed = 0L
                while (elapsed < 100) {
                    // Simulating a walking pattern: periodic oscillations
                    val timeFactor = elapsed.toDouble() / 150.0
                    val waveX = Math.sin(timeFactor) * 1.5
                    val waveY = Math.cos(timeFactor) * 2.0
                    val waveZ = 9.8 + Math.sin(timeFactor * 2.0) * 1.2
                    
                    val x = (waveX + random.nextDouble(-0.3, 0.3)).toFloat()
                    val y = (waveY + random.nextDouble(-0.3, 0.3)).toFloat()
                    val z = (waveZ + random.nextDouble(-0.3, 0.3)).toFloat()

                    // Gyroscope rotation: small variations during walking
                    val gx = (Math.sin(timeFactor) * 0.4 + random.nextDouble(-0.1, 0.1)).toFloat()
                    val gy = (Math.cos(timeFactor) * 0.3 + random.nextDouble(-0.1, 0.1)).toFloat()
                    val gz = (Math.sin(timeFactor * 1.5) * 0.2 + random.nextDouble(-0.1, 0.1)).toFloat()

                    sensorManager.processRawSensorData(Sensor.TYPE_ACCELEROMETER, floatArrayOf(x, y, z))
                    sensorManager.processRawSensorData(Sensor.TYPE_GYROSCOPE, floatArrayOf(gx, gy, gz))

                    delay(intervalMs)
                    elapsed += intervalMs
                }

                // Phase 2: Loss of Balance / Falling Descent (100 ms)
                _simulationPhase.value = "FALL_DESCENT"
                elapsed = 0L
                while (elapsed < 100) {
                    // Free fall: accelerometer magnitude drops close to zero, rotation starts to spike
                    val progress = elapsed.toFloat() / 100f
                    val x = (random.nextDouble(-1.0, 1.0) * (1f - progress)).toFloat()
                    val y = (random.nextDouble(-1.0, 1.0) * (1f - progress)).toFloat()
                    val z = (2.0 + random.nextDouble(-0.5, 0.5) * (1f - progress)).toFloat() // low gravity simulation

                    // Spinning rapidly as falling
                    val gx = (2.0 + progress * 4.0).toFloat()
                    val gy = (-1.5 - progress * 3.0).toFloat()
                    val gz = (1.0 + progress * 2.0).toFloat()

                    sensorManager.processRawSensorData(Sensor.TYPE_ACCELEROMETER, floatArrayOf(x, y, z))
                    sensorManager.processRawSensorData(Sensor.TYPE_GYROSCOPE, floatArrayOf(gx, gy, gz))

                    delay(intervalMs)
                    elapsed += intervalMs
                }

                // Phase 3: Sudden Ground Impact (80 ms)
                _simulationPhase.value = "IMPACT"
                elapsed = 0L
                val targetPeak = targetEmergencyThreshold + 5.0f
                val componentVal = (targetPeak / Math.sqrt(3.0)).toFloat()
                while (elapsed < 80) {
                    // Extreme shock: acceleration magnitude is scaled to exceed target threshold
                    val x = componentVal + random.nextDouble(-1.5, 1.5).toFloat()
                    val y = componentVal + random.nextDouble(-1.5, 1.5).toFloat()
                    val z = componentVal + random.nextDouble(-1.5, 1.5).toFloat()

                    val gx = (6.0f + random.nextDouble(-1.0, 1.0).toFloat())
                    val gy = (-5.0f + random.nextDouble(-1.0, 1.0).toFloat())
                    val gz = (4.0f + random.nextDouble(-1.0, 1.0).toFloat()) // High rotation speed

                    sensorManager.processRawSensorData(Sensor.TYPE_ACCELEROMETER, floatArrayOf(x, y, z))
                    sensorManager.processRawSensorData(Sensor.TYPE_GYROSCOPE, floatArrayOf(gx, gy, gz))

                    delay(intervalMs)
                    elapsed += intervalMs
                }

                // Phase 4: Post-Impact Unconsciousness / Immobility (1200 ms)
                _simulationPhase.value = "IMMOBILITY"
                elapsed = 0L
                while (elapsed < 1200) {
                    // Lies flat on the ground. Orientation has changed (z close to 0, x or y close to 9.8)
                    // Let's assume the device lies flat on its side (x = 9.6, y = 1.5, z = 0.5)
                    // Very small micro-noises to simulate absolute still/immobility
                    val x = (9.6f + random.nextDouble(-0.02, 0.02).toFloat())
                    val y = (1.5f + random.nextDouble(-0.02, 0.02).toFloat())
                    val z = (0.5f + random.nextDouble(-0.02, 0.02).toFloat())

                    // Almost zero angular velocity
                    val gx = random.nextDouble(-0.01, 0.01).toFloat()
                    val gy = random.nextDouble(-0.01, 0.01).toFloat()
                    val gz = random.nextDouble(-0.01, 0.01).toFloat()

                    sensorManager.processRawSensorData(Sensor.TYPE_ACCELEROMETER, floatArrayOf(x, y, z))
                    sensorManager.processRawSensorData(Sensor.TYPE_GYROSCOPE, floatArrayOf(gx, gy, gz))

                    delay(intervalMs)
                    elapsed += intervalMs
                }

                Log.d("SensorSimulationService", "Sensor simulation stream completed successfully.")
            } catch (e: Exception) {
                Log.e("SensorSimulationService", "Sensor simulation was interrupted", e)
            } finally {
                _isSimulating.value = false
                _simulationPhase.value = "IDLE"
                scope.launch(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        _isSimulating.value = false
        _simulationPhase.value = "IDLE"
    }
}
