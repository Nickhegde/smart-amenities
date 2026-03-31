package com.smartamenities.data.model

import java.util.UUID

/**
 * Represents a registered or guest user of SmartAmenities.
 *
 * Stored locally in SharedPreferences (serialised as JSON via Gson).
 * No PII is sent over the network in Iteration 1 — this is a fully
 * offline, on-device user store.
 *
 * Password is never stored in plaintext — only the SHA-256 hex digest.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val email: String,
    /** SHA-256 hex of the user's password — empty for guest accounts. */
    val passwordHash: String,
    /** Optional — for future push-notification support. */
    val phone: String = "",
    /** Accessibility flags carried forward from the sign-up form. */
    val accessibilityPrefs: UserPreferences = UserPreferences(),
    /** True for "Continue as Guest" sessions — no password, no stored profile. */
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (isGuest) "Guest" else "$firstName $lastName".trim()

    val initials: String
        get() = if (isGuest) "G"
                else "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}
