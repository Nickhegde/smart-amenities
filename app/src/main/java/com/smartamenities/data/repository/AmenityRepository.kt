package com.smartamenities.data.repository

import com.smartamenities.data.local.MockAmenityDataSource
import com.smartamenities.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

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
}

// ─────────────────────────────────────────────────────────────────────────────
// Mock implementation — backed by MockAmenityDataSource
// Replace with RemoteAmenityRepository (Retrofit) when backend is ready
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MockAmenityRepository @Inject constructor() : AmenityRepository {

    // Simulated 150 ms network delay so loading states are visible during dev
    private val fakeLatencyMs = 150L

    override fun getAmenities(preferences: UserPreferences): Flow<List<Amenity>> = flow {
        delay(fakeLatencyMs)
        val filtered = MockAmenityDataSource.amenities
            .filter { amenity -> matchesPreferences(amenity, preferences) }
            .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
        emit(filtered)
    }

    override fun getAmenitiesByType(
        type: AmenityType,
        preferences: UserPreferences
    ): Flow<List<Amenity>> = flow {
        delay(fakeLatencyMs)
        val filtered = MockAmenityDataSource.amenities
            .filter { it.type == type }
            .filter { amenity -> matchesPreferences(amenity, preferences) }
            .sortedBy { it.estimatedWalkMinutes + it.crowdLevel.waitEstimateMinutes }
        emit(filtered)
    }

    override suspend fun getAmenityById(id: String): Amenity? {
        delay(fakeLatencyMs)
        return MockAmenityDataSource.amenities.find { it.id == id }
    }

    override suspend fun getRoute(amenity: Amenity, preferences: UserPreferences): Route {
        delay(fakeLatencyMs * 2) // Route calculation takes slightly longer
        val steps = MockAmenityDataSource.getMockRoute(amenity)
        return Route(
            amenityId = amenity.id,
            steps = steps,
            totalWalkMinutes = amenity.estimatedWalkMinutes,
            totalWaitMinutes = amenity.crowdLevel.waitEstimateMinutes,
            isStepFreeRoute = amenity.isStepFreeRoute,
            computedAtTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun reportAmenityStatus(amenityId: String, status: AmenityStatus) {
        delay(fakeLatencyMs)
        // In the real implementation this POSTs to /api/amenities/{id}/report
        // For now it's a no-op — the mock data doesn't mutate
    }

    // ── Preference filtering logic ────────────────────────────────────────────

    private fun matchesPreferences(amenity: Amenity, prefs: UserPreferences): Boolean {
        if (prefs.requiresWheelchairAccess && !amenity.isWheelchairAccessible) return false
        if (prefs.requiresStepFreeRoute && !amenity.isStepFreeRoute) return false
        if (prefs.preferFamilyRestroom && amenity.type == AmenityType.RESTROOM) return false
        return true
    }
}
