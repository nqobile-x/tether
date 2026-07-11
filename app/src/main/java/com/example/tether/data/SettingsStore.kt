package com.example.tether.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoredSettings(
    val debounceSeconds: Int = 15,
    val backgroundMonitoringEnabled: Boolean = true
)

/**
 * SharedPreferences-backed settings with a StateFlow view.
 * The debounce value is the high-priority (abrupt disconnect) window.
 * The low-priority window for gradual signal loss is derived as three times this value,
 * matching the 15s/45s defaults in the architecture doc.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tether_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        StoredSettings(
            debounceSeconds = prefs.getInt(KEY_DEBOUNCE, 15),
            backgroundMonitoringEnabled = prefs.getBoolean(KEY_BG_MONITORING, true)
        )
    )
    val settings: StateFlow<StoredSettings> = _settings.asStateFlow()

    fun updateDebounce(seconds: Int) {
        val clamped = seconds.coerceIn(5, 60)
        prefs.edit().putInt(KEY_DEBOUNCE, clamped).apply()
        _settings.value = _settings.value.copy(debounceSeconds = clamped)
    }

    fun setBackgroundMonitoring(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_MONITORING, enabled).apply()
        _settings.value = _settings.value.copy(backgroundMonitoringEnabled = enabled)
    }

    fun highPriorityWindowMs(): Long = _settings.value.debounceSeconds * 1000L
    fun lowPriorityWindowMs(): Long = _settings.value.debounceSeconds * 3000L

    private companion object {
        const val KEY_DEBOUNCE = "debounce_seconds"
        const val KEY_BG_MONITORING = "background_monitoring"
    }
}
