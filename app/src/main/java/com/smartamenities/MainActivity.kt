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
import com.smartamenities.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — single-activity architecture.
 *
 * Full screen flow:
 *
 *   AuthScreen  (first launch / logged out)
 *     ├─ Sign In  → LoginScreen  ──┐
 *     ├─ Create Account → SignUpScreen ─┤
 *     └─ Continue as Guest ───────────┘
 *                                   │ auth success
 *                                   ▼
 *   HomeScreen  (terminal selection)
 *        │ select Terminal D
 *        ▼
 *   MapScreen  (Map tab + List tab)
 *        │ tap pin or list item
 *        ▼
 *   AmenityDetailScreen
 *        │ tap Navigate
 *        ▼
 *   NavigationScreen
 *        │ End / Done
 *        ▼
 *   MapScreen  ← back here
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
    val navController    = rememberNavController()
    val amenityViewModel: AmenityViewModel = hiltViewModel()
    val authViewModel:    AuthViewModel    = hiltViewModel()

    // Decide start destination synchronously (reads SharedPreferences, no coroutine needed).
    // If a session already exists we skip the auth screens entirely.
    val startDestination = remember {
        if (authViewModel.hasActiveSession()) Screen.Home.route else Screen.Auth.route
    }

    var selectedAmenity by remember { mutableStateOf<Amenity?>(null) }
    val preferences  by amenityViewModel.preferences.collectAsState()
    val currentUser  by authViewModel.currentUser.collectAsState()

    // Helper: navigate to the Auth screen and clear the entire back-stack.
    // Used by sign-out from any screen.
    fun onSignOut() {
        authViewModel.logout()
        navController.navigate(Screen.Auth.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    // Helper: navigate to Home after auth, wiping the auth back-stack so the user
    // cannot press Back to return to the login / sign-up screens.
    fun onAuthSuccess() {
        // Apply the user's stored accessibility prefs to the amenity ViewModel
        authViewModel.currentUser.value?.accessibilityPrefs?.let { prefs ->
            amenityViewModel.updatePreferences(prefs)
        }
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Auth: landing ─────────────────────────────────────────────────────
        composable(Screen.Auth.route) {
            AuthScreen(
                authViewModel  = authViewModel,
                onLoginClick   = { navController.navigate(Screen.Login.route) },
                onSignUpClick  = { navController.navigate(Screen.SignUp.route) },
                onGuestSuccess = { onAuthSuccess() }
            )
        }

        // ── Auth: sign in ─────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel  = authViewModel,
                onBack         = { navController.popBackStack() },
                onLoginSuccess = { onAuthSuccess() }
            )
        }

        // ── Auth: create account ──────────────────────────────────────────────
        composable(Screen.SignUp.route) {
            SignUpScreen(
                authViewModel   = authViewModel,
                onBack          = { navController.popBackStack() },
                onSignUpSuccess = { onAuthSuccess() }
            )
        }

        // ── Home: terminal selection ──────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onTerminalSelected = { terminal ->
                    if (terminal == "D") navController.navigate(Screen.Map.route)
                },
                currentUser      = currentUser,
                onSignOut        = { onSignOut() },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                onOpenSettings   = { navController.navigate(Screen.Preferences.route) }
            )
        }

        // ── Terminal D: map + list (combined) ─────────────────────────────────
        composable(Screen.Map.route) {
            MapScreen(
                viewModel = amenityViewModel,
                onAmenitySelected = { amenity ->
                    selectedAmenity = amenity
                    amenityViewModel.selectAmenity(amenity)
                    navController.navigate(Screen.AmenityDetail.createRoute(amenity.id))
                },
                onOpenPreferences = { navController.navigate(Screen.Preferences.route) },
                currentUser      = currentUser,
                onSignOut        = { onSignOut() },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
            )
        }

        // ── Amenity detail ────────────────────────────────────────────────────
        composable(
            route     = Screen.AmenityDetail.route,
            arguments = listOf(navArgument("amenityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val amenityId = backStackEntry.arguments?.getString("amenityId") ?: return@composable
            AmenityDetailScreen(
                amenityId  = amenityId,
                viewModel  = amenityViewModel,
                onNavigate = { amenity ->
                    selectedAmenity = amenity
                    navController.navigate(Screen.Navigation.createRoute(amenity.id))
                },
                onBack           = { navController.popBackStack() },
                currentUser      = currentUser,
                onSignOut        = { onSignOut() },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                onOpenSettings   = { navController.navigate(Screen.Preferences.route) }
            )
        }

        // ── Turn-by-turn navigation ───────────────────────────────────────────
        composable(
            route     = Screen.Navigation.route,
            arguments = listOf(navArgument("amenityId") { type = NavType.StringType })
        ) {
            val amenity = selectedAmenity ?: run {
                navController.popBackStack(); return@composable
            }
            NavigationScreen(
                amenity     = amenity,
                preferences = preferences,
                onEndNavigation = {
                    navController.popBackStack(Screen.Map.route, inclusive = false)
                }
            )
        }

        // ── Accessibility preferences & account ───────────────────────────────
        composable(Screen.Preferences.route) {
            PreferencesScreen(
                viewModel     = amenityViewModel,
                authViewModel = authViewModel,
                onBack        = { navController.popBackStack() },
                onSignOut     = { onSignOut() }
            )
        }
    }
}
