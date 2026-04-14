package com.smartamenities.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AmenityDao {

    @Query("SELECT * FROM cached_amenities WHERE type = :type")
    suspend fun getByType(type: String): List<CachedAmenity>

    @Query("SELECT * FROM cached_amenities")
    suspend fun getAll(): List<CachedAmenity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(amenities: List<CachedAmenity>)

    @Query("DELETE FROM cached_amenities WHERE type = :type")
    suspend fun deleteByType(type: String)
}
