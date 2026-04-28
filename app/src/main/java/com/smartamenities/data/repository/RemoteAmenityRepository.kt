package com.smartamenities.data.repository

import android.util.Log
import com.smartamenities.data.graph.TerminalDGraph
import com.smartamenities.data.local.MockAmenityDataSource
import com.smartamenities.data.local.db.AmenityDao
import com.smartamenities.data.local.db.toCached
import com.smartamenities.data.local.db.toDomain
import com.smartamenities.data.model.AdminSimulationState
import com.smartamenities.data.model.Amenity
import com.smartamenities.data.model.AmenitySimulationOverride
import com.smartamenities.data.model.AmenityStatus
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.CrowdLevel
import com.smartamenities.data.model.DirectionIcon
import com.smartamenities.data.model.NavigationStep
import com.smartamenities.data.model.Route
import com.smartamenities.data.model.SimulationConfig
import com.smartamenities.data.model.SimulationLocation
import com.smartamenities.data.model.SimulationPreset
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.data.remote.ApiService
import com.smartamenities.data.remote.RouteOptionDto
import com.smartamenities.data.remote.RouteRecommendRequestDto
import com.smartamenities.data.remote.AmenityOverrideRequestDto
import com.smartamenities.data.remote.ScenarioApplyRequestDto
import com.smartamenities.data.remote.ZoneControlRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "RemoteAmenityRepo"

// Default start node — center of Terminal D's main corridor.
// Admin can override via setUserNode(); a real location SDK would update this automatically.
private const val DEFAULT_USER_NODE = "COR_C"

@Singleton
class RemoteAmenityRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: AmenityDao,
    private val mock: MockAmenityRepository
) : AmenityRepository {

    // Current user location node — defaults to corridor center, overridable by admin.
    @Volatile private var userNode: String = DEFAULT_USER_NODE

    // Cached per-amenity recommendation from the last backend call.
    // Keyed by amenity_id so getRoute() can use the pre-computed path.
    private val recommendationCache = ConcurrentHashMap<String, RouteOptionDto>()

    // In-memory list of the last successfully fetched amenities, keyed by AmenityType name.
    // Used to re-filter without an API call when only accessibility prefs change.
    private val inMemoryAmenities = ConcurrentHashMap<String, List<Amenity>>()

    // Emit Unit here after any admin write to trigger a re-fetch in observeAdminSimulation().
    private val adminRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Base amenity data from mock — provides static fields (name, floor, coordinates, etc.)
    // that the backend route endpoint doesn't return.
    private val baseAmenityMap = MockAmenityDataSource.amenities.associateBy { it.id }

    // ── Amenity list — powered by backend recommendations ────────────────────

    override fun getAmenitiesByType(
        type: AmenityType,
        preferences: UserPreferences
    ): Flow<List<Amenity>> = flow {
        // Wheelchair / step-free are client-side filters — re-filter in memory if we
        // already have data for this type, no API call needed.
        val memCached = inMemoryAmenities[type.name]
        if (memCached != null) {
            Log.d(TAG, "Re-filtering ${memCached.size} in-memory amenities for ${type.name}")
            emit(memCached.filter { matchesPreferences(it, preferences) })
            return@flow
        }

        try {
            val recommendations = fetchRecommendations(type, preferences)
            emit(recommendations)
        } catch (e: Exception) {
            Log.e(TAG, "getAmenitiesByType failed, trying Room cache: ${e.message}")
            val cached = dao.getByType(type.name)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Serving ${cached.size} Room-cached amenities for ${type.name}")
                emit(cached.map { it.toDomain() }.filter { matchesPreferences(it, preferences) })
            } else {
                Log.w(TAG, "Room cache empty, falling back to mock")
                emitAll(mock.getAmenitiesByType(type, preferences))
            }
        }
    }

    override fun getAmenities(preferences: UserPreferences): Flow<List<Amenity>> = flow {
        // "All" — fetch every amenity type, merge, and sort by total time (walk + wait).
        try {
            val allAmenities = mutableListOf<Amenity>()
            for (type in AmenityType.values()) {
                val cached = inMemoryAmenities[type.name]
                if (cached != null) {
                    Log.d(TAG, "Using in-memory cache for ${type.name} (${cached.size} items)")
                    allAmenities.addAll(cached)
                } else {
                    fetchRecommendations(type, preferences)
                    allAmenities.addAll(inMemoryAmenities[type.name] ?: emptyList())
                }
            }
            val result = allAmenities
                .filter { matchesPreferences(it, preferences) }
                .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getAmenities failed, trying Room cache: ${e.message}")
            val cached = AmenityType.values().flatMap { type ->
                dao.getByType(type.name).map { it.toDomain() }
            }
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Serving ${cached.size} Room-cached amenities (all types)")
                emit(cached
                    .filter { matchesPreferences(it, preferences) }
                    .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes })
            } else {
                Log.w(TAG, "Room cache empty, falling back to mock")
                emitAll(mock.getAmenities(preferences))
            }
        }
    }

    // ── Route — use the path the backend already computed for this amenity ───

    override suspend fun getRoute(amenity: Amenity, preferences: UserPreferences): Route {
        val cached = recommendationCache[amenity.id]
        return if (cached != null) {
            Log.d(TAG, "Using cached path for ${amenity.id}: ${cached.path}")
            Route(
                amenityId = cached.amenityId,
                steps = pathToNavigationSteps(cached.path, amenity.name),
                totalWalkMinutes = (cached.walkSeconds / 60).coerceAtLeast(1),
                totalWaitMinutes = cached.waitSeconds / 60,
                isStepFreeRoute = amenity.isStepFreeRoute,
                computedAtTimestamp = System.currentTimeMillis(),
                routeNodeIds = cached.path
            )
        } else {
            // Cache miss — amenity list wasn't loaded yet, fetch on demand.
            Log.w(TAG, "No cached path for ${amenity.id}, fetching on demand")
            try {
                val recommendations = fetchRecommendations(amenity.type, preferences)
                val matchingAmenity = recommendations.firstOrNull { it.id == amenity.id }
                val freshCached = recommendationCache[amenity.id]
                if (freshCached != null) {
                    Route(
                        amenityId = freshCached.amenityId,
                        steps = pathToNavigationSteps(freshCached.path, amenity.name),
                        totalWalkMinutes = (freshCached.walkSeconds / 60).coerceAtLeast(1),
                        totalWaitMinutes = freshCached.waitSeconds / 60,
                        isStepFreeRoute = (matchingAmenity ?: amenity).isStepFreeRoute,
                        computedAtTimestamp = System.currentTimeMillis(),
                        routeNodeIds = freshCached.path
                    )
                } else {
                    mock.getRoute(amenity, preferences)
                }
            } catch (e: Exception) {
                Log.e(TAG, "On-demand route fetch failed, using mock: ${e.message}")
                mock.getRoute(amenity, preferences)
            }
        }
    }

    // ── Non-admin pass-throughs ──────────────────────────────────────────────

    override suspend fun getAmenityById(id: String): Amenity? = mock.getAmenityById(id)

    override suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus) =
        mock.reportAmenityStatus(amenityId, status)

    // ── Admin — reads ────────────────────────────────────────────────────────

    /**
     * Fetches all amenities from the admin endpoint and re-emits whenever an
     * admin write operation completes (via adminRefreshTrigger).
     * Falls back to the mock if the backend is unreachable.
     */
    override fun observeAdminSimulation(): Flow<AdminSimulationState> =
        merge(flowOf(Unit), adminRefreshTrigger).map {
            try {
                val dtos = apiService.getAdminAmenities()
                val amenities = dtos.map { it.toDomain(baseAmenityMap) }
                AdminSimulationState(amenities = amenities)
            } catch (e: Exception) {
                Log.e(TAG, "getAdminAmenities failed: ${e.message}")
                AdminSimulationState()
            }
        }

    // ── Admin — single amenity override ─────────────────────────────────────

    override suspend fun updateAmenityOverride(
        amenityId: String,
        status: AmenityStatus?,
        crowdLevel: CrowdLevel?
    ) {
        try {
            apiService.updateAmenity(
                amenityId,
                AmenityOverrideRequestDto(
                    status     = status?.displayName,
                    crowdLevel = crowdLevel?.displayName
                )
            )
            invalidateCaches()
            adminRefreshTrigger.tryEmit(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateAmenity $amenityId failed: ${e.message}")
            throw e
        }
    }

    override suspend fun clearAmenityOverride(amenityId: String) {
        try {
            apiService.resetAmenity(amenityId)
            invalidateCaches()
            adminRefreshTrigger.tryEmit(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "resetAmenity $amenityId failed: ${e.message}")
            throw e
        }
    }

    // ── Admin — zone bulk control ────────────────────────────────────────────

    override suspend fun updateSimulationConfig(config: SimulationConfig) {
        try {
            val zoneName = when (config.selectedLocation) {
                SimulationLocation.TERMINAL_D_ALL     -> "All Zones"
                SimulationLocation.TERMINAL_D_EAST    -> "East Zone"
                SimulationLocation.TERMINAL_D_CENTRAL -> "Central Zone"
                SimulationLocation.TERMINAL_D_WEST    -> "West Zone"
            }
            val crowdEnum = when (config.crowdLevel.coerceIn(0, 3)) {
                0    -> CrowdLevel.EMPTY
                1    -> CrowdLevel.SHORT
                2    -> CrowdLevel.MEDIUM
                else -> CrowdLevel.LONG
            }
            apiService.updateZone(
                ZoneControlRequestDto(
                    zone            = zoneName,
                    crowdLevel      = crowdEnum.displayName,
                    avgUsageMinutes = config.averageUsageTimeMinutes,
                    isOpen          = config.isSystemOpen
                )
            )
            invalidateCaches()
            adminRefreshTrigger.tryEmit(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateZone failed: ${e.message}")
            throw e
        }
    }

    // ── Admin — preset scenarios ─────────────────────────────────────────────

    override suspend fun applySimulationPreset(preset: SimulationPreset) {
        try {
            val scenarios = apiService.getScenarios()
            val match = scenarios.firstOrNull { it.name == preset.displayName }
                ?: throw Exception("No backend scenario matching '${preset.displayName}'")
            apiService.applyScenario(ScenarioApplyRequestDto(match.id))
            invalidateCaches()
            adminRefreshTrigger.tryEmit(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "applySimulationPreset ${preset.displayName} failed: ${e.message}")
            throw e
        }
    }

    /**
     * Updates the origin node for all future route recommendations.
     * Clears both caches so the next getAmenities() call fetches fresh paths from the new location.
     */
    override fun setUserNode(node: String) {
        if (node == userNode) return
        Log.d(TAG, "User node changed: $userNode → $node")
        userNode = node
        inMemoryAmenities.clear()
        recommendationCache.clear()
    }

    // ── Core backend call ────────────────────────────────────────────────────

    /**
     * Calls POST /api/route/recommend, caches each RouteOptionDto by amenity_id,
     * and returns the recommendations mapped to enriched Amenity domain objects
     * in backend-ranked order (best total_seconds first).
     */
    private suspend fun fetchRecommendations(
        type: AmenityType,
        preferences: UserPreferences
    ): List<Amenity> {
        val request = RouteRecommendRequestDto(
            userNode = userNode,
            amenityType = type.displayName,       // e.g. "Restroom", "Family Restroom"
            wheelchairRequired = preferences.requiresWheelchairAccess
        )
        val response = apiService.getRouteRecommendations(request)
        Log.d(TAG, "Backend returned ${response.recommendations.size} recommendations for ${type.displayName}")

        // Cache route paths so getRoute() can use them without a second API call.
        response.recommendations.forEach { option ->
            recommendationCache[option.amenityId] = option
        }

        // Merge backend data onto the static base amenity.
        // dataFreshnessTimestamp = now so "updated X min ago" reflects this actual fetch.
        val fetchedAt = System.currentTimeMillis()
        val enrichedAmenities = response.recommendations.mapNotNull { option ->
            baseAmenityMap[option.amenityId]?.copy(
                status = option.status.toAmenityStatus(),
                crowdLevel = option.crowdLevel.toCrowdLevel(),
                estimatedWalkMinutes = (option.walkSeconds / 60).coerceAtLeast(1),
                dataFreshnessTimestamp = fetchedAt
            )
        }

        // Save to in-memory cache (for fast re-filtering without API calls)
        // and to Room (for offline use across sessions).
        if (enrichedAmenities.isNotEmpty()) {
            inMemoryAmenities[type.name] = enrichedAmenities
            dao.deleteByType(type.name)
            dao.insertAll(enrichedAmenities.map { it.toCached() })
            Log.d(TAG, "Cached ${enrichedAmenities.size} amenities for ${type.name}")
        }

        return enrichedAmenities.filter { matchesPreferences(it, preferences) }
        // List is already sorted by total_seconds from the backend.
    }

    // ── Cache invalidation ───────────────────────────────────────────────────

    /** Clear both in-memory caches after any admin write so the next passenger
     *  fetch picks up the updated state from the backend. */
    private fun invalidateCaches() {
        inMemoryAmenities.clear()
        recommendationCache.clear()
    }

    // ── Admin DTO → domain model ─────────────────────────────────────────────

    private fun com.smartamenities.data.remote.AmenityAdminResponseDto.toDomain(
        baseMap: Map<String, Amenity>
    ): Amenity {
        val base = baseMap[id]
        return Amenity(
            id                     = id,
            name                   = name,
            type                   = type.toAmenityType(),
            floor                  = floor,
            locationX              = base?.locationX ?: 0.5f,
            locationY              = base?.locationY ?: 0.5f,
            status                 = status.toAmenityStatus(),
            crowdLevel             = crowdLevel.toCrowdLevel(),
            estimatedWalkMinutes   = base?.estimatedWalkMinutes ?: avgUsageMinutes,
            isWheelchairAccessible = isWheelchairAccessible,
            isStepFreeRoute        = isStepFreeRoute,
            isFamilyRestroom       = isFamilyRestroom,
            isGenderNeutral        = isGenderNeutral,
            dataFreshnessTimestamp = System.currentTimeMillis(),
            confidenceScore        = 1.0f,
            gateProximity          = gateProximity
        )
    }

    private fun String.toAmenityType(): com.smartamenities.data.model.AmenityType =
        com.smartamenities.data.model.AmenityType.values()
            .firstOrNull { it.displayName == this }
            ?: com.smartamenities.data.model.AmenityType.RESTROOM

    // ── Enum mapping — backend sends display-value strings ───────────────────

    private fun String.toCrowdLevel(): CrowdLevel =
        CrowdLevel.values().firstOrNull { it.displayName == this } ?: CrowdLevel.UNKNOWN

    private fun String.toAmenityStatus(): AmenityStatus =
        AmenityStatus.values().firstOrNull { it.displayName == this } ?: AmenityStatus.UNKNOWN

    // ── Preference helpers ───────────────────────────────────────────────────

    /**
     * Picks the backend amenity type to query based on what the user prefers.
     * Gender-neutral takes priority over family restroom; both override the default.
     */
    private fun effectiveAmenityType(preferences: UserPreferences): AmenityType = when {
        preferences.preferGenderNeutral  -> AmenityType.GENDER_NEUTRAL_RESTROOM
        preferences.preferFamilyRestroom -> AmenityType.FAMILY_RESTROOM
        else                             -> preferences.preferredAmenityType
    }

    private fun matchesPreferences(amenity: Amenity, prefs: UserPreferences): Boolean {
        if (prefs.requiresWheelchairAccess && !amenity.isWheelchairAccessible) return false
        if (prefs.requiresStepFreeRoute && !amenity.isStepFreeRoute) return false
        if (prefs.preferFamilyRestroom && amenity.type == AmenityType.RESTROOM) return false
        if (prefs.preferGenderNeutral && amenity.type != AmenityType.GENDER_NEUTRAL_RESTROOM) return false
        return true
    }

    // ── Path → NavigationStep conversion ────────────────────────────────────

    /**
     * Converts a list of node IDs returned by the backend (e.g. ["COR_C", "SKY_E", "D30", "REST_D29"])
     * into human-readable NavigationStep objects for the turn-by-turn screen.
     */
    private fun pathToNavigationSteps(path: List<String>, amenityName: String): List<NavigationStep> {
        if (path.size < 2) return emptyList()

        val nodeMap = TerminalDGraph.nodes.associateBy { it.id }
        val steps = mutableListOf<NavigationStep>()

        path.zipWithNext().forEachIndexed { index, (fromId, toId) ->
            val from = nodeMap[fromId] ?: return@forEachIndexed
            val to = nodeMap[toId] ?: return@forEachIndexed

            val dx = to.x - from.x
            val dy = to.y - from.y
            val distMeters = (sqrt((dx * dx + dy * dy).toDouble()) * 400).toFloat()
            val isLastStep = (index == path.size - 2)

            val (instruction, icon) = when {
                isLastStep ->
                    "Arrive at $amenityName" to DirectionIcon.ARRIVE
                toId.startsWith("SKY_") ->
                    "Continue through Skylink station" to DirectionIcon.STRAIGHT
                toId.startsWith("SEC_") ->
                    "Pass through security checkpoint" to DirectionIcon.STRAIGHT
                toId.startsWith("COR_") ->
                    "Continue along the main corridor" to DirectionIcon.STRAIGHT
                toId.startsWith("REST_") || toId.startsWith("FAM_") || toId.startsWith("LAC_") ->
                    "Turn toward $amenityName" to DirectionIcon.SLIGHT_RIGHT
                abs(dx) >= abs(dy) && dx > 0f ->
                    "Continue east past Gate $toId" to DirectionIcon.STRAIGHT
                abs(dx) >= abs(dy) && dx < 0f ->
                    "Continue west past Gate $toId" to DirectionIcon.STRAIGHT
                dy > 0f ->
                    "Head toward Gate $toId" to DirectionIcon.SLIGHT_RIGHT
                else ->
                    "Head toward Gate $toId" to DirectionIcon.SLIGHT_LEFT
            }

            steps.add(
                NavigationStep(
                    stepNumber = index + 1,
                    instruction = instruction,
                    directionIcon = icon,
                    distanceMeters = distMeters,
                    floorTransition = null
                )
            )
        }

        return steps
    }
}
