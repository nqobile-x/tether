package com.example.tether.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.tether.R
import com.example.tether.alerting.AlertPipeline
import com.example.tether.ble.BleScanner
import com.example.tether.data.db.PairedDeviceEntity
import com.example.tether.data.db.TetherDatabase
import com.example.tether.data.db.TrustedContactEntity
import com.example.tether.service.TetherMonitorService
import com.example.tether.ui.data.AlertEvent
import com.example.tether.ui.data.AppSettings
import com.example.tether.ui.data.Contact
import com.example.tether.ui.data.PairedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The real data layer behind the UI. Exposes the exact same surface the
 * FakeTetherRepository did, so the ViewModels only needed an import change.
 * Backed by Room, SharedPreferences, real BLE scanning, and the alert pipeline.
 */
object TetherRepository {

    private lateinit var appContext: Context
    private lateinit var db: TetherDatabase
    private lateinit var bleScanner: BleScanner
    private lateinit var alertPipeline: AlertPipeline
    lateinit var settingsStore: SettingsStore
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    private val _lastSyncAt = MutableStateFlow<Long?>(null)
    private val _permissions = MutableStateFlow(PermissionState())
    private val _availableDevices = MutableStateFlow<List<PairedDevice>>(emptyList())

    lateinit var pairedDevice: StateFlow<PairedDevice?>
        private set
    lateinit var contacts: StateFlow<List<Contact>>
        private set
    lateinit var alerts: StateFlow<List<AlertEvent>>
        private set
    lateinit var settings: StateFlow<AppSettings>
        private set
    val availableDevices: StateFlow<List<PairedDevice>> = _availableDevices.asStateFlow()

    private data class PermissionState(
        val bluetooth: Boolean = false,
        val location: Boolean = false,
        val sms: Boolean = false
    )

    fun initialize(context: Context) {
        appContext = context.applicationContext
        db = TetherDatabase.get(appContext)
        settingsStore = SettingsStore(appContext)
        bleScanner = BleScanner(appContext)
        alertPipeline = AlertPipeline(appContext, db)

        pairedDevice = combine(
            db.pairedDeviceDao().observe(), _isConnected, _lastSyncAt
        ) { entity, connected, syncAt ->
            entity?.let {
                PairedDevice(
                    id = it.deviceAddress,
                    name = it.deviceName,
                    isConnected = connected,
                    lastSyncTime = formatTime(syncAt ?: it.pairedAt)
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)

        contacts = db.trustedContactDao().observeAll().map { list ->
            list.map { Contact(it.id.toString(), it.name, it.phoneNumber, it.isPrimary) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

        alerts = db.alertEventDao().observeAll().map { list ->
            list.map { entity ->
                AlertEvent(
                    id = entity.id.toString(),
                    timestamp = formatTime(entity.timestamp),
                    classification = classificationLabel(entity.classification),
                    isFalsePositive = entity.wasFalsePositive,
                    locationCoordinates = formatCoordinates(entity.latitude, entity.longitude),
                    notifiedContacts = entity.contactsNotified
                        .split(",").filter { it.isNotBlank() }
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

        settings = combine(settingsStore.settings, _permissions) { stored, perms ->
            AppSettings(
                debounceSeconds = stored.debounceSeconds,
                backgroundMonitoringEnabled = stored.backgroundMonitoringEnabled,
                bluetoothPermissionGranted = perms.bluetooth,
                locationPermissionGranted = perms.location,
                smsPermissionGranted = perms.sms
            )
        }.stateIn(scope, SharingStarted.Eagerly, AppSettings())

        refreshPermissions()
    }

    suspend fun scanForDevices() {
        val pairedAddress = db.pairedDeviceDao().getOnce()?.deviceAddress
        _availableDevices.value = emptyList()
        val found = bleScanner.scan()
        _availableDevices.value = found.map { device ->
            PairedDevice(
                id = device.address,
                name = device.name,
                isConnected = device.address == pairedAddress,
                lastSyncTime = appContext.getString(R.string.sync_never)
            )
        }
    }

    fun pairWithDevice(device: PairedDevice) {
        scope.launch {
            db.pairedDeviceDao().clear()
            db.pairedDeviceDao().insert(
                PairedDeviceEntity(
                    deviceAddress = device.id,
                    deviceName = device.name,
                    pairedAt = System.currentTimeMillis(),
                    lastKnownRssi = 0
                )
            )
            _availableDevices.value = _availableDevices.value.map {
                it.copy(isConnected = it.id == device.id)
            }
            if (settingsStore.settings.value.backgroundMonitoringEnabled) {
                TetherMonitorService.stop(appContext)
                TetherMonitorService.start(appContext)
            }
        }
    }

    fun addContact(name: String, phone: String) {
        scope.launch {
            val isFirst = db.trustedContactDao().getAllOnce().isEmpty()
            db.trustedContactDao().insert(
                TrustedContactEntity(name = name, phoneNumber = phone, isPrimary = isFirst)
            )
        }
    }

    fun deleteContact(id: String) {
        val contactId = id.toLongOrNull() ?: return
        scope.launch {
            val dao = db.trustedContactDao()
            dao.delete(contactId)
            if (dao.primaryCount() == 0) {
                dao.firstId()?.let { dao.setPrimary(it) }
            }
        }
    }

    fun setPrimaryContact(id: String) {
        val contactId = id.toLongOrNull() ?: return
        scope.launch { db.trustedContactDao().setPrimary(contactId) }
    }

    fun toggleFalsePositive(alertId: String) {
        val id = alertId.toLongOrNull() ?: return
        scope.launch { db.alertEventDao().toggleFalsePositive(id) }
    }

    fun updateDebounce(seconds: Int) {
        settingsStore.updateDebounce(seconds)
    }

    fun toggleBackgroundMonitoring(enabled: Boolean) {
        settingsStore.setBackgroundMonitoring(enabled)
        if (enabled) {
            scope.launch {
                if (db.pairedDeviceDao().getOnce() != null) {
                    TetherMonitorService.start(appContext)
                }
            }
        } else {
            TetherMonitorService.stop(appContext)
        }
    }

    fun sendTestAlert() {
        scope.launch { alertPipeline.trigger(AlertPipeline.Classification.TEST) }
    }

    // Called by the foreground service.

    fun onConnectionChanged(connected: Boolean) {
        _isConnected.value = connected
        if (connected) _lastSyncAt.value = System.currentTimeMillis()
    }

    fun onRssiUpdate(rssi: Int) {
        _lastSyncAt.value = System.currentTimeMillis()
        scope.launch {
            db.pairedDeviceDao().getOnce()?.let {
                db.pairedDeviceDao().updateRssi(it.deviceAddress, rssi)
            }
        }
    }

    fun refreshPermissions() {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(appContext, permission) ==
                PackageManager.PERMISSION_GRANTED

        val bluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(Manifest.permission.BLUETOOTH_SCAN) &&
                granted(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
        _permissions.value = PermissionState(
            bluetooth = bluetooth,
            location = granted(Manifest.permission.ACCESS_FINE_LOCATION),
            sms = granted(Manifest.permission.SEND_SMS)
        )
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))

    private fun formatCoordinates(lat: Double?, lng: Double?): String =
        if (lat != null && lng != null) {
            String.format(Locale.US, "%.5f, %.5f", lat, lng)
        } else {
            appContext.getString(R.string.location_unavailable)
        }

    private fun classificationLabel(code: String): String = when (code) {
        AlertPipeline.Classification.ABRUPT -> appContext.getString(R.string.class_abrupt)
        AlertPipeline.Classification.GRADUAL -> appContext.getString(R.string.class_gradual)
        AlertPipeline.Classification.TEST -> appContext.getString(R.string.class_test)
        else -> code
    }
}
