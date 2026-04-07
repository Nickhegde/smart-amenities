package com.smartamenities.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedAmenity::class],
    version = 1,
    exportSchema = false
)
abstract class SmartAmenitiesDatabase : RoomDatabase() {
    abstract fun amenityDao(): AmenityDao
}
