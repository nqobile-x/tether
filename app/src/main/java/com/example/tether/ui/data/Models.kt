package com.example.tether.ui.data

data class PairedDevice(
    val id: String,
    val name: String,
    val isConnected: Boolean,
    val lastSyncTime: String,
    val batteryLevel: Int = 88
)

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false
)

data class AlertEvent(
    val id: String,
    val timestamp: String,
    val classification: String,
    val isFalsePositive: Boolean,
    val locationCoordinates: String,
    val notifiedContacts: List<String>
)

data class AppSettings(
    val debounceSeconds: Int = 15,
    val backgroundMonitoringEnabled: Boolean = true,
    val bluetoothPermissionGranted: Boolean = true,
    val locationPermissionGranted: Boolean = true,
    val smsPermissionGranted: Boolean = false
)