package com.example.tether.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PairedDeviceEntity::class, TrustedContactEntity::class, AlertEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TetherDatabase : RoomDatabase() {
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun alertEventDao(): AlertEventDao

    companion object {
        @Volatile
        private var instance: TetherDatabase? = null

        fun get(context: Context): TetherDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TetherDatabase::class.java,
                    "tether.db"
                ).build().also { instance = it }
            }
    }
}
