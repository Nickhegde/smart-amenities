package com.smartamenities.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartamenities.data.model.Amenity
import com.smartamenities.data.model.AmenityStatus
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.CrowdLevel

/**
 * Room entity mirroring the Amenity domain model.
 *
 * [cachedAt] is a Room-only column tracking when this row was last written.
 * The RoomAmenityRepository uses it to detect stale data (> 15 min old).
 * It is stripped when converting back to the domain Amenity via [toDomain].
 */
@Entity(tableName = "amenities")
data class AmenityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,                    // AmenityType.name — Room has no enum support
    val floor: Int,
    val locationX: Float,
    val locationY: Float,
    val status: String,                  // AmenityStatus.name
    val crowdLevel: String,              // CrowdLevel.name
    val isWheelchairAccessible: Boolean,
    val isStepFreeRoute: Boolean,
    val isFamilyRestroom: Boolean,
    val isGenderNeutral: Boolean,
    val dataFreshnessTimestamp: Long,
    val confidenceScore: Float,
    val gateProximity: String,
    val cachedAt: Long                   // Room-only: when this row was last refreshed
)

fun AmenityEntity.toDomain(): Amenity = Amenity(
    id = id,
    name = name,
    type = AmenityType.valueOf(type),
    floor = floor,
    locationX = locationX,
    locationY = locationY,
    status = AmenityStatus.valueOf(status),
    crowdLevel = CrowdLevel.valueOf(crowdLevel),
    estimatedWalkMinutes = 0,            // computed at runtime by RouteCalculator, not stored
    isWheelchairAccessible = isWheelchairAccessible,
    isStepFreeRoute = isStepFreeRoute,
    isFamilyRestroom = isFamilyRestroom,
    isGenderNeutral = isGenderNeutral,
    dataFreshnessTimestamp = dataFreshnessTimestamp,
    confidenceScore = confidenceScore,
    gateProximity = gateProximity
)

fun Amenity.toEntity(cachedAt: Long = System.currentTimeMillis()): AmenityEntity = AmenityEntity(
    id = id,
    name = name,
    type = type.name,
    floor = floor,
    locationX = locationX,
    locationY = locationY,
    status = status.name,
    crowdLevel = crowdLevel.name,
    isWheelchairAccessible = isWheelchairAccessible,
    isStepFreeRoute = isStepFreeRoute,
    isFamilyRestroom = isFamilyRestroom,
    isGenderNeutral = isGenderNeutral,
    dataFreshnessTimestamp = dataFreshnessTimestamp,
    confidenceScore = confidenceScore,
    gateProximity = gateProximity,
    cachedAt = cachedAt
)
