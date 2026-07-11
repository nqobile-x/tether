package com.example.tether.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the GATT link to the paired device and polls RSSI while connected.
 * Emits connection events for the disconnect monitor to classify.
 */
class GattConnectionManager(
    private val context: Context,
    private val address: String,
    private val scope: CoroutineScope
) {
    sealed interface Event {
        data object Connected : Event
        data object Disconnected : Event
        data class Rssi(val value: Int) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 32)
    val events: SharedFlow<Event> = _events

    private var gatt: BluetoothGatt? = null
    private var rssiJob: Job? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _events.tryEmit(Event.Connected)
                    startRssiPolling()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    stopRssiPolling()
                    _events.tryEmit(Event.Disconnected)
                    closeQuietly()
                }
            }
        }

        override fun onReadRemoteRssi(g: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(Event.Rssi(rssi))
            }
        }
    }

    fun connect() {
        if (gatt != null) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        try {
            val device = adapter.getRemoteDevice(address)
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            Log.w(TAG, "connectGatt blocked, missing BLUETOOTH_CONNECT", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid device address: $address", e)
        }
    }

    fun disconnect() {
        stopRssiPolling()
        try {
            gatt?.disconnect()
        } catch (e: SecurityException) {
            Log.w(TAG, "disconnect blocked", e)
        }
        closeQuietly()
    }

    private fun startRssiPolling() {
        rssiJob?.cancel()
        rssiJob = scope.launch {
            while (isActive) {
                try {
                    gatt?.readRemoteRssi()
                } catch (e: SecurityException) {
                    Log.w(TAG, "readRemoteRssi blocked", e)
                    break
                }
                delay(RSSI_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopRssiPolling() {
        rssiJob?.cancel()
        rssiJob = null
    }

    private fun closeQuietly() {
        try {
            gatt?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "close blocked", e)
        }
        gatt = null
    }

    private companion object {
        const val TAG = "GattConnectionManager"
        const val RSSI_POLL_INTERVAL_MS = 2_000L
    }
}
