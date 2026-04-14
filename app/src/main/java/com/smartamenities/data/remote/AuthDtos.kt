package com.smartamenities.data.remote

import com.google.gson.annotations.SerializedName

// ── POST /api/auth/signup — request ──────────────────────────────────────────

data class SignupRequestDto(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name")  val lastName: String,
    val email: String,
    val password: String
)

// ── POST /api/auth/login — request ───────────────────────────────────────────

data class LoginRequestDto(
    val email: String,
    val password: String
)

// ── Shared auth response ──────────────────────────────────────────────────────

data class AuthTokenResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type")   val tokenType: String
)
