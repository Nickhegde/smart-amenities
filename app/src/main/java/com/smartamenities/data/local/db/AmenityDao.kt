package com.smartamenities.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AmenityDao {

    /** Upserts all amenities — used on network refresh and initial seed. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(amenities: List<AmenityEntity>)

    /** Live stream of all amenities ordered by walk time — drives the list UI. */
    @Query("SELECT * FROM amenities")
    fun getAll(): Flow<List<AmenityEntity>>

    /** One-shot fetch for detail screen. */
    @Query("SELECT * FROM amenities WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AmenityEntity?

    /** Live stream filtered by type — drives the type-filtered list view. */
    @Query("SELECT * FROM amenities WHERE type = :type")
    fun getByType(type: String): Flow<List<AmenityEntity>>

    /** Persists a status update from the passenger report flow. */
    @Query("UPDATE amenities SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Persists a crowd level change from the admin panel. */
    @Query("UPDATE amenities SET crowdLevel = :crowdLevel WHERE id = :id")
    suspend fun updateCrowdLevel(id: String, crowdLevel: String)

    /**
     * Returns the earliest [AmenityEntity.cachedAt] across all rows.
     * Null when the table is empty. Used to determine overall cache age.
     */
    @Query("SELECT MIN(cachedAt) FROM amenities")
    suspend fun oldestCacheTimestamp(): Long?

    /** Wipes all rows — called before a full network refresh. */
    @Query("DELETE FROM amenities")
    suspend fun clearAll()
}
