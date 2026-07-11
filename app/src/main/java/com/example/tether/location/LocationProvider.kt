package com.example.tether.location

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Thin wrapper over FusedLocationProviderClient. Last known location first,
 * one bounded fresh fix as fallback. No continuous tracking.
 */
class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    suspend fun lastKnownLocation(): Location? {
        return try {
            awaitTask(client.lastLocation) ?: freshLocation()
        } catch (e: SecurityException) {
            Log.w(TAG, "Location blocked, missing permission", e)
            null
        }
    }

    private suspend fun freshLocation(): Location? = withTimeoutOrNull(10_000) {
        try {
            val cts = CancellationTokenSource()
            awaitTask(
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            )
        } catch (e: SecurityException) {
            null
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            task.addOnFailureListener { if (cont.isActive) cont.resume(null) }
            task.addOnCanceledListener { if (cont.isActive) cont.resume(null) }
        }

    private companion object {
        const val TAG = "LocationProvider"
    }
}
