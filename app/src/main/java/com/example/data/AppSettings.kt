package com.example.data

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_GUARDIAN_NAME = "guardian_name"
        private const val KEY_GUARDIAN_PHONE = "guardian_phone"
        private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
        private const val KEY_SENSITIVITY = "sensitivity" // "low", "medium", "high"
        private const val KEY_SENSITIVITY_THRESHOLD = "sensitivity_threshold"
        private const val KEY_WARNING_THRESHOLD = "warning_threshold"
        private const val KEY_EMERGENCY_THRESHOLD = "emergency_threshold"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_EMERGENCY_TARGET = "emergency_target" // "119", "police", "guardian", "custom"
        private const val KEY_PHONE_PRESET = "phone_preset" // "galaxy_s", "galaxy_a", "pixel", "other", "custom"
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var phonePreset: String
        get() = prefs.getString(KEY_PHONE_PRESET, "galaxy_s") ?: "galaxy_s"
        set(value) = prefs.edit().putString(KEY_PHONE_PRESET, value).apply()

    var guardianName: String
        get() = prefs.getString(KEY_GUARDIAN_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GUARDIAN_NAME, value).apply()

    var guardianPhone: String
        get() = prefs.getString(KEY_GUARDIAN_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GUARDIAN_PHONE, value).apply()

    var emergencyContact: String
        get() = prefs.getString(KEY_EMERGENCY_CONTACT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT, value).apply()

    var emergencyTarget: String
        get() = prefs.getString(KEY_EMERGENCY_TARGET, "119") ?: "119"
        set(value) = prefs.edit().putString(KEY_EMERGENCY_TARGET, value).apply()

    var sensitivity: String
        get() = prefs.getString(KEY_SENSITIVITY, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_SENSITIVITY, value).apply()
        
    var sensitivityThreshold: Float
        get() = prefs.getFloat(KEY_SENSITIVITY_THRESHOLD, 25.0f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY_THRESHOLD, value).apply()

    var warningThreshold: Float
        get() = prefs.getFloat(KEY_WARNING_THRESHOLD, 15.0f)
        set(value) = prefs.edit().putFloat(KEY_WARNING_THRESHOLD, value).apply()

    var emergencyThreshold: Float
        get() = prefs.getFloat(KEY_EMERGENCY_THRESHOLD, 25.0f)
        set(value) = prefs.edit().putFloat(KEY_EMERGENCY_THRESHOLD, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    fun getImpactEventsRaw(): String {
        return prefs.getString("impact_events_raw", "") ?: ""
    }

    fun saveImpactEventsRaw(raw: String) {
        prefs.edit().putString("impact_events_raw", raw).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
