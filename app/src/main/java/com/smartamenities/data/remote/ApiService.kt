package com.smartamenities.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // ── Route recommendation ──────────────────────────────────────────────────

    @POST("api/route/recommend")
    suspend fun getRouteRecommendations(
        @Body request: RouteRecommendRequestDto
    ): RouteRecommendResponseDto

    // ── Admin — amenity reads & writes ────────────────────────────────────────

    @GET("api/admin/amenities")
    suspend fun getAdminAmenities(): List<AmenityAdminResponseDto>

    @PATCH("api/admin/amenity/{amenity_id}")
    suspend fun updateAmenity(
        @Path("amenity_id") amenityId: String,
        @Body request: AmenityOverrideRequestDto
    ): AmenityAdminResponseDto

    @DELETE("api/admin/amenity/{amenity_id}/override")
    suspend fun resetAmenity(
        @Path("amenity_id") amenityId: String
    ): AmenityAdminResponseDto

    // ── Admin — zone bulk control ─────────────────────────────────────────────

    @PATCH("api/admin/zone")
    suspend fun updateZone(
        @Body request: ZoneControlRequestDto
    ): AdminActionResponseDto

    // ── Admin — preset scenarios ──────────────────────────────────────────────

    @GET("api/admin/scenarios")
    suspend fun getScenarios(): List<ScenarioResponseDto>

    @POST("api/admin/scenario/apply")
    suspend fun applyScenario(
        @Body request: ScenarioApplyRequestDto
    ): AdminActionResponseDto
}
