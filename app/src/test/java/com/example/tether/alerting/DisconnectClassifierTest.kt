package com.example.tether.alerting

import com.example.tether.alerting.DisconnectClassifier.Classification
import com.example.tether.alerting.DisconnectClassifier.Sample
import org.junit.Assert.assertEquals
import org.junit.Test

class DisconnectClassifierTest {

    private val disconnectAt = 100_000L

    private fun samples(vararg pairs: Pair<Long, Int>) =
        pairs.map { Sample(disconnectAt - it.first, it.second) }

    @Test
    fun `strong signal then instant drop is abrupt`() {
        val history = samples(
            18_000L to -55, 14_000L to -54, 10_000L to -56,
            4_000L to -55, 2_000L to -54, 500L to -55
        )
        assertEquals(Classification.ABRUPT, DisconnectClassifier.classify(history, disconnectAt))
    }

    @Test
    fun `slow fade before drop is gradual`() {
        val history = samples(
            18_000L to -50, 14_000L to -52, 10_000L to -55,
            4_000L to -68, 2_000L to -72, 500L to -75
        )
        assertEquals(Classification.GRADUAL, DisconnectClassifier.classify(history, disconnectAt))
    }

    @Test
    fun `no history defaults to abrupt`() {
        assertEquals(Classification.ABRUPT, DisconnectClassifier.classify(emptyList(), disconnectAt))
    }

    @Test
    fun `only recent samples defaults to abrupt`() {
        val history = samples(3_000L to -60, 1_000L to -62)
        assertEquals(Classification.ABRUPT, DisconnectClassifier.classify(history, disconnectAt))
    }
}
