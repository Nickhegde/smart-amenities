package com.smartamenities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartamenities.data.model.Amenity
import com.smartamenities.navigation.Screen
import com.smartamenities.ui.screens.*
import com.smartamenities.ui.theme.SmartAmenitiesTheme
import com.smartamenities.viewmodel.AmenityViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — single-activity architecture.
 * Navigation is handled by Compose Navigation (NavHost).
 *
 * Screen flow (matches System Sequence Diagram):
 *
 *   AmenityListScreen
 *        │ tap amenity
 *        ▼
 *   AmenityDetailScreen
 *        │ tap Navigate
 *        ▼
 *   NavigationScreen  ──  stepComplete / reroute loop
 *        │ tap End
 *        ▼
 *   AmenityListScreen
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartAmenitiesTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // Shared ViewModel instance across screens so amenity state is preserved
    val amenityViewModel: AmenityViewModel = hiltViewModel()

    // Holds the amenity selected by the user — passed to NavigationScreen
    var selectedAmenity by remember { mutableStateOf<Amenity?>(null) }
    val preferences by amenityViewModel.preferences.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {

        // ── Screen 0: Home / terminal selection ──────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onTerminalSelected = { terminal ->
                    // Only Terminal D is active; navigate to amenity list
                    if (terminal == "D") navController.navigate(Screen.AmenityList.route)
                }
            )
        }

        // ── Screen 1: Amenity list / map ─────────────────────────────────────
        composable(Screen.AmenityList.route) {
            AmenityListScreen(
                viewModel = amenityViewModel,
                onAmenitySelected = { amenity ->
                    selectedAmenity = amenity
                    navController.navigate(Screen.AmenityDetail.createRoute(amenity.id))
                },
                onOpenPreferences = {
                    navController.navigate(Screen.Preferences.route)
                }
            )
        }

        // ── Screen 2: Amenity detail ─────────────────────────────────────────
        composable(
            route = Screen.AmenityDetail.route,
            arguments = listOf(navArgument("amenityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val amenityId = backStackEntry.arguments?.getString("amenityId") ?: return@composable
            AmenityDetailScreen(
                amenityId = amenityId,
                viewModel = amenityViewModel,
                onNavigate = { amenity ->
                    selectedAmenity = amenity
                    navController.navigate(Screen.Navigation.createRoute(amenity.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Screen 3: Turn-by-turn navigation ───────────────────────────────
        composable(
            route = Screen.Navigation.route,
            arguments = listOf(navArgument("amenityId") { type = NavType.StringType })
        ) {
            val amenity = selectedAmenity ?: run {
                navController.popBackStack()
                return@composable
            }
            NavigationScreen(
                amenity = amenity,
                preferences = preferences,
                onEndNavigation = {
                    // FR 2.6: End navigation → return to map
                    navController.popBackStack(Screen.AmenityList.route, inclusive = false)
                }
            )
        }

        // ── Screen 4: Preferences ────────────────────────────────────────────
        composable(Screen.Preferences.route) {
            PreferencesScreen(
                viewModel = amenityViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
