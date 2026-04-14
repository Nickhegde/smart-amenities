package com.smartamenities.data.remote

import com.google.gson.annotations.SerializedName

data class RouteRecommendRequestDto(
    @SerializedName("user_node") val userNode: String,
    @SerializedName("amenity_type") val amenityType: String,
    @SerializedName("wheelchair_required") val wheelchairRequired: Boolean
)

data class RouteOptionDto(
    @SerializedName("amenity_id") val amenityId: String,
    val path: List<String>,
    @SerializedName("walk_seconds") val walkSeconds: Int,
    @SerializedName("wait_seconds") val waitSeconds: Int,
    @SerializedName("total_seconds") val totalSeconds: Int,
    @SerializedName("crowd_level") val crowdLevel: String,
    val status: String
)

data class RouteRecommendResponseDto(
    val recommendations: List<RouteOptionDto>
)
