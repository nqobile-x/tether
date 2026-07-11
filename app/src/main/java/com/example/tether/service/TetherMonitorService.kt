package com.example.tether.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.tether.R
import com.example.tether.TetherApplication
import com.example.tether.alerting.AlertPipeline
import com.example.tether.alerting.DisconnectClassifier
import com.example.tether.alerting.DisconnectMonitor
import com.example.tether.ble.GattConnectionManager
import com.example.tether.data.TetherRepository
import com.example.tether.data.db.TetherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The foreground service tying BLE and alerting together. Holds the GATT link,
 * feeds RSSI and connection events into the debounce monitor, and hands confirmed
 * disconnects to the alert pipeline.
 */
class TetherMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gattManager: GattConnectionManager? = null
    private var monitor: DisconnectMonitor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_CONNECT not granted, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        startMonitoring()
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this, TetherApplication.MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(getString(R.string.notif_monitor_title))
            .setContentText(getString(R.string.notif_monitor_body))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        )
    }

    private fun startMonitoring() {
        if (gattManager != null) return
        val db = TetherDatabase.get(applicationContext)
        val pipeline = AlertPipeline(applicationContext, db)

        scope.launch {
            val device = db.pairedDeviceDao().getOnce()
            if (device == null) {
                Log.i(TAG, "No paired device, nothing to monitor")
                stopSelf()
                return@launch
            }

            val gatt = GattConnectionManager(applicationContext, device.deviceAddress, scope)
            gattManager = gatt

            val disconnectMonitor = DisconnectMonitor(
                scope = scope,
                highPriorityWindowMs = { TetherRepository.settingsStore.highPriorityWindowMs() },
                lowPriorityWindowMs = { TetherRepository.settingsStore.lowPriorityWindowMs() },
                onReconnectAttempt = { gatt.connect() },
                onConfirmedDisconnect = { classification ->
                    val code = when (classification) {
                        DisconnectClassifier.Classification.ABRUPT ->
                            AlertPipeline.Classification.ABRUPT
                        DisconnectClassifier.Classification.GRADUAL ->
                            AlertPipeline.Classification.GRADUAL
                    }
                    pipeline.trigger(code)
                }
            )
            monitor = disconnectMonitor

            launch {
                gatt.events.collect { event ->
                    when (event) {
                        is GattConnectionManager.Event.Connected -> {
                            disconnectMonitor.onConnected()
                            TetherRepository.onConnectionChanged(true)
                        }
                        is GattConnectionManager.Event.Disconnected -> {
                            TetherRepository.onConnectionChanged(false)
                            disconnectMonitor.onDisconnected()
                        }
                        is GattConnectionManager.Event.Rssi -> {
                            disconnectMonitor.onRssi(event.value)
                            TetherRepository.onRssiUpdate(event.value)
                        }
                    }
                }
            }

            gatt.connect()
        }
    }

    override fun onDestroy() {
        gattManager?.disconnect()
        gattManager = null
        TetherRepository.onConnectionChanged(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TetherMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.example.tether.action.STOP_MONITORING"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, TetherMonitorService::class.java)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TetherMonitorService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
