package com.smartamenities.data.local

import com.smartamenities.data.model.*

/**
 * Mock data source — replaces the real FastAPI backend during early development.
 *
 * HOW TO SWAP TO REAL BACKEND:
 *   1. Implement AmenityRemoteDataSource with Retrofit calls
 *   2. In AmenityRepositoryImpl, switch from MockAmenityDataSource to the remote source
 *   3. No changes needed in ViewModel or UI — the Repository interface stays the same
 *
 * The amenity positions (locationX, locationY) are normalized 0.0–1.0 coordinates
 * that map to Terminal D's floor plan. Replace these with real DFW Wayfinder
 * coordinates once the map integration is implemented in Iteration 2.
 */
object MockAmenityDataSource {

    private val now = System.currentTimeMillis()

    val amenities: List<Amenity> = listOf(

        // ── Floor 1 ──────────────────────────────────────────────────────────

        Amenity(
            id = "D1-REST-01",
            name = "Restroom – Near D10",
            type = AmenityType.RESTROOM,
            floor = 1,
            locationX = 0.15f,
            locationY = 0.60f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 2,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 120_000L,    // 2 min ago
            confidenceScore = 0.9f,
            gateProximity = "Near Gate D10"
        ),

        Amenity(
            id = "D1-REST-02",
            name = "Restroom – Near D22",
            type = AmenityType.RESTROOM,
            floor = 1,
            locationX = 0.42f,
            locationY = 0.55f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.MEDIUM,
            estimatedWalkMinutes = 4,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 300_000L,    // 5 min ago
            confidenceScore = 0.75f,
            gateProximity = "Near Gate D22"
        ),

        Amenity(
            id = "D1-FAM-01",
            name = "Family Restroom – Central",
            type = AmenityType.FAMILY_RESTROOM,
            floor = 1,
            locationX = 0.50f,
            locationY = 0.45f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 5,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = true,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 60_000L,     // 1 min ago
            confidenceScore = 0.95f,
            gateProximity = "Near Gate D25"
        ),

        Amenity(
            id = "D1-LAC-01",
            name = "Lactation Room – D Concourse",
            type = AmenityType.LACTATION_ROOM,
            floor = 1,
            locationX = 0.35f,
            locationY = 0.30f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 6,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 900_000L,    // 15 min ago (older data)
            confidenceScore = 0.5f,
            gateProximity = "Near Gate D20"
        ),

        Amenity(
            id = "D1-REST-03",
            name = "Restroom – Near D38",
            type = AmenityType.RESTROOM,
            floor = 1,
            locationX = 0.72f,
            locationY = 0.58f,
            status = AmenityStatus.CLOSED,              // Closed — tests FR 3.4 / FR 2.4
            crowdLevel = CrowdLevel.UNKNOWN,
            estimatedWalkMinutes = 8,
            isWheelchairAccessible = false,
            isStepFreeRoute = false,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 1_800_000L,  // 30 min ago
            confidenceScore = 0.3f,
            gateProximity = "Near Gate D38"
        ),

        // ── Floor 2 ──────────────────────────────────────────────────────────

        Amenity(
            id = "D2-REST-01",
            name = "Restroom – Upper Level",
            type = AmenityType.RESTROOM,
            floor = 2,
            locationX = 0.55f,
            locationY = 0.50f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.LONG,
            estimatedWalkMinutes = 7,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,                     // Accessible via elevator
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 180_000L,
            confidenceScore = 0.8f,
            gateProximity = "Near Gate D30"
        ),

        Amenity(
            id = "D2-GN-01",
            name = "Gender-Neutral Restroom – Upper",
            type = AmenityType.GENDER_NEUTRAL_RESTROOM,
            floor = 2,
            locationX = 0.65f,
            locationY = 0.40f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 9,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = true,
            dataFreshnessTimestamp = now - 240_000L,
            confidenceScore = 0.85f,
            gateProximity = "Near Gate D35"
        )
    )

    /**
     * Returns mock navigation steps from a hypothetical passenger position
     * to the given amenity. In Iteration 1 this is hardcoded; the real
     * RouteCalculator (Dijkstra's on a weighted graph) replaces this.
     */
    fun getMockRoute(amenity: Amenity): List<NavigationStep> = listOf(
        NavigationStep(
            stepNumber = 1,
            instruction = "Head toward Gate ${amenity.gateProximity.substringAfterLast(" ")}",
            directionIcon = DirectionIcon.STRAIGHT,
            distanceMeters = 45f,
            floorTransition = null
        ),
        NavigationStep(
            stepNumber = 2,
            instruction = "Turn right at the central corridor",
            directionIcon = DirectionIcon.TURN_RIGHT,
            distanceMeters = 20f,
            floorTransition = null
        ),
        NavigationStep(
            stepNumber = 3,
            instruction = if (amenity.floor == 2)
                "Take elevator to Floor 2"
            else
                "Continue straight for 30 meters",
            directionIcon = if (amenity.floor == 2) DirectionIcon.ELEVATOR_UP else DirectionIcon.STRAIGHT,
            distanceMeters = 30f,
            floorTransition = if (amenity.floor == 2) FloorTransition(1, 2, TransitionType.ELEVATOR) else null
        ),
        NavigationStep(
            stepNumber = 4,
            instruction = "${amenity.name} is on your left",
            directionIcon = DirectionIcon.ARRIVE,
            distanceMeters = 0f,
            floorTransition = null
        )
    )
}
