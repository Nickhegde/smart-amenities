package com.smartamenities.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AmenityEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun amenityDao(): AmenityDao
}
