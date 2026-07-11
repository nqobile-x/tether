package com.example.tether.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_device LIMIT 1")
    fun observe(): Flow<PairedDeviceEntity?>

    @Query("SELECT * FROM paired_device LIMIT 1")
    suspend fun getOnce(): PairedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_device")
    suspend fun clear()

    @Query("UPDATE paired_device SET lastKnownRssi = :rssi WHERE deviceAddress = :address")
    suspend fun updateRssi(address: String, rssi: Int)
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contact ORDER BY isPrimary DESC, name ASC")
    fun observeAll(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contact ORDER BY isPrimary DESC, name ASC")
    suspend fun getAllOnce(): List<TrustedContactEntity>

    @Insert
    suspend fun insert(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contact WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE trusted_contact SET isPrimary = (id = :id)")
    suspend fun setPrimary(id: Long)

    @Query("SELECT COUNT(*) FROM trusted_contact WHERE isPrimary = 1")
    suspend fun primaryCount(): Int

    @Query("SELECT id FROM trusted_contact ORDER BY id ASC LIMIT 1")
    suspend fun firstId(): Long?
}

@Dao
interface AlertEventDao {
    @Query("SELECT * FROM alert_event ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AlertEventEntity>>

    @Insert
    suspend fun insert(event: AlertEventEntity)

    @Query("UPDATE alert_event SET wasFalsePositive = NOT wasFalsePositive WHERE id = :id")
    suspend fun toggleFalsePositive(id: Long)
}
