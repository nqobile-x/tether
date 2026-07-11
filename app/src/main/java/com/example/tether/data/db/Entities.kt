package com.example.tether.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_device")
data class PairedDeviceEntity(
    @PrimaryKey val deviceAddress: String,
    val deviceName: String,
    val pairedAt: Long,
    val lastKnownRssi: Int
)

@Entity(tableName = "trusted_contact")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isPrimary: Boolean
)

@Entity(tableName = "alert_event")
data class AlertEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val classification: String,
    val latitude: Double?,
    val longitude: Double?,
    // Comma-joined contact names, kept flat to avoid a join table in v1.
    val contactsNotified: String,
    val wasFalsePositive: Boolean
)
