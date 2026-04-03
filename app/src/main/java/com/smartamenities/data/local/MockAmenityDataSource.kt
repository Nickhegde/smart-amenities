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
 * matching GraphConstants node positions for Terminal D Level 3 (post-security).
 */
object MockAmenityDataSource {

    private val now = System.currentTimeMillis()

    val amenities: List<Amenity> = listOf(

        // ── Level 3 Restrooms (Top Wall: Gates D5–D22) ───────────────────────

        Amenity(
            id = "REST_D6",
            name = "Restroom – Near Gate D6",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.10f,
            locationY = 0.35f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 8,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 90_000L,
            confidenceScore = 0.92f,
            gateProximity = "Near Gate D6"
        ),

        Amenity(
            id = "REST_D10",
            name = "Restroom – Near Gate D10",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.34f,
            locationY = 0.35f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.MEDIUM,
            estimatedWalkMinutes = 6,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 180_000L,
            confidenceScore = 0.88f,
            gateProximity = "Near Gate D10"
        ),

        Amenity(
            id = "REST_D17",
            name = "Restroom – Near Gate D17",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.70f,
            locationY = 0.35f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 4,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 60_000L,
            confidenceScore = 0.95f,
            gateProximity = "Near Gate D17"
        ),

        Amenity(
            id = "REST_D20",
            name = "Restroom – Near Gate D20",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.88f,
            locationY = 0.35f,
            status = AmenityStatus.OUT_OF_SERVICE,
            crowdLevel = CrowdLevel.UNKNOWN,
            estimatedWalkMinutes = 2,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 600_000L,
            confidenceScore = 0.70f,
            gateProximity = "Near Gate D20"
        ),

        Amenity(
            id = "REST_D22",
            name = "Restroom – Near Gate D22",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.96f,
            locationY = 0.35f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.LONG,
            estimatedWalkMinutes = 1,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 30_000L,
            confidenceScore = 0.97f,
            gateProximity = "Near Gate D22"
        ),

        // ── Level 3 Restrooms (Bottom Wall: Gates D23–D40) ───────────────────

        Amenity(
            id = "REST_D24",
            name = "Restroom – Near Gate D24",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.90f,
            locationY = 0.65f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.MEDIUM,
            estimatedWalkMinutes = 2,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 120_000L,
            confidenceScore = 0.90f,
            gateProximity = "Near Gate D24"
        ),

        Amenity(
            id = "REST_D27",
            name = "Restroom – Near Gate D27",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.72f,
            locationY = 0.65f,
            status = AmenityStatus.CLOSED,
            crowdLevel = CrowdLevel.UNKNOWN,
            estimatedWalkMinutes = 3,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 1_200_000L,
            confidenceScore = 0.60f,
            gateProximity = "Near Gate D27"
        ),

        Amenity(
            id = "REST_D29",
            name = "Restroom – Near Gate D29",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.60f,
            locationY = 0.65f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 3,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 150_000L,
            confidenceScore = 0.87f,
            gateProximity = "Near Gate D29"
        ),

        Amenity(
            id = "REST_D36",
            name = "Restroom – Near Gate D36",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.30f,
            locationY = 0.65f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 5,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 240_000L,
            confidenceScore = 0.83f,
            gateProximity = "Near Gate D36"
        ),

        Amenity(
            id = "REST_D40",
            name = "Restroom – Near Gate D40",
            type = AmenityType.RESTROOM,
            floor = 3,
            locationX = 0.06f,
            locationY = 0.65f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.MEDIUM,
            estimatedWalkMinutes = 7,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 300_000L,
            confidenceScore = 0.80f,
            gateProximity = "Near Gate D40"
        ),

        // ── Level 3 Family Restrooms ──────────────────────────────────────────

        Amenity(
            id = "FAM_D18",
            name = "Family Restroom – Near Gate D18",
            type = AmenityType.FAMILY_RESTROOM,
            floor = 3,
            locationX = 0.76f,
            locationY = 0.35f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 2,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = true,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 45_000L,
            confidenceScore = 0.94f,
            gateProximity = "Near Gate D18"
        ),

        Amenity(
            id = "FAM_D25",
            name = "Family Restroom – Near Gate D25",
            type = AmenityType.FAMILY_RESTROOM,
            floor = 3,
            locationX = 0.84f,
            locationY = 0.65f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 2,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = true,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 75_000L,
            confidenceScore = 0.93f,
            gateProximity = "Near Gate D25"
        ),

        Amenity(
            id = "FAM_D28",
            name = "Family Restroom – Near Gate D28",
            type = AmenityType.FAMILY_RESTROOM,
            floor = 3,
            locationX = 0.66f,
            locationY = 0.65f,
            status = AmenityStatus.OUT_OF_SERVICE,
            crowdLevel = CrowdLevel.UNKNOWN,
            estimatedWalkMinutes = 3,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = true,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 900_000L,
            confidenceScore = 0.55f,
            gateProximity = "Near Gate D28"
        ),

        // ── Level 3 Lactation Room ────────────────────────────────────────────

        Amenity(
            id = "LAC_D22",
            name = "Lactation Room – Near Gate D22",
            type = AmenityType.LACTATION_ROOM,
            floor = 3,
            locationX = 0.96f,
            locationY = 0.38f,
            status = AmenityStatus.OPEN,
            crowdLevel = CrowdLevel.SHORT,
            estimatedWalkMinutes = 1,
            isWheelchairAccessible = true,
            isStepFreeRoute = true,
            isFamilyRestroom = false,
            isGenderNeutral = false,
            dataFreshnessTimestamp = now - 60_000L,
            confidenceScore = 0.96f,
            gateProximity = "Near Gate D22"
        ),
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
            instruction = "Continue straight for 30 meters",
            directionIcon = DirectionIcon.STRAIGHT,
            distanceMeters = 30f,
            floorTransition = null
        ),
        NavigationStep(
            stepNumber = 4,
            instruction = "${amenity.name} is on your left",
            directionIcon = DirectionIcon.ARRIVE,
            distanceMeters = 0f,
            floorTransition = null
        )
    )

    fun getSimulationLocation(amenity: Amenity): SimulationLocation {
        val gateNumber = "D(\\d+)".toRegex()
            .find(amenity.gateProximity)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        return when (gateNumber) {
            null -> SimulationLocation.TERMINAL_D_ALL
            in 5..18  -> SimulationLocation.TERMINAL_D_EAST
            in 19..30 -> SimulationLocation.TERMINAL_D_CENTRAL
            else      -> SimulationLocation.TERMINAL_D_WEST
        }
    }
}
