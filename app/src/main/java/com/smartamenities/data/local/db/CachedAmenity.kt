package com.smartamenities.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_amenities")
data class CachedAmenity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,               // AmenityType.name  e.g. "RESTROOM"
    val floor: Int,
    val locationX: Float,
    val locationY: Float,
    val status: String,             // AmenityStatus.name e.g. "OPEN"
    val crowdLevel: String,         // CrowdLevel.name    e.g. "SHORT"
    val estimatedWalkMinutes: Int,
    val isWheelchairAccessible: Boolean,
    val isStepFreeRoute: Boolean,
    val isFamilyRestroom: Boolean,
    val isGenderNeutral: Boolean,
    val dataFreshnessTimestamp: Long,
    val confidenceScore: Float,
    val gateProximity: String
)
