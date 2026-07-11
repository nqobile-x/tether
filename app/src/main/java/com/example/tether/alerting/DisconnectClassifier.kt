package com.example.tether.alerting

/**
 * Rule-based classification of a BLE disconnect, kept pure so it is unit-testable
 * and explainable in one sentence: if the signal was already fading before the drop
 * it is gradual (boring), if it was strong and then instantly gone it is abrupt.
 */
object DisconnectClassifier {

    data class Sample(val timeMs: Long, val rssi: Int)

    enum class Classification { ABRUPT, GRADUAL }

    private const val RECENT_WINDOW_MS = 5_000L
    private const val BASELINE_WINDOW_MS = 20_000L
    private const val DECLINE_THRESHOLD_DB = 10.0

    fun classify(samples: List<Sample>, disconnectTimeMs: Long): Classification {
        val recent = samples.filter { disconnectTimeMs - it.timeMs in 0..RECENT_WINDOW_MS }
        val baseline = samples.filter {
            disconnectTimeMs - it.timeMs in (RECENT_WINDOW_MS + 1)..BASELINE_WINDOW_MS
        }
        // Too little history to see a fade means we assume the worst case.
        if (recent.isEmpty() || baseline.isEmpty()) return Classification.ABRUPT

        val recentAvg = recent.map { it.rssi }.average()
        val baselineAvg = baseline.map { it.rssi }.average()
        return if (baselineAvg - recentAvg >= DECLINE_THRESHOLD_DB) {
            Classification.GRADUAL
        } else {
            Classification.ABRUPT
        }
    }
}
