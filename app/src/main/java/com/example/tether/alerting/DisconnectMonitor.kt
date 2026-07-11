package com.example.tether.alerting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The debounce state machine. On disconnect it classifies the drop from the RSSI
 * history, waits the matching window while reconnect attempts run, and only fires
 * the alert if the device did not come back in time.
 */
class DisconnectMonitor(
    private val scope: CoroutineScope,
    private val highPriorityWindowMs: () -> Long,
    private val lowPriorityWindowMs: () -> Long,
    private val onReconnectAttempt: () -> Unit,
    private val onConfirmedDisconnect: suspend (DisconnectClassifier.Classification) -> Unit
) {
    private val samples = ArrayDeque<DisconnectClassifier.Sample>()
    private var pendingAlert: Job? = null

    @Synchronized
    fun onRssi(rssi: Int) {
        val now = System.currentTimeMillis()
        samples.addLast(DisconnectClassifier.Sample(now, rssi))
        while (samples.isNotEmpty() && now - samples.first().timeMs > SAMPLE_RETENTION_MS) {
            samples.removeFirst()
        }
    }

    @Synchronized
    fun onConnected() {
        pendingAlert?.cancel()
        pendingAlert = null
    }

    @Synchronized
    fun onDisconnected() {
        if (pendingAlert?.isActive == true) return
        val now = System.currentTimeMillis()
        val classification = DisconnectClassifier.classify(samples.toList(), now)
        val windowMs = when (classification) {
            DisconnectClassifier.Classification.ABRUPT -> highPriorityWindowMs()
            DisconnectClassifier.Classification.GRADUAL -> lowPriorityWindowMs()
        }
        pendingAlert = scope.launch {
            var waited = 0L
            while (waited < windowMs) {
                onReconnectAttempt()
                val step = minOf(RECONNECT_INTERVAL_MS, windowMs - waited)
                delay(step)
                waited += step
            }
            onConfirmedDisconnect(classification)
        }
    }

    private companion object {
        const val SAMPLE_RETENTION_MS = 30_000L
        const val RECONNECT_INTERVAL_MS = 5_000L
    }
}
