package com.example.tether.alerting

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tether.R
import com.example.tether.TetherApplication
import com.example.tether.data.db.AlertEventEntity
import com.example.tether.data.db.TetherDatabase
import com.example.tether.location.LocationProvider
import java.util.Locale

/**
 * Confirmed disconnect (or test) to SMS and history log:
 * fetch last known location, text every trusted contact, persist the event.
 */
class AlertPipeline(
    private val context: Context,
    private val db: TetherDatabase,
    private val locationProvider: LocationProvider = LocationProvider(context),
    private val smsSender: SmsSender = SmsSender(context)
) {
    object Classification {
        const val ABRUPT = "abrupt_disconnect"
        const val GRADUAL = "gradual_disconnect"
        const val TEST = "test_alert"
    }

    suspend fun trigger(classification: String) {
        val contacts = db.trustedContactDao().getAllOnce()
        val location = locationProvider.lastKnownLocation()

        val locationText = if (location != null) {
            String.format(
                Locale.US,
                "https://maps.google.com/?q=%.5f,%.5f",
                location.latitude,
                location.longitude
            )
        } else {
            context.getString(R.string.sms_location_unknown)
        }

        val deviceName = db.pairedDeviceDao().getOnce()?.deviceName
            ?: context.getString(R.string.no_device_paired)

        val message = if (classification == Classification.TEST) {
            context.getString(R.string.sms_test_message, deviceName, locationText)
        } else {
            context.getString(R.string.sms_alert_message, deviceName, locationText)
        }

        val notified = mutableListOf<String>()
        for (contact in contacts) {
            if (smsSender.send(contact.phoneNumber, message)) {
                notified += contact.name
            }
        }
        Log.i(TAG, "Alert $classification, notified ${notified.size}/${contacts.size} contacts")

        db.alertEventDao().insert(
            AlertEventEntity(
                timestamp = System.currentTimeMillis(),
                classification = classification,
                latitude = location?.latitude,
                longitude = location?.longitude,
                contactsNotified = notified.joinToString(","),
                wasFalsePositive = false
            )
        )

        postLocalNotification(classification, notified.size)
    }

    private fun postLocalNotification(classification: String, notifiedCount: Int) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val title = if (classification == Classification.TEST) {
                context.getString(R.string.notif_test_alert_title)
            } else {
                context.getString(R.string.notif_alert_title)
            }
            val notification =
                NotificationCompat.Builder(context, TetherApplication.ALERT_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(
                        context.getString(R.string.notif_alert_body, notifiedCount)
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
            manager.notify(classification.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification blocked", e)
        }
    }

    private companion object {
        const val TAG = "AlertPipeline"
    }
}
