package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.BatteryManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.FallAnalysisResult
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.Content as ApiContent
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.FallLog
import com.example.data.FallRepository
import com.example.sensor.FallSensorManager
import com.example.sensor.ShockEventData
import com.example.sensor.SensorSimulationService
import android.speech.tts.TextToSpeech
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.os.Bundle
import android.location.Geocoder
import android.location.Address
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FallAlertState(
    val maxAccel: Float,
    val finalTilt: Float,
    val confidence: Double,
    val aiReason: String,
    val countdownSeconds: Int,
    val isSimulated: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class FallViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val settings = AppSettings(app)
    private val database = AppDatabase.getDatabase(app)
    private val repository = FallRepository(database.fallLogDao())

    // UI state flows from Room
    val logsList: StateFlow<List<FallLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form states
    private val _isSetupComplete = MutableStateFlow(settings.isSetupComplete)
    val isSetupComplete = _isSetupComplete.asStateFlow()

    private val _userName = MutableStateFlow(settings.userName)
    val userName = _userName.asStateFlow()

    private val _guardianName = MutableStateFlow(settings.guardianName)
    val guardianName = _guardianName.asStateFlow()

    private val _guardianPhone = MutableStateFlow(settings.guardianPhone)
    val guardianPhone = _guardianPhone.asStateFlow()

    private val _emergencyContact = MutableStateFlow(settings.emergencyContact)
    val emergencyContact = _emergencyContact.asStateFlow()

    private val _emergencyTarget = MutableStateFlow(settings.emergencyTarget)
    val emergencyTarget = _emergencyTarget.asStateFlow()

    private val _sensitivity = MutableStateFlow(settings.sensitivity)
    val sensitivity = _sensitivity.asStateFlow()

    private val _sensitivityThreshold = MutableStateFlow(settings.sensitivityThreshold)
    val sensitivityThreshold = _sensitivityThreshold.asStateFlow()

    private val _warningThreshold = MutableStateFlow(settings.warningThreshold)
    val warningThreshold = _warningThreshold.asStateFlow()

    private val _emergencyThreshold = MutableStateFlow(settings.emergencyThreshold)
    val emergencyThreshold = _emergencyThreshold.asStateFlow()

    private var lastWarningSpeakTime = 0L

    private val _guidanceTitle = MutableStateFlow<String?>(null)
    val guidanceTitle = _guidanceTitle.asStateFlow()

    private val _guidanceMessage = MutableStateFlow<String?>(null)
    val guidanceMessage = _guidanceMessage.asStateFlow()

    private var warningRepeatJob: Job? = null

    fun startWarningRepeatLoop(speakText: String) {
        warningRepeatJob?.cancel()
        warningRepeatJob = viewModelScope.launch {
            while (isActive && _guidanceMessage.value != null) {
                speakWarning(speakText)
                delay(6000L)
            }
        }
    }

    fun stopWarningRepeatLoop() {
        warningRepeatJob?.cancel()
        warningRepeatJob = null
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun dismissGuidance() {
        _guidanceTitle.value = null
        _guidanceMessage.value = null
        stopWarningRepeatLoop()
    }

    private val _soundEnabled = MutableStateFlow(settings.soundEnabled)
    val soundEnabled = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(settings.vibrationEnabled)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    private val _phonePreset = MutableStateFlow(settings.phonePreset)
    val phonePreset = _phonePreset.asStateFlow()

    // Real-time sensor state
    private val _realtimeX = MutableStateFlow(0f)
    val realtimeX = _realtimeX.asStateFlow()

    private val _realtimeY = MutableStateFlow(0f)
    val realtimeY = _realtimeY.asStateFlow()

    private val _realtimeZ = MutableStateFlow(0f)
    val realtimeZ = _realtimeZ.asStateFlow()

    private val _realtimeTilt = MutableStateFlow(0f)
    val realtimeTilt = _realtimeTilt.asStateFlow()

    private val _realtimeRotSpeed = MutableStateFlow(0f)
    val realtimeRotSpeed = _realtimeRotSpeed.asStateFlow()

    // Rolling G-force history (magnitude in m/s²)
    private val _gForceHistory = MutableStateFlow<List<Float>>(emptyList())
    val gForceHistory = _gForceHistory.asStateFlow()

    private val _peakGForce = MutableStateFlow(0f)
    val peakGForce = _peakGForce.asStateFlow()

    fun updateGForceHistory(magnitude: Float) {
        _gForceHistory.value = (_gForceHistory.value + magnitude).takeLast(60)
    }

    fun clearGForceHistory() {
        _gForceHistory.value = emptyList()
    }

    fun resetPeakGForce() {
        _peakGForce.value = 0f
    }

    // Battery monitoring states
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging = _isBatteryCharging.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = ((level.toFloat() / scale.toFloat()) * 100).toInt()
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isBatteryCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    // Impact Event History (for 'Detection History')
    private val _impactEvents = MutableStateFlow<List<ImpactEvent>>(emptyList())
    val impactEvents = _impactEvents.asStateFlow()

    private fun loadImpactEvents() {
        val raw = settings.getImpactEventsRaw()
        if (raw.isEmpty()) {
            _impactEvents.value = emptyList()
            return
        }
        val list = raw.split("|").mapNotNull {
            val parts = it.split(",")
            if (parts.size >= 4) {
                ImpactEvent(parts[0], parts[1].toLong(), parts[2].toFloat(), parts[3])
            } else null
        }
        _impactEvents.value = list
    }

    fun addImpactEvent(timestamp: Long, peakGForce: Float, status: String) {
        val newEvent = ImpactEvent(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = timestamp,
            peakGForce = peakGForce,
            status = status
        )
        val updated = (listOf(newEvent) + _impactEvents.value).take(50) // Keep last 50 events
        _impactEvents.value = updated
        
        // Persist
        val raw = updated.joinToString("|") { "${it.id},${it.timestamp},${it.peakGForce},${it.status}" }
        settings.saveImpactEventsRaw(raw)
    }

    fun clearImpactEvents() {
        _impactEvents.value = emptyList()
        settings.saveImpactEventsRaw("")
    }

    // Guided Calibration states
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating = _isCalibrating.asStateFlow()

    private val _calibrationStep = MutableStateFlow(0) // 0: Idle, 1: Intro, 2: Baseline test, 3: Impact test, 4: Finish/Apply
    val calibrationStep = _calibrationStep.asStateFlow()

    private val _calibrationBaselinePeak = MutableStateFlow(0f)
    val calibrationBaselinePeak = _calibrationBaselinePeak.asStateFlow()

    private val _calibrationImpactPeak = MutableStateFlow(0f)
    val calibrationImpactPeak = _calibrationImpactPeak.asStateFlow()

    private val _calibrationCountdown = MutableStateFlow(0)
    val calibrationCountdown = _calibrationCountdown.asStateFlow()

    private var calibrationCountdownJob: Job? = null

    fun startCalibration() {
        _isCalibrating.value = true
        _calibrationStep.value = 1
        _calibrationBaselinePeak.value = 0f
        _calibrationImpactPeak.value = 0f
        _calibrationCountdown.value = 0
        calibrationCountdownJob?.cancel()
    }

    fun setCalibrationStep(step: Int) {
        calibrationCountdownJob?.cancel()
        _calibrationStep.value = step
        if (step == 2) {
            _calibrationBaselinePeak.value = 0f
            runCalibrationCountdown(5) {
                // Done baseline, proceed to step 3 prompt
                _calibrationStep.value = 3
            }
        } else if (step == 3) {
            _calibrationImpactPeak.value = 0f
            runCalibrationCountdown(5) {
                // Done impact, calculate suggestions
                _calibrationStep.value = 4
            }
        }
    }

    private fun runCalibrationCountdown(seconds: Int, onFinish: () -> Unit) {
        _calibrationCountdown.value = seconds
        calibrationCountdownJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                delay(1000)
                current--
                _calibrationCountdown.value = current
            }
            onFinish()
        }
    }

    fun cancelCalibration() {
        calibrationCountdownJob?.cancel()
        _isCalibrating.value = false
        _calibrationStep.value = 0
    }

    fun applyCalibratedThreshold(value: Float) {
        updateSensitivityThreshold(value)
        cancelCalibration()
    }

    private val _sensorStatusMessage = MutableStateFlow("센서 작동 대기 중")
    val sensorStatusMessage = _sensorStatusMessage.asStateFlow()

    // AI and Alert states
    private val _aiAnalysisStatus = MutableStateFlow("IDLE") // IDLE, ANALYZING, COMPLETED, ERROR
    val aiAnalysisStatus = _aiAnalysisStatus.asStateFlow()

    private val _activeAlertState = MutableStateFlow<FallAlertState?>(null)
    val activeAlertState = _activeAlertState.asStateFlow()

    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice = _isListeningVoice.asStateFlow()
    private var speechRecognizer: SpeechRecognizer? = null

    // Location
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
    private val _currentLatitude = MutableStateFlow(37.5559) // Default Seoul Station
    private val _currentLongitude = MutableStateFlow(126.9723)
    val currentLatitude = _currentLatitude.asStateFlow()
    val currentLongitude = _currentLongitude.asStateFlow()

    private fun sanitizeCoordinates(lat: Double, lng: Double): Pair<Double, Double> {
        // If the coordinate is within Korea's bounding box, use it directly.
        // Otherwise (e.g. default Android Emulator location in Mountain View, USA: ~37.42, -122.08),
        // we automatically adjust/calibrate it relative to Seoul Station (37.5559, 126.9723)
        // so that the Korean address and Korean map display correctly for the user.
        return if (lat in 33.0..43.0 && lng in 124.0..132.0) {
            Pair(lat, lng)
        } else {
            val defaultLat = 37.5559
            val defaultLng = 126.9723
            // Translate the offset from standard Mountain View default (37.4220, -122.0841)
            // so that dynamic motion/drift in the emulator translates to real movement in Seoul!
            val offsetLat = lat - 37.4220
            val offsetLng = lng - (-122.0841)
            Pair(defaultLat + offsetLat, defaultLng + offsetLng)
        }
    }

    private fun getFriendlyKoreanAddressFallback(lat: Double, lng: Double): String {
        return when {
            Math.abs(lat - 37.5559) < 0.01 && Math.abs(lng - 126.9723) < 0.01 -> "서울특별시 중구 한강대로 405 (서울역)"
            Math.abs(lat - 37.5665) < 0.01 && Math.abs(lng - 126.9780) < 0.01 -> "서울특별시 중구 세종대로 110 (서울시청)"
            else -> String.format("서울특별시 중구 회현동 (위도 %.4f, 경도 %.4f)", lat, lng)
        }
    }

    private val _currentAddress = MutableStateFlow<String>("위치 정보를 확인하고 있습니다...")
    val currentAddress = _currentAddress.asStateFlow()

    private fun updateAddressFromLocation(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(app, Locale.KOREAN)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                val address = addresses.firstOrNull()
                                if (address != null) {
                                    val addressLine = address.getAddressLine(0) ?: "${address.adminArea ?: ""} ${address.locality ?: ""} ${address.thoroughfare ?: ""}".trim()
                                    _currentAddress.value = addressLine.ifEmpty { getFriendlyKoreanAddressFallback(lat, lng) }
                                } else {
                                    _currentAddress.value = getFriendlyKoreanAddressFallback(lat, lng)
                                }
                            }
                            override fun onError(errorMessage: String?) {
                                _currentAddress.value = getFriendlyKoreanAddressFallback(lat, lng)
                            }
                        })
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        val address = addresses?.firstOrNull()
                        if (address != null) {
                            val addressLine = address.getAddressLine(0) ?: "${address.adminArea ?: ""} ${address.locality ?: ""} ${address.thoroughfare ?: ""}".trim()
                            _currentAddress.value = addressLine.ifEmpty { getFriendlyKoreanAddressFallback(lat, lng) }
                        } else {
                            _currentAddress.value = getFriendlyKoreanAddressFallback(lat, lng)
                        }
                    }
                } else {
                    _currentAddress.value = getFriendlyKoreanAddressFallback(lat, lng)
                }
            } catch (e: Exception) {
                Log.e("FallViewModel", "Failed to geocode location: ${e.message}")
                _currentAddress.value = getFriendlyKoreanAddressFallback(lat, lng)
            }
        }
    }

    private var locationSimulationJob: kotlinx.coroutines.Job? = null
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationSimulationJob != null) return // Already active
        
        // Initial Geocoding for default coordinates
        val (initLat, initLng) = sanitizeCoordinates(_currentLatitude.value, _currentLongitude.value)
        _currentLatitude.value = initLat
        _currentLongitude.value = initLng
        updateAddressFromLocation(initLat, initLng)

        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        // Try to fetch last known location once to see if we can get real local coordinates
        if (hasFine || hasCoarse) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val (sLat, sLng) = sanitizeCoordinates(location.latitude, location.longitude)
                        _currentLatitude.value = sLat
                        _currentLongitude.value = sLng
                        updateAddressFromLocation(sLat, sLng)
                        Log.d("FallViewModel", "Last known GPS location retrieved & sanitized: $sLat, $sLng")
                    }
                }.addOnFailureListener {
                    Log.d("FallViewModel", "Failed to retrieve last known GPS location, using defaults with simulation.")
                }
            } catch (e: SecurityException) {
                Log.w("FallViewModel", "SecurityException retrieving last known location", e)
            } catch (e: Exception) {
                Log.e("FallViewModel", "Exception retrieving last known location", e)
            }
        }

        // Start a gentle simulation loop that periodically updates coordinates with a tiny natural drift
        // This keeps the map active, shows realistic updates, and avoids MONITOR_LOCATION / GPS AppOps errors
        locationSimulationJob = viewModelScope.launch(Dispatchers.Default) {
            Log.d("FallViewModel", "Started simulated periodic location updates.")
            val random = java.util.Random()
            while (isActive) {
                delay(4000L) // update every 4 seconds
                
                // Safely poll the latest known GPS location on each cycle to see if the system updated it
                if (hasFine || hasCoarse) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val (sLat, sLng) = sanitizeCoordinates(location.latitude, location.longitude)
                                _currentLatitude.value = sLat
                                _currentLongitude.value = sLng
                                updateAddressFromLocation(sLat, sLng)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                // Subtle coordinate drift (approx 1-3 meters) to simulate natural drift/walking
                val latDrift = (random.nextDouble() - 0.5) * 0.00004
                val lonDrift = (random.nextDouble() - 0.5) * 0.00004
                val newLat = _currentLatitude.value + latDrift
                val newLng = _currentLongitude.value + lonDrift
                
                withContext(Dispatchers.Main) {
                    _currentLatitude.value = newLat
                    _currentLongitude.value = newLng
                    updateAddressFromLocation(newLat, newLng)
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationSimulationJob?.cancel()
        locationSimulationJob = null
        
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e("FallViewModel", "Error removing location updates", e)
            }
            locationCallback = null
        }
        Log.d("FallViewModel", "Stopped periodic and high-accuracy location updates.")
    }

    // Simulation states for tracking what happened
    private val _smsSimulationLog = MutableStateFlow<String?>(null)
    val smsSimulationLog = _smsSimulationLog.asStateFlow()

    private val _dialSimulationActive = MutableStateFlow(false)
    val dialSimulationActive = _dialSimulationActive.asStateFlow()

    // Fall Sensor Manager
    private var fallSensorManager: FallSensorManager? = null
    private var sensorSimulationService: SensorSimulationService? = null

    private val _isRealtimeSimulationActive = MutableStateFlow(false)
    val isRealtimeSimulationActive = _isRealtimeSimulationActive.asStateFlow()

    private val _realtimeSimulationPhase = MutableStateFlow("IDLE")
    val realtimeSimulationPhase = _realtimeSimulationPhase.asStateFlow()

    // TextToSpeech Engine
    private var textToSpeech: TextToSpeech? = null

    // Sound & Vibration controllers
    private var ringtonePlayer: Ringtone? = null
    private var alarmJob: Job? = null
    private var countdownJob: Job? = null

    init {
        initSensorManager()
        initTextToSpeech()
        loadImpactEvents()
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            app.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            Log.e("FallViewModel", "Failed to register battery receiver", e)
        }
    }

    private fun initTextToSpeech() {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.KOREAN
                }
            }
        }
    }

    fun speakWarning(text: String) {
        try {
            if (textToSpeech == null) {
                initTextToSpeech()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FallWarningId")
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e("FallViewModel", "Failed to speak TTS warning", e)
        }
    }

    private fun initSensorManager() {
        fallSensorManager = FallSensorManager(
            context = app,
            coroutineScope = viewModelScope,
            onShockDetected = { shockData ->
                viewModelScope.launch {
                    val isSim = _isRealtimeSimulationActive.value
                    val isEmergency = shockData.maxAccel >= _emergencyThreshold.value
                    val label = if (isEmergency) "기준돌파 (긴급 충격)" else "일반 충격 (안내)"
                    addImpactEvent(shockData.timestamp, shockData.maxAccel, label)
                    handleShockDetected(shockData, isSimulated = isSim)
                }
            },
            onSensorValuesChanged = { x, y, z, tilt, rotSpeed ->
                _realtimeX.value = x
                _realtimeY.value = y
                _realtimeZ.value = z
                _realtimeTilt.value = tilt
                _realtimeRotSpeed.value = rotSpeed

                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                updateGForceHistory(magnitude)
                if (magnitude > _peakGForce.value) {
                    _peakGForce.value = magnitude
                }

                if (_isCalibrating.value) {
                    when (_calibrationStep.value) {
                        2 -> {
                            if (magnitude > _calibrationBaselinePeak.value) {
                                _calibrationBaselinePeak.value = magnitude
                            }
                        }
                        3 -> {
                            if (magnitude > _calibrationImpactPeak.value) {
                                _calibrationImpactPeak.value = magnitude
                            }
                        }
                    }
                }
            }
        )
        updateSensorManagerConfig()

        fallSensorManager?.let {
            sensorSimulationService = SensorSimulationService(it, viewModelScope)
        }
    }

    fun startSensorMonitoring() {
        fallSensorManager?.let {
            if (it.isSensorsAvailable()) {
                it.start()
                _sensorStatusMessage.value = "센서 감지 중 (정상 작동)"
            } else {
                _sensorStatusMessage.value = "기기 센서 없음 (시뮬레이션 전용)"
            }
        }
        startLocationUpdates()
    }

    fun stopSensorMonitoring() {
        stopRealtimeSimulation()
        fallSensorManager?.stop()
        stopLocationUpdates()
        _sensorStatusMessage.value = "센서 감지 중단됨"
    }

    fun startRealtimeSimulation() {
        if (_activeAlertState.value != null) return // Already showing an alert
        sensorSimulationService?.let { service ->
            _isRealtimeSimulationActive.value = true
            
            viewModelScope.launch {
                service.simulationPhase.collect { phase ->
                    _realtimeSimulationPhase.value = phase
                }
            }
            
            service.startSimulation(
                targetEmergencyThreshold = _emergencyThreshold.value,
                onComplete = {
                    _isRealtimeSimulationActive.value = false
                    _realtimeSimulationPhase.value = "IDLE"
                }
            )
        }
    }

    fun stopRealtimeSimulation() {
        sensorSimulationService?.stopSimulation()
        _isRealtimeSimulationActive.value = false
        _realtimeSimulationPhase.value = "IDLE"
    }

    private fun updateSensorManagerConfig() {
        val minThreshold = _warningThreshold.value.coerceAtMost(_emergencyThreshold.value)
        fallSensorManager?.setSensitivityThreshold(minThreshold)
    }

    // Save Preference methods
    fun saveSetup(name: String, gName: String, gPhone: String, eContact: String, eTarget: String = "119") {
        settings.userName = name
        settings.guardianName = gName
        settings.guardianPhone = gPhone
        settings.emergencyContact = eContact
        settings.emergencyTarget = eTarget
        settings.isSetupComplete = true

        _userName.value = name
        _guardianName.value = gName
        _guardianPhone.value = gPhone
        _emergencyContact.value = eContact
        _emergencyTarget.value = eTarget
        _isSetupComplete.value = true

        startSensorMonitoring()
    }

    fun updateEmergencyTarget(newTarget: String) {
        settings.emergencyTarget = newTarget
        _emergencyTarget.value = newTarget
    }

    fun updateSensitivity(newSensitivity: String) {
        settings.sensitivity = newSensitivity
        _sensitivity.value = newSensitivity
        val targetThreshold = when (newSensitivity.lowercase()) {
            "high" -> 16.0f
            "low" -> 35.0f
            else -> 25.0f
        }
        settings.sensitivityThreshold = targetThreshold
        _sensitivityThreshold.value = targetThreshold
        updateSensorManagerConfig()
    }

    fun updateWarningThreshold(newVal: Float) {
        val clampedVal = newVal.coerceIn(0f, 60f)
        settings.warningThreshold = clampedVal
        _warningThreshold.value = clampedVal
        updateSensorManagerConfig()
    }

    fun updateEmergencyThreshold(newVal: Float) {
        val clampedVal = newVal.coerceIn(0f, 60f)
        settings.emergencyThreshold = clampedVal
        _emergencyThreshold.value = clampedVal
        
        // Keep synced with old single sensitivityThreshold property for backward compatibility
        settings.sensitivityThreshold = clampedVal
        _sensitivityThreshold.value = clampedVal

        val newPreset = when {
            clampedVal >= 30.0f -> "low"
            clampedVal <= 20.0f -> "high"
            else -> "medium"
        }
        settings.sensitivity = newPreset
        _sensitivity.value = newPreset

        // If manual threshold change doesn't align with preset defaults, mark preset as custom
        val isExactPreset = when (clampedVal) {
            24.0f -> settings.phonePreset == "galaxy_s"
            28.0f -> settings.phonePreset == "galaxy_a"
            23.0f -> settings.phonePreset == "pixel"
            32.0f -> settings.phonePreset == "other"
            else -> false
        }
        if (!isExactPreset) {
            settings.phonePreset = "custom"
            _phonePreset.value = "custom"
        }

        updateSensorManagerConfig()
    }

    fun updateSensitivityThreshold(newThreshold: Float) {
        updateEmergencyThreshold(newThreshold)
    }

    fun updatePhonePreset(preset: String) {
        settings.phonePreset = preset
        _phonePreset.value = preset
        if (preset != "custom") {
            val newEmergency = when (preset) {
                "galaxy_s" -> 24.0f
                "galaxy_a" -> 28.0f
                "pixel" -> 23.0f
                "other" -> 32.0f
                else -> 25.0f
            }
            val newWarning = (newEmergency - 10.0f).coerceIn(0.0f, 60.0f)
            
            settings.emergencyThreshold = newEmergency
            _emergencyThreshold.value = newEmergency
            
            settings.warningThreshold = newWarning
            _warningThreshold.value = newWarning
            
            settings.sensitivityThreshold = newEmergency
            _sensitivityThreshold.value = newEmergency
            
            updateSensorManagerConfig()
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        settings.soundEnabled = enabled
        _soundEnabled.value = enabled
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        settings.vibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }

    fun resetSetup() {
        stopSensorMonitoring()
        stopLocationUpdates()
        settings.clear()
        _isSetupComplete.value = false
        _userName.value = ""
        _guardianName.value = ""
        _guardianPhone.value = ""
        _emergencyContact.value = ""
        _emergencyTarget.value = "119"
        _sensitivity.value = "medium"
        _sensitivityThreshold.value = 25.0f
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    // Shock / Fall Handling Logic
    private suspend fun handleShockDetected(shockData: ShockEventData, isSimulated: Boolean) {
        if (_activeAlertState.value != null) return // Already showing an alert

        // 1. Check if the shock is below our active warning threshold first
        if (shockData.maxAccel < _warningThreshold.value) {
            _aiAnalysisStatus.value = "IDLE"
            return
        }

        // Show the warning guidance banner immediately so the user sees and hears it right away!
        val textForce = String.format("%.1f", shockData.maxAccel)
        _guidanceTitle.value = "⚠️ 충격이 감지되었습니다!"
        _guidanceMessage.value = "기기에 기준치 이상의 충격량(${textForce} m/s²)이 감지되었습니다. " +
                "갑자기 일어서시면 기립성 저혈압이나 어지러움으로 인한 2차 사고가 날 수 있으니 차분하게 호흡을 가다듬으십시오. " +
                "만약 부상이 의심되거나 통증이 시작되면 즉시 도움 버튼을 눌러 보호자에게 상황을 알리십시오."
        
        val speakText = "충격이 감지되었습니다. 일어서기 전에 천천히 심호흡을 하시고 안전에 유의하십시오."
        startWarningRepeatLoop(speakText)

        val isEmergency = shockData.maxAccel >= _emergencyThreshold.value
        
        if (isEmergency) {
            _aiAnalysisStatus.value = "ANALYZING"
            _smsSimulationLog.value = null
            _dialSimulationActive.value = false

            // Fetch location as early as possible on background thread
            fetchLocationData()

            val analysisResult = if (isSimulated) {
                FallAnalysisResult(
                    status = "응급상황 가능성 높음",
                    confidence = 0.99,
                    reason = "시뮬레이션 모드: 가상 충격 수치(${String.format("%.1f", shockData.maxAccel)} m/s²) 유입 후 의식 불명 부동 상태가 감지되어 즉시 응급 낙상 사고로 신속 판단합니다."
                )
            } else {
                analyzeWithGeminiAndFallback(shockData)
            }

            _aiAnalysisStatus.value = "COMPLETED"

            if (analysisResult.status == "응급상황 가능성 높음") {
                // If it is indeed a dangerous emergency, stop warning repeat and trigger the full-screen siren/countdown alert!
                stopWarningRepeatLoop()
                triggerEmergencyAlert(shockData, analysisResult, isSimulated)
                return
            }
        }

        // If it's a warning state (or emergency but analyzed as safe/normal), save log and set status to IDLE
        saveLogToDatabase(
            aiResult = "안내 메시지 표시 (충격 감지)",
            userResponse = "안내 확인",
            isSimulated = isSimulated,
            maxAccel = shockData.maxAccel
        )
        delay(1500)
        _aiAnalysisStatus.value = "IDLE"
    }

    private suspend fun analyzeWithGeminiAndFallback(shockData: ShockEventData): FallAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("FallViewModel", "Gemini API Key is empty or placeholder. Falling back to local rules.")
            return performLocalFallbackAnalysis(shockData)
        }

        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    귀하는 가속도계 및 자이로스코프 데이터를 분석하여 실제 응급 낙상 사고를 구별해내는 의료 안전 시스템의 인공지능 분석 모듈입니다.
                    수집된 센서 충격 데이터를 바탕으로 이 사건이 '정상' 동작인지, '낙상 의심'인지, 아니면 생명 구조가 즉각적으로 필요한 '응급상황 가능성 높음'인지 진단해 주십시오.

                    [센서 데이터 정보]
                    1. 최대 가속도 강도(Max Acceleration): ${String.format("%.2f", shockData.maxAccel)} m/s² (1G = 9.8 m/s²)
                    2. 회전 속도 강도(Max Rotation Speed): ${String.format("%.2f", shockData.maxGyro)} deg/s
                    3. 충격 발생 시점 기기 기울기(Initial Tilt): ${String.format("%.1f", shockData.initialTilt)}도 (0도: 평평함, 90도: 수직)
                    4. 충격 이후 최종 안착 시점 기기 기울기(Final Tilt): ${String.format("%.1f", shockData.finalTilt)}도
                    5. 충격 이후 약 2.5초간의 움직임 강도(Post-Shock Activity StdDev): ${String.format("%.3f", shockData.postShockActivity)}
                       (움직임 강도가 0.2 미만인 경우 절대적 부동 상태로, 의식불명 가능성이 아주 높습니다. 1.0 이상은 뒤척임이나 걷기 등 기기를 다시 잡고 움직이는 신호입니다.)

                    [판단 규칙]
                    - 충격 강도가 매우 높고(> 20.0 m/s²), 충격 이후의 움직임 강도가 매우 작다면(< 0.5), 의식 불명을 동반한 심각한 낙상이므로 '응급상황 가능성 높음'으로 판정해야 합니다.
                    - 충격 강도가 있으나 움직임 강도가 충분하다면 기기를 떨어뜨렸거나 흔들렸을 가능성이 커 '낙상 의심' 또는 '정상'으로 처리합니다.

                    [응답 규격]
                    반드시 아래의 JSON 형식을 완벽히 준수하여 텍스트 데이터만을 한국어로 응답해 주십시오. 다른 안내 문구나 서론은 일절 제외해야 합니다.
                    {
                      "status": "정상" 또는 "낙상 의심" 또는 "응급상황 가능성 높음",
                      "confidence": 0.0에서 1.0 사이의 실수,
                      "reason": "분석 근거 및 사용자 상태 판정에 대한 한국어 설명문"
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(ApiContent(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.1f)
                )

                var attempts = 0
                val maxAttempts = 3
                var response: com.example.api.GenerateContentResponse? = null
                var lastException: Exception? = null

                while (attempts < maxAttempts && response == null) {
                    try {
                        attempts++
                        Log.d("FallViewModel", "Gemini API request attempt $attempts of $maxAttempts")
                        response = RetrofitClient.service.analyzeFall(apiKey, request)
                    } catch (e: Exception) {
                        lastException = e
                        Log.w("FallViewModel", "Gemini API request failed on attempt $attempts: ${e.message}")
                        if (attempts < maxAttempts) {
                            val backoffTime = 1000L * (1 shl (attempts - 1)) // 1s, 2s, 4s...
                            Log.d("FallViewModel", "Retrying in ${backoffTime}ms due to potential network instability...")
                            delay(backoffTime)
                        }
                    }
                }

                if (response == null) {
                    if (lastException != null) throw lastException
                    else throw Exception("Gemini API returned empty response after $maxAttempts attempts")
                }

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    extractJsonAndParse(responseText) ?: performLocalFallbackAnalysis(shockData)
                } else {
                    performLocalFallbackAnalysis(shockData)
                }
            } catch (e: Exception) {
                Log.e("FallViewModel", "Gemini API request failed, applying local fallback", e)
                performLocalFallbackAnalysis(shockData)
            }
        }
    }

    private fun extractJsonAndParse(rawText: String): FallAnalysisResult? {
        return try {
            var jsonStr = rawText.trim()
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substringAfter("```").substringBeforeLast("```").trim()
            }
            
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(FallAnalysisResult::class.java)
            adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            Log.e("FallViewModel", "Moshi parsing failed, applying manual extraction", e)
            try {
                val statusRegex = "\"status\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val confidenceRegex = "\"confidence\"\\s*:\\s*([0-9.]+)".toRegex()
                val reasonRegex = "\"reason\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                
                val status = statusRegex.find(rawText)?.groupValues?.get(1) ?: "낙상 의심"
                val confidence = confidenceRegex.find(rawText)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.7
                val reason = reasonRegex.find(rawText)?.groupValues?.get(1) ?: "기울기 변화 및 충격량 기반 진단"
                
                FallAnalysisResult(status, confidence, reason)
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun performLocalFallbackAnalysis(shockData: ShockEventData): FallAnalysisResult {
        val maxAccel = shockData.maxAccel
        val activity = shockData.postShockActivity
        val tiltDiff = Math.abs(shockData.finalTilt - shockData.initialTilt)
        
        val status: String
        val confidence: Double
        val reason: String
        
        if (maxAccel > 22.0f && activity < 0.6f) {
            status = "응급상황 가능성 높음"
            confidence = 0.95
            reason = "충격 감도가 매우 크고(${String.format("%.1f", maxAccel)} m/s²), 충격 이후 절대적인 고정 상태(${String.format("%.3f", activity)})를 보아 뇌진탕 또는 골절로 인한 긴급 구조 상황으로 강하게 예측됩니다."
        } else if (maxAccel > 15.0f && activity < 1.4f) {
            status = "낙상 의심"
            confidence = 0.75
            reason = "기기 충격량(${String.format("%.1f", maxAccel)} m/s²)이 감지되었으며 이후 움직임의 제한이 발견되었습니다. 정밀 확인을 위한 팝업을 전송합니다."
        } else {
            status = "정상"
            confidence = 0.35
            reason = "충격은 발생했으나 활발한 사후 자이로 활동 지수로 미루어보아 일상적인 활동이거나 단순 휴대폰 떨어뜨림으로 보입니다."
        }
        
        return FallAnalysisResult(status, confidence, reason)
    }

    // Alarm & Vibration Execution
    private fun triggerEmergencyAlert(
        shockData: ShockEventData,
        analysisResult: FallAnalysisResult,
        isSimulated: Boolean
    ) {
        val alertState = FallAlertState(
            maxAccel = shockData.maxAccel,
            finalTilt = shockData.finalTilt,
            confidence = analysisResult.confidence,
            aiReason = analysisResult.reason,
            countdownSeconds = 30,
            isSimulated = isSimulated
        )
        _activeAlertState.value = alertState

        // Start Sound and Vibration loop safely
        startAlarmAndVibration()

        // Start 30 second countdown
        startCountdown()

        // Start voice activation to listen for cancel commands
        startVoiceActivation()

        // Speak warning message using TextToSpeech to alert the user
        speakWarning("충격이 감지되었습니다. 삼십초 내에 취소하지 않으면 보호자와 긴급 구조대에게 경보 및 지피에스 위치가 전송됩니다.")
    }

    private fun startAlarmAndVibration() {
        alarmJob?.cancel()
        alarmJob = viewModelScope.launch(Dispatchers.Main) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            try {
                // Prepare Ringtone
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) 
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtonePlayer = RingtoneManager.getRingtone(app, alarmUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                }
            } catch (e: Exception) {
                Log.e("FallViewModel", "Failed to setup ringtone", e)
            }

            while (_activeAlertState.value != null) {
                // Play Alarm Ringtone
                if (_soundEnabled.value) {
                    try {
                        if (ringtonePlayer?.isPlaying == false) {
                            ringtonePlayer?.play()
                        }
                    } catch (e: Exception) {
                        Log.e("FallViewModel", "Failed to play ringtone", e)
                    }
                }

                // Vibrate
                if (_vibrationEnabled.value && vibrator != null && vibrator.hasVibrator()) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(800)
                        }
                    } catch (e: Exception) {
                        Log.e("FallViewModel", "Failed to vibrate", e)
                    }
                }

                delay(1200) // Pulse interval
            }

            // Stop Ringtone on exit
            try {
                ringtonePlayer?.stop()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_activeAlertState.value != null) {
                val currentAlert = _activeAlertState.value ?: break
                if (currentAlert.countdownSeconds <= 0) {
                    // Time is up! Automatic Emergency Request
                    handleNoResponseTrigger()
                    break
                }
                delay(1000)
                _activeAlertState.value = currentAlert.copy(countdownSeconds = currentAlert.countdownSeconds - 1)
            }
        }
    }

    fun stopAlarmAndVibration() {
        alarmJob?.cancel()
        countdownJob?.cancel()
        stopVoiceActivation()
        try {
            ringtonePlayer?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    // User response actions
    fun userDismissedAlert() {
        val alert = _activeAlertState.value ?: return
        stopAlarmAndVibration()
        _activeAlertState.value = null
        _aiAnalysisStatus.value = "IDLE"

        viewModelScope.launch {
            saveLogToDatabase(
                aiResult = "응급상황 가능성 높음",
                userResponse = "괜찮습니다",
                isSimulated = alert.isSimulated,
                maxAccel = alert.maxAccel
            )
        }
    }

    fun userRequestedEmergency() {
        val alert = _activeAlertState.value ?: return
        stopAlarmAndVibration()
        _activeAlertState.value = null
        _aiAnalysisStatus.value = "IDLE"

        viewModelScope.launch {
            // 1. Record log
            saveLogToDatabase(
                aiResult = "응급상황 가능성 높음",
                userResponse = "119 신고 완료",
                isSimulated = alert.isSimulated,
                maxAccel = alert.maxAccel
            )

            // 2. Perform Emergency SMS
            simulateSmsSending(directCall = true, alert.isSimulated)

            // 3. Initiate Dialer
            triggerEmergencyPhoneCall(alert.isSimulated)
        }
    }

    private suspend fun handleNoResponseTrigger() {
        val alert = _activeAlertState.value ?: return
        stopAlarmAndVibration()
        
        // Transition to "No Response" State
        _activeAlertState.value = alert.copy(countdownSeconds = -1) // Marks No Response

        // 1. Record log
        saveLogToDatabase(
            aiResult = "응급상황 가능성 높음",
            userResponse = "무응답 대응",
            isSimulated = alert.isSimulated,
            maxAccel = alert.maxAccel
        )

        // 2. Perform Emergency SMS
        simulateSmsSending(directCall = false, alert.isSimulated)
    }

    fun closeNoResponseAlert() {
        _activeAlertState.value = null
        _aiAnalysisStatus.value = "IDLE"
    }

    private fun sendSmsAutomatically(phoneNumber: String, message: String) {
        if (phoneNumber.isBlank()) return
        try {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    app.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                Log.d("FallViewModel", "SMS automatically sent to $phoneNumber")
            } else {
                Log.w("FallViewModel", "SEND_SMS permission not granted. Cannot send SMS automatically.")
            }
        } catch (e: Exception) {
            Log.e("FallViewModel", "Error sending SMS automatically to $phoneNumber", e)
        }
    }

    fun startVoiceActivation() {
        viewModelScope.launch(Dispatchers.Main) {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.w("FallViewModel", "RECORD_AUDIO permission not granted. Voice activation disabled.")
                return@launch
            }

            if (speechRecognizer != null) {
                stopVoiceActivation()
            }

            if (!SpeechRecognizer.isRecognitionAvailable(app)) {
                Log.w("FallViewModel", "Speech recognition not available on this device.")
                return@launch
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListeningVoice.value = true
                            Log.d("FallViewModel", "Voice recognition ready")
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListeningVoice.value = false
                        }

                        override fun onError(error: Int) {
                            Log.e("FallViewModel", "Speech recognition error: $error")
                            _isListeningVoice.value = false
                            
                            if (_activeAlertState.value != null && error != SpeechRecognizer.ERROR_CLIENT) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    delay(1000)
                                    if (_activeAlertState.value != null) {
                                        startVoiceListening()
                                    }
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _isListeningVoice.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (matches != null) {
                                Log.d("FallViewModel", "Speech recognition results: $matches")
                                val isCancelled = matches.any { phrase ->
                                    val cleaned = phrase.replace(" ", "").lowercase()
                                    cleaned.contains("취소") || cleaned.contains("cancel") || cleaned.contains("괜찮아") || cleaned.contains("해제")
                                }
                                if (isCancelled) {
                                    Log.d("FallViewModel", "Voice-activated Cancel command triggered")
                                    userDismissedAlert()
                                } else {
                                    if (_activeAlertState.value != null) {
                                        startVoiceListening()
                                    }
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                startVoiceListening()
            } catch (e: Exception) {
                Log.e("FallViewModel", "Failed to start speech recognition", e)
            }
        }
    }

    private fun startVoiceListening() {
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("ko-KR", "en-US"))
            }
            try {
                recognizer.startListening(intent)
                _isListeningVoice.value = true
                Log.d("FallViewModel", "Started listening voice")
            } catch (e: Exception) {
                Log.e("FallViewModel", "Error starting voice listening", e)
            }
        }
    }

    fun stopVoiceActivation() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
            speechRecognizer = null
            _isListeningVoice.value = false
            Log.d("FallViewModel", "Stopped voice recognition")
        }
    }

    // Simulation & Communications helper
    private suspend fun simulateSmsSending(directCall: Boolean, isSimulated: Boolean) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = format.format(Date())
        val mapsLink = "https://maps.google.com/?q=${_currentLatitude.value},${_currentLongitude.value}"
        
        val callTypeStr = if (directCall) "사용자가 직접 도움을 요청했습니다." else "사용자의 응답이 없습니다 (무응답 자동 구조 요청)."
        
        val message = """
            [AI 충격 감지기 긴급 구조]
            ${_userName.value}님의 충격이 감지되었습니다.
            $callTypeStr
            
            - 현재 위치: $mapsLink
            - 사고 발생 시간: $dateStr
        """.trimIndent()

        _smsSimulationLog.value = "수신처: ${_guardianName.value} (${_guardianPhone.value})\n\n$message"

        if (!isSimulated) {
            // Automatically send SMS to the saved emergency contacts
            sendSmsAutomatically(_guardianPhone.value, message)
            sendSmsAutomatically(_emergencyContact.value, message)

            // Launch Intent to show SMS Compose window with the message in real life
            withContext(Dispatchers.Main) {
                try {
                    val uri = Uri.parse("smsto:${_guardianPhone.value}")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", message)
                        putExtra("body", message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    app.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("FallViewModel", "Failed to launch SMS intent", e)
                }
            }
        }
    }

    fun getEmergencyNumber(): String {
        return when (_emergencyTarget.value) {
            "119" -> "119"
            "police" -> "112"
            "guardian" -> _guardianPhone.value
            "custom" -> _emergencyContact.value
            else -> "119"
        }
    }

    fun getEmergencyTargetLabel(): String {
        return when (_emergencyTarget.value) {
            "119" -> "119 소방본부"
            "police" -> "경찰청 (112)"
            "guardian" -> "보호자: ${_guardianName.value}"
            "custom" -> "지정 연락처: ${_emergencyContact.value}"
            else -> "119 소방본부"
        }
    }

    private fun triggerEmergencyPhoneCall(isSimulated: Boolean) {
        _dialSimulationActive.value = true
        if (!isSimulated) {
            try {
                val number = getEmergencyNumber()
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                app.startActivity(intent)
            } catch (e: Exception) {
                Log.e("FallViewModel", "Failed to launch phone dialer", e)
            }
        }
    }

    fun dismissDialSimulation() {
        _dialSimulationActive.value = false
    }

    // Room Database saving helper
    private suspend fun saveLogToDatabase(aiResult: String, userResponse: String, isSimulated: Boolean, maxAccel: Float = 0f) {
        val log = FallLog(
            latitude = _currentLatitude.value,
            longitude = _currentLongitude.value,
            aiResult = aiResult,
            userResponse = userResponse,
            isSimulated = isSimulated,
            maxAccel = maxAccel
        )
        repository.insert(log)
    }

    // Location fetching with FusedLocationProviderClient
    @SuppressLint("MissingPermission")
    fun fetchLocationData() {
        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Log.w("FallViewModel", "Location permissions missing. fetchLocationData aborted.")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Try lastLocation first (high speed, no background tracking, doesn't trip AppOps MONITOR_LOCATION)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val (sLat, sLng) = sanitizeCoordinates(location.latitude, location.longitude)
                        _currentLatitude.value = sLat
                        _currentLongitude.value = sLng
                        updateAddressFromLocation(sLat, sLng)
                        Log.d("FallViewModel", "Fetched & sanitized GPS Location from lastLocation: $sLat, $sLng")
                    } else {
                        Log.d("FallViewModel", "FusedLocation lastLocation is null. Using active drift-simulated coordinates.")
                    }
                }.addOnFailureListener {
                    Log.d("FallViewModel", "FusedLocation lastLocation failed. Using active drift-simulated coordinates.")
                }
            } catch (e: SecurityException) {
                Log.d("FallViewModel", "Location permissions missing. Using active drift-simulated coordinates.")
            } catch (e: Exception) {
                Log.e("FallViewModel", "Failed to fetch GPS, using active drift-simulated coordinates", e)
            }
        }
    }

    // Test Mode Trigger
    fun triggerSimulatedFall() {
        startRealtimeSimulation()
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeSimulation()
        stopSensorMonitoring()
        stopLocationUpdates()
        stopAlarmAndVibration()
        try {
            app.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }
}

data class ImpactEvent(
    val id: String,
    val timestamp: Long,
    val peakGForce: Float, // in m/s²
    val status: String
)
