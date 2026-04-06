package com.smartamenities.data.repository

import com.smartamenities.data.local.MockAmenityDataSource
import com.smartamenities.data.local.db.AmenityDao
import com.smartamenities.data.local.db.toDomain
import com.smartamenities.data.local.db.toEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Data older than this threshold triggers the stale-data warning in the UI. */
private const val STALE_THRESHOLD_MS = 15 * 60 * 1000L

/**
 * Cache-first implementation of [AmenityRepository] backed by Room.
 *
 * Strategy:
 *  1. On first launch: Room is empty → seed from [MockAmenityDataSource] so the
 *     app is usable immediately (replace seed call with Retrofit call when backend is live).
 *  2. Room emits data instantly → UI renders without waiting for network.
 *  3. Background job attempts a network refresh; on success it upserts Room rows.
 *  4. If cache age exceeds [STALE_THRESHOLD_MS] and no refresh succeeded,
 *     [observeDataFreshness] emits `true` so the UI can show a stale-data warning.
 *
 * To wire to the real backend: replace the body of [refreshFromNetwork] with a
 * Retrofit call and un-comment the dao.clearAll() + insertAll() lines.
 */
@Singleton
class RoomAmenityRepository @Inject constructor(
    private val dao: AmenityDao
) : AmenityRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val simulationConfig = MutableStateFlow(SimulationConfig())
    private val amenityOverrides = MutableStateFlow<Map<String, AmenitySimulationOverride>>(emptyMap())
    private val _isDataStale = MutableStateFlow(false)

    init {
        scope.launch { seedIfEmpty() }
        scope.launch { scheduleBackgroundRefresh() }
    }

    // ── AmenityRepository interface ───────────────────────────────────────────

    override fun observeDataFreshness(): Flow<Boolean> = _isDataStale.asStateFlow()

    override fun getAmenities(preferences: UserPreferences): Flow<List<Amenity>> =
        roomAmenitiesWithSimulation().map { amenities ->
            amenities
                .filter { matchesPreferences(it, preferences) }
                .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
        }

    override fun getAmenitiesByType(type: AmenityType, preferences: UserPreferences): Flow<List<Amenity>> =
        roomAmenitiesWithSimulation().map { amenities ->
            amenities
                .filter { it.type == type }
                .filter { matchesPreferences(it, preferences) }
                .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
        }

    override suspend fun getAmenityById(id: String): Amenity? {
        val entity = dao.getById(id) ?: return null
        return applySimulation(entity.toDomain(), simulationConfig.value, amenityOverrides.value[id])
    }

    override suspend fun getRoute(amenity: Amenity, preferences: UserPreferences): Route {
        val active = getAmenityById(amenity.id) ?: amenity
        return Route(
            amenityId = active.id,
            steps = MockAmenityDataSource.getMockRoute(active),
            totalWalkMinutes = active.estimatedWalkMinutes,
            totalWaitMinutes = active.crowdLevel.waitEstimateMinutes,
            isStepFreeRoute = active.isStepFreeRoute,
            computedAtTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus) {
        dao.updateStatus(amenityId, status.name)
        updateAmenityOverride(amenityId = amenityId, status = status)
    }

    override fun observeAdminSimulation(): Flow<AdminSimulationState> =
        combine(simulationConfig, roomAmenitiesWithSimulation(), amenityOverrides) { config, amenities, overrides ->
            AdminSimulationState(config = config, amenities = amenities, overrides = overrides)
        }

    override suspend fun updateSimulationConfig(config: SimulationConfig) {
        simulationConfig.value = config.normalized()
    }

    override suspend fun applySimulationPreset(preset: SimulationPreset) {
        simulationConfig.update { current ->
            when (preset) {
                SimulationPreset.HIGH_TRAFFIC -> current.copy(gateCount = 28, crowdLevel = 3, averageUsageTimeMinutes = 9,  isSystemOpen = true, isSimulationModeEnabled = true)
                SimulationPreset.LOW_TRAFFIC  -> current.copy(gateCount = 8,  crowdLevel = 0, averageUsageTimeMinutes = 3,  isSystemOpen = true, isSimulationModeEnabled = true)
                SimulationPreset.PEAK_HOURS   -> current.copy(gateCount = 22, crowdLevel = 2, averageUsageTimeMinutes = 7,  isSystemOpen = true, isSimulationModeEnabled = true)
            }.normalized()
        }
    }

    override suspend fun updateAmenityOverride(amenityId: String, status: AmenityStatus?, crowdLevel: CrowdLevel?) {
        amenityOverrides.update { existing ->
            val current = existing[amenityId] ?: AmenitySimulationOverride()
            val next = AmenitySimulationOverride(
                status = status ?: current.status,
                crowdLevel = crowdLevel ?: current.crowdLevel
            )
            if (next.status == null && next.crowdLevel == null) existing - amenityId
            else existing + (amenityId to next)
        }
    }

    override suspend fun clearAmenityOverride(amenityId: String) {
        amenityOverrides.update { it - amenityId }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun roomAmenitiesWithSimulation(): Flow<List<Amenity>> =
        combine(dao.getAll(), simulationConfig, amenityOverrides) { entities, config, overrides ->
            entities.map { applySimulation(it.toDomain(), config, overrides[it.id]) }
        }

    private suspend fun seedIfEmpty() {
        if (dao.oldestCacheTimestamp() == null) {
            // Room is empty — seed from mock data so app works before backend is ready.
            // Replace MockAmenityDataSource.amenities with an API call when backend is live.
            val now = System.currentTimeMillis()
            dao.insertAll(MockAmenityDataSource.amenities.map { it.toEntity(cachedAt = now) })
        }
        checkStaleness()
    }

    private suspend fun scheduleBackgroundRefresh() {
        delay(500) // let the initial Room emission reach the UI first
        refreshFromNetwork()
    }

    /**
     * Stub for the network refresh layer.
     * When the FastAPI backend is ready, replace this body with:
     *
     *   val fresh = amenityApi.getAllAmenities()   // Retrofit call
     *   val now   = System.currentTimeMillis()
     *   dao.clearAll()
     *   dao.insertAll(fresh.map { it.toEntity(cachedAt = now) })
     *   _isDataStale.value = false
     */
    private suspend fun refreshFromNetwork() {
        checkStaleness()
    }

    private suspend fun checkStaleness() {
        val oldest = dao.oldestCacheTimestamp() ?: return
        _isDataStale.value = (System.currentTimeMillis() - oldest) > STALE_THRESHOLD_MS
    }

    private fun matchesPreferences(amenity: Amenity, prefs: UserPreferences): Boolean {
        if (prefs.requiresWheelchairAccess && !amenity.isWheelchairAccessible) return false
        if (prefs.requiresStepFreeRoute && !amenity.isStepFreeRoute) return false
        if (prefs.preferFamilyRestroom && amenity.type == AmenityType.RESTROOM) return false
        return true
    }

    private fun applySimulation(amenity: Amenity, config: SimulationConfig, override: AmenitySimulationOverride?): Amenity {
        if (!config.isSimulationModeEnabled) return amenity
        val inScope = config.selectedLocation == SimulationLocation.TERMINAL_D_ALL ||
            MockAmenityDataSource.getSimulationLocation(amenity) == config.selectedLocation
        val effectiveStatus = when {
            override?.status != null          -> override.status
            inScope && !config.isSystemOpen   -> AmenityStatus.CLOSED
            inScope                           -> AmenityStatus.OPEN
            else                              -> amenity.status
        }
        val globalCrowd = if (inScope) config.crowdLevel.toCrowdLevel() else amenity.crowdLevel
        val congestionDelay = if (inScope) {
            ((config.gateCount / 7f) + (config.averageUsageTimeMinutes / 3.5f)).roundToInt() - 3
        } else 0
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
        0    -> CrowdLevel.EMPTY
        1    -> CrowdLevel.SHORT
        2    -> CrowdLevel.MEDIUM
        else -> CrowdLevel.LONG
    }

    private fun SimulationConfig.normalized(): SimulationConfig = copy(
        gateCount = gateCount.coerceIn(1, 40),
        crowdLevel = crowdLevel.coerceIn(0, 3),
        averageUsageTimeMinutes = averageUsageTimeMinutes.coerceIn(1, 60)
    )
}
