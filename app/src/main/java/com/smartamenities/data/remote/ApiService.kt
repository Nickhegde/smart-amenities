package com.smartamenities.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/route/recommend")
    suspend fun getRouteRecommendations(
        @Body request: RouteRecommendRequestDto
    ): RouteRecommendResponseDto
}
