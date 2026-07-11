package com.example.tether

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.tether.data.TetherRepository

class TetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        TetherRepository.initialize(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                MONITOR_CHANNEL_ID,
                getString(R.string.channel_monitor_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_monitor_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.channel_alert_desc) }
        )
    }

    companion object {
        const val MONITOR_CHANNEL_ID = "monitoring"
        const val ALERT_CHANNEL_ID = "alerts"
    }
}
