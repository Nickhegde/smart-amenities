package com.smartamenities.data.remote

import com.google.gson.annotations.SerializedName

// ── GET /api/admin/amenities — response ──────────────────────────────────────

data class AmenityAdminResponseDto(
    val id: String,
    val name: String,
    val type: String,
    val floor: Int,
    val status: String,
    @SerializedName("crowd_level")            val crowdLevel: String,
    @SerializedName("avg_usage_minutes")      val avgUsageMinutes: Int,
    @SerializedName("is_wheelchair_accessible") val isWheelchairAccessible: Boolean,
    @SerializedName("is_step_free_route")     val isStepFreeRoute: Boolean,
    @SerializedName("is_family_restroom")     val isFamilyRestroom: Boolean,
    @SerializedName("is_gender_neutral")      val isGenderNeutral: Boolean,
    @SerializedName("gate_proximity")         val gateProximity: String
)

// ── PATCH /api/admin/amenity/{id} — request ──────────────────────────────────

data class AmenityOverrideRequestDto(
    val status: String? = null,
    @SerializedName("crowd_level") val crowdLevel: String? = null
)

// ── PATCH /api/admin/zone — request ──────────────────────────────────────────

data class ZoneControlRequestDto(
    val zone: String,
    @SerializedName("crowd_level")       val crowdLevel: String? = null,
    @SerializedName("avg_usage_minutes") val avgUsageMinutes: Int? = null,
    @SerializedName("is_open")           val isOpen: Boolean? = null
)

// ── POST /api/admin/scenario/apply — request ─────────────────────────────────

data class ScenarioApplyRequestDto(
    @SerializedName("scenario_id") val scenarioId: Int
)

// ── GET /api/admin/scenarios — response ──────────────────────────────────────

data class ScenarioResponseDto(
    val id: Int,
    val name: String,
    val description: String?
)

// ── Generic admin write response ─────────────────────────────────────────────

data class AdminActionResponseDto(
    val success: Boolean,
    val message: String,
    @SerializedName("updated_count") val updatedCount: Int = 0
)
