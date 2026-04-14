package com.smartamenities.data.local.db

import com.smartamenities.data.model.Amenity
import com.smartamenities.data.model.AmenityStatus
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.CrowdLevel

fun Amenity.toCached(): CachedAmenity = CachedAmenity(
    id = id,
    name = name,
    type = type.name,
    floor = floor,
    locationX = locationX,
    locationY = locationY,
    status = status.name,
    crowdLevel = crowdLevel.name,
    estimatedWalkMinutes = estimatedWalkMinutes,
    isWheelchairAccessible = isWheelchairAccessible,
    isStepFreeRoute = isStepFreeRoute,
    isFamilyRestroom = isFamilyRestroom,
    isGenderNeutral = isGenderNeutral,
    dataFreshnessTimestamp = dataFreshnessTimestamp,
    confidenceScore = confidenceScore,
    gateProximity = gateProximity
)

fun CachedAmenity.toDomain(): Amenity = Amenity(
    id = id,
    name = name,
    type = AmenityType.valueOf(type),
    floor = floor,
    locationX = locationX,
    locationY = locationY,
    status = runCatching { AmenityStatus.valueOf(status) }.getOrDefault(AmenityStatus.UNKNOWN),
    crowdLevel = runCatching { CrowdLevel.valueOf(crowdLevel) }.getOrDefault(CrowdLevel.UNKNOWN),
    estimatedWalkMinutes = estimatedWalkMinutes,
    isWheelchairAccessible = isWheelchairAccessible,
    isStepFreeRoute = isStepFreeRoute,
    isFamilyRestroom = isFamilyRestroom,
    isGenderNeutral = isGenderNeutral,
    dataFreshnessTimestamp = dataFreshnessTimestamp,
    confidenceScore = confidenceScore,
    gateProximity = gateProximity
)
