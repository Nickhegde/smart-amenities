package com.smartamenities.data.repository

import com.smartamenities.data.local.MockAmenityDataSource
import com.smartamenities.data.model.AdminSimulationState
import com.smartamenities.data.model.Amenity
import com.smartamenities.data.model.AmenitySimulationOverride
import com.smartamenities.data.model.AmenityStatus
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.CrowdLevel
import com.smartamenities.data.model.Route
import com.smartamenities.data.model.SimulationConfig
import com.smartamenities.data.model.SimulationLocation
import com.smartamenities.data.model.SimulationPreset
import com.smartamenities.data.model.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Repository interface — the contract the ViewModel depends on
//
// Keeping this as an interface means:
//   1. The ViewModel never knows or cares where data comes from
//   2. Swapping MockAmenityRepository for a real RemoteAmenityRepository
//      requires zero changes in the ViewModel or UI
// ─────────────────────────────────────────────────────────────────────────────

interface AmenityRepository {
    /** Emits all amenities, filtered and sorted by estimated time-to-reach */
    fun getAmenities(preferences: UserPreferences): Flow<List<Amenity>>

    /** Returns amenities of a specific type sorted by time-to-reach */
    fun getAmenitiesByType(type: AmenityType, preferences: UserPreferences): Flow<List<Amenity>>

    /** Fetches full detail for a single amenity (status refresh on open) */
    suspend fun getAmenityById(id: String): Amenity?

    /** Returns a route to the selected amenity respecting accessibility prefs */
    suspend fun getRoute(amenity: Amenity, preferences: UserPreferences): Route

    /** Reports a status update from the passenger (user reporting — SRS FR 3.5 area) */
    suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus)

    /** Emits the live admin simulator state for the hidden control panel. */
    fun observeAdminSimulation(): Flow<AdminSimulationState>

    /** Updates the global simulator controls. */
    suspend fun updateSimulationConfig(config: SimulationConfig)

    /** Applies a predefined simulator scenario. */
    suspend fun applySimulationPreset(preset: SimulationPreset)

    /** Overrides a single amenity with an admin-defined status or crowd level. */
    suspend fun updateAmenityOverride(
        amenityId: String,
        status: AmenityStatus? = null,
        crowdLevel: CrowdLevel? = null
    )

    /** Removes any admin override for the selected amenity. */
    suspend fun clearAmenityOverride(amenityId: String)

    /**
     * Emits `true` when cached data is older than 15 minutes and no fresh
     * network data has been received — triggers the stale-data warning in the UI.
     */
    fun observeDataFreshness(): Flow<Boolean>
}

// ─────────────────────────────────────────────────────────────────────────────
// Mock implementation — backed by MockAmenityDataSource
// Replace with RemoteAmenityRepository (Retrofit) when backend is ready
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MockAmenityRepository @Inject constructor() : AmenityRepository {

    private val fakeLatencyMs = 150L
    private val baseAmenities = MockAmenityDataSource.amenities
    private val simulationConfig = MutableStateFlow(SimulationConfig())
    private val amenityOverrides = MutableStateFlow<Map<String, AmenitySimulationOverride>>(emptyMap())
    private val simulatedAmenities = combine(simulationConfig, amenityOverrides) { config, overrides ->
        baseAmenities.map { amenity ->
            applySimulation(
                amenity = amenity,
                config = config,
                override = overrides[amenity.id]
            )
        }
    }

    override fun getAmenities(preferences: UserPreferences): Flow<List<Amenity>> =
        simulatedAmenities.map { amenities ->
            amenities
                .filter { amenity -> matchesPreferences(amenity, preferences) }
                .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
        }

    override fun getAmenitiesByType(
        type: AmenityType,
        preferences: UserPreferences
    ): Flow<List<Amenity>> = simulatedAmenities.map { amenities ->
        amenities
            .filter { it.type == type }
            .filter { amenity -> matchesPreferences(amenity, preferences) }
            .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
    }

    override suspend fun getAmenityById(id: String): Amenity? {
        delay(fakeLatencyMs)
        return simulatedAmenities.first().find { it.id == id }
    }

    override suspend fun getRoute(amenity: Amenity, preferences: UserPreferences): Route {
        delay(fakeLatencyMs * 2)
        val activeAmenity = getAmenityById(amenity.id) ?: amenity
        val steps = MockAmenityDataSource.getMockRoute(activeAmenity)
        return Route(
            amenityId = activeAmenity.id,
            steps = steps,
            totalWalkMinutes = activeAmenity.estimatedWalkMinutes,
            totalWaitMinutes = activeAmenity.crowdLevel.waitEstimateMinutes,
            isStepFreeRoute = activeAmenity.isStepFreeRoute,
            computedAtTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus) {
        delay(fakeLatencyMs)
        updateAmenityOverride(amenityId = amenityId, status = status)
    }

    override fun observeAdminSimulation(): Flow<AdminSimulationState> =
        combine(simulationConfig, simulatedAmenities, amenityOverrides) { config, amenities, overrides ->
            AdminSimulationState(
                config = config,
                amenities = amenities,
                overrides = overrides
            )
        }

    override suspend fun updateSimulationConfig(config: SimulationConfig) {
        simulationConfig.value = config.normalized()
    }

    override suspend fun applySimulationPreset(preset: SimulationPreset) {
        simulationConfig.update { current ->
            when (preset) {
                SimulationPreset.HIGH_TRAFFIC -> current.copy(
                    gateCount = 28,
                    crowdLevel = 3,
                    averageUsageTimeMinutes = 9,
                    isSystemOpen = true,
                    isSimulationModeEnabled = true
                )
                SimulationPreset.LOW_TRAFFIC -> current.copy(
                    gateCount = 8,
                    crowdLevel = 0,
                    averageUsageTimeMinutes = 3,
                    isSystemOpen = true,
                    isSimulationModeEnabled = true
                )
                SimulationPreset.PEAK_HOURS -> current.copy(
                    gateCount = 22,
                    crowdLevel = 2,
                    averageUsageTimeMinutes = 7,
                    isSystemOpen = true,
                    isSimulationModeEnabled = true
                )
            }.normalized()
        }
    }

    override suspend fun updateAmenityOverride(
        amenityId: String,
        status: AmenityStatus?,
        crowdLevel: CrowdLevel?
    ) {
        amenityOverrides.update { existing ->
            val current = existing[amenityId] ?: AmenitySimulationOverride()
            val nextOverride = AmenitySimulationOverride(
                status = status ?: current.status,
                crowdLevel = crowdLevel ?: current.crowdLevel
            )
            if (nextOverride.status == null && nextOverride.crowdLevel == null) {
                existing - amenityId
            } else {
                existing + (amenityId to nextOverride)
            }
        }
    }

    override suspend fun clearAmenityOverride(amenityId: String) {
        amenityOverrides.update { it - amenityId }
    }

    override fun observeDataFreshness(): Flow<Boolean> = flowOf(false)

    private fun matchesPreferences(amenity: Amenity, prefs: UserPreferences): Boolean {
        if (prefs.requiresWheelchairAccess && !amenity.isWheelchairAccessible) return false
        if (prefs.requiresStepFreeRoute && !amenity.isStepFreeRoute) return false
        if (prefs.preferFamilyRestroom && amenity.type == AmenityType.RESTROOM) return false
        return true
    }

    private fun applySimulation(
        amenity: Amenity,
        config: SimulationConfig,
        override: AmenitySimulationOverride?
    ): Amenity {
        if (!config.isSimulationModeEnabled) {
            return amenity
        }

        val inScope = config.selectedLocation == SimulationLocation.TERMINAL_D_ALL ||
            MockAmenityDataSource.getSimulationLocation(amenity) == config.selectedLocation
        val globalCrowd = if (inScope) config.crowdLevel.toCrowdLevel() else amenity.crowdLevel
        val effectiveStatus = when {
            override?.status != null -> override.status
            inScope && !config.isSystemOpen -> AmenityStatus.CLOSED
            inScope -> AmenityStatus.OPEN
            else -> amenity.status
        }
        val congestionDelay = if (inScope) {
            ((config.gateCount / 7f) + (config.averageUsageTimeMinutes / 3.5f)).roundToInt() - 3
        } else {
            0
        }
        val effectiveCrowd = when {
            effectiveStatus != AmenityStatus.OPEN && override?.crowdLevel == null -> CrowdLevel.UNKNOWN
            override?.crowdLevel != null -> override.crowdLevel
            else -> globalCrowd
        }

        return amenity.copy(
            status = effectiveStatus,
            crowdLevel = effectiveCrowd,
            estimatedWalkMinutes = (amenity.estimatedWalkMinutes + congestionDelay).coerceIn(1, 20),
            dataFreshnessTimestamp = if (inScope || override != null) System.currentTimeMillis() else amenity.dataFreshnessTimestamp,
            confidenceScore = if (inScope || override != null) 1f else amenity.confidenceScore
        )
    }

    private fun Int.toCrowdLevel(): CrowdLevel = when (coerceIn(0, 3)) {
        0 -> CrowdLevel.EMPTY
        1 -> CrowdLevel.SHORT
        2 -> CrowdLevel.MEDIUM
        else -> CrowdLevel.LONG
    }

    private fun SimulationConfig.normalized(): SimulationConfig = copy(
        gateCount = gateCount.coerceIn(1, 40),
        crowdLevel = crowdLevel.coerceIn(0, 3),
        averageUsageTimeMinutes = averageUsageTimeMinutes.coerceIn(1, 60)
    )
}
