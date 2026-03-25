package com.smartamenities.navigation

/**
 * All navigation destinations in the app.
 * Using a sealed class keeps all route strings in one place —
 * no magic strings scattered across screens.
 */
sealed class Screen(val route: String) {

    // Terminal selection home screen
    data object Home : Screen("home")

    // The main map/list entry point
    data object AmenityList : Screen("amenity_list")

    // Full-screen amenity detail (shown after tapping a map pin or list item)
    data object AmenityDetail : Screen("amenity_detail/{amenityId}") {
        fun createRoute(amenityId: String) = "amenity_detail/$amenityId"
    }

    // Turn-by-turn navigation
    data object Navigation : Screen("navigation/{amenityId}") {
        fun createRoute(amenityId: String) = "navigation/$amenityId"
    }

    // Accessibility preferences form (FR 4.1, FR 4.2)
    data object Preferences : Screen("preferences")
}
