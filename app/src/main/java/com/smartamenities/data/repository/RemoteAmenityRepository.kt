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
import com.smartamenities.data.model.SimulationPreset
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.data.remote.ApiService
import com.smartamenities.data.remote.RouteOptionDto
import com.smartamenities.data.remote.RouteRecommendRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
        val type = effectiveAmenityType(preferences)

        // Re-filter in memory if type hasn't changed — avoids API call for
        // wheelchair / step-free toggles which are purely client-side.
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
            Log.e(TAG, "getAmenities failed, trying Room cache: ${e.message}")
            val cached = dao.getByType(type.name)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Serving ${cached.size} Room-cached amenities for ${type.name}")
                emit(cached.map { it.toDomain() }.filter { matchesPreferences(it, preferences) })
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
                computedAtTimestamp = System.currentTimeMillis()
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
                        computedAtTimestamp = System.currentTimeMillis()
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

    // ── Delegated to mock (admin simulation stays local) ────────────────────

    override suspend fun getAmenityById(id: String): Amenity? = mock.getAmenityById(id)

    override suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus) =
        mock.reportAmenityStatus(amenityId, status)

    override fun observeAdminSimulation(): Flow<AdminSimulationState> =
        mock.observeAdminSimulation()

    override suspend fun updateSimulationConfig(config: SimulationConfig) =
        mock.updateSimulationConfig(config)

    override suspend fun applySimulationPreset(preset: SimulationPreset) =
        mock.applySimulationPreset(preset)

    override suspend fun updateAmenityOverride(
        amenityId: String,
        status: AmenityStatus?,
        crowdLevel: CrowdLevel?
    ) = mock.updateAmenityOverride(amenityId, status, crowdLevel)

    override suspend fun clearAmenityOverride(amenityId: String) =
        mock.clearAmenityOverride(amenityId)

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
