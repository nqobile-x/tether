package com.example.tether.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int
)

/**
 * One-shot BLE scan. Collects named devices seen during the scan window.
 * Returns an empty list when Bluetooth is off or permissions are missing,
 * the pairing screen shows its empty state in that case instead of crashing.
 */
class BleScanner(private val context: Context) {

    suspend fun scan(durationMs: Long = 8_000): List<DiscoveredDevice> {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val scanner = adapter?.bluetoothLeScanner ?: return emptyList()
        if (adapter.isEnabled.not()) return emptyList()

        val found = LinkedHashMap<String, DiscoveredDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = try {
                    result.device.name
                } catch (e: SecurityException) {
                    null
                } ?: result.scanRecord?.deviceName ?: return
                found[result.device.address] =
                    DiscoveredDevice(result.device.address, name, result.rssi)
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        return try {
            scanner.startScan(null, settings, callback)
            delay(durationMs)
            scanner.stopScan(callback)
            found.values.toList()
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE scan blocked, missing permission", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "BleScanner"
    }
}
