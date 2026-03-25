package com.smartamenities.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartamenities.data.model.*
import com.smartamenities.data.repository.AmenityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NavigationViewModel — owns the UI state for NavigationScreen
 *
 * Responsibilities (from HLA doc):
 *  - observeRoute()      : provides turn-by-turn steps to the UI
 *  - triggerReroute()    : requests a new route when disruption detected
 *  - observeClosure()    : watches for corridor/amenity closures
 *
 * The navigation loop (loop [whileNavigating] in the Sequence Diagram) is
 * modelled by the currentStepIndex advancing through the Route's steps list.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val repository: AmenityRepository
) : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _navState = MutableStateFlow<NavigationUiState>(NavigationUiState.Idle)
    val navState: StateFlow<NavigationUiState> = _navState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // The route currently being navigated
    private var activeRoute: Route? = null
    private var targetAmenity: Amenity? = null

    // ── Navigation lifecycle ──────────────────────────────────────────────────

    /**
     * Called when the passenger taps "Navigate" on the AmenityDetailScreen.
     * Corresponds to beginNavigation(selectedAmenity, accessibilityStatus) in Sequence Diagram.
     */
    fun beginNavigation(amenity: Amenity, preferences: UserPreferences) {
        targetAmenity = amenity
        viewModelScope.launch {
            _navState.value = NavigationUiState.Loading

            // Check amenity is still available before starting (FR 2.4)
            if (amenity.status == AmenityStatus.CLOSED ||
                amenity.status == AmenityStatus.OUT_OF_SERVICE) {
                _navState.value = NavigationUiState.AmenityUnavailable(amenity)
                return@launch
            }

            val route = repository.getRoute(amenity, preferences)
            activeRoute = route
            _currentStepIndex.value = 0
            _navState.value = NavigationUiState.Navigating(
                route = route,
                amenity = amenity,
                currentStep = route.steps.first()
            )
        }
    }

    /**
     * Called when the user confirms a step is complete.
     * Corresponds to stepComplete(step) in the Sequence Diagram.
     */
    fun completeStep() {
        val route = activeRoute ?: return
        val nextIndex = _currentStepIndex.value + 1

        if (nextIndex >= route.steps.size) {
            // All steps done — navigation complete (FR 2.8)
            _navState.value = NavigationUiState.Arrived(targetAmenity!!)
        } else {
            _currentStepIndex.value = nextIndex
            _navState.value = NavigationUiState.Navigating(
                route = route,
                amenity = targetAmenity!!,
                currentStep = route.steps[nextIndex]
            )
        }
    }

    /**
     * Called when a route disruption is detected — triggers reroute prompt (FR 2.5).
     * In Iteration 1 this goes straight to rerouting; FR 2.5 user confirmation
     * dialog is added in Iteration 2.
     */
    fun triggerReroute(preferences: UserPreferences) {
        val amenity = targetAmenity ?: return
        viewModelScope.launch {
            _navState.value = NavigationUiState.Rerouting
            val newRoute = repository.getRoute(amenity, preferences)
            activeRoute = newRoute
            _currentStepIndex.value = 0
            _navState.value = NavigationUiState.Navigating(
                route = newRoute,
                amenity = amenity,
                currentStep = newRoute.steps.first()
            )
        }
    }

    /** Called when the user taps the End Navigation button (FR 2.6) */
    fun endNavigation() {
        activeRoute = null
        targetAmenity = null
        _currentStepIndex.value = 0
        _navState.value = NavigationUiState.Idle
    }
}

// ── UI state sealed class ─────────────────────────────────────────────────────

sealed class NavigationUiState {
    data object Idle : NavigationUiState()
    data object Loading : NavigationUiState()
    data object Rerouting : NavigationUiState()

    data class Navigating(
        val route: Route,
        val amenity: Amenity,
        val currentStep: NavigationStep
    ) : NavigationUiState()

    data class AmenityUnavailable(val amenity: Amenity) : NavigationUiState()
    data class Arrived(val amenity: Amenity) : NavigationUiState()
}
