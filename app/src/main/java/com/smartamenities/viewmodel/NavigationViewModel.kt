package com.smartamenities.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartamenities.data.model.*
import com.smartamenities.data.repository.AmenityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val repository: AmenityRepository
) : ViewModel() {

    private val _navState = MutableStateFlow<NavigationUiState>(NavigationUiState.Idle)
    val navState: StateFlow<NavigationUiState> = _navState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private var activeRoute: Route? = null
    private var targetAmenity: Amenity? = null
    private var activePreferences: UserPreferences = UserPreferences()
    private var amenitiesJob: Job? = null
    private var statusMonitorJob: Job? = null

    companion object {
        private const val STATUS_POLL_MS = 5_000L
    }

    fun beginNavigation(amenity: Amenity, preferences: UserPreferences) {
        statusMonitorJob?.cancel()
        targetAmenity = amenity
        activePreferences = preferences
        viewModelScope.launch {
            _navState.value = NavigationUiState.Loading

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
            startStatusMonitoring(amenity, preferences)
        }
    }

    fun completeStep() {
        val route = activeRoute ?: return
        val nextIndex = _currentStepIndex.value + 1

        if (nextIndex >= route.steps.size) {
            statusMonitorJob?.cancel()
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

    fun navigateToAlternative(alternative: Amenity) {
        beginNavigation(alternative, activePreferences)
    }

    fun endNavigation() {
        statusMonitorJob?.cancel()
        amenitiesJob?.cancel()
        activeRoute = null
        targetAmenity = null
        _currentStepIndex.value = 0
        _navState.value = NavigationUiState.Idle
    }

    // ── Status monitoring ─────────────────────────────────────────────────────

    private fun startStatusMonitoring(amenity: Amenity, preferences: UserPreferences) {
        statusMonitorJob?.cancel()
        statusMonitorJob = viewModelScope.launch {
            while (true) {
                delay(STATUS_POLL_MS)
                if (_navState.value !is NavigationUiState.Navigating) break

                // Query the admin endpoint directly — it reads straight from the DB,
                // bypassing the recommendation cache that AmenityViewModel also writes to.
                // null means a transient network error; skip and retry next cycle.
                val freshStatus = repository.getFreshAmenityStatus(amenity.id) ?: continue

                val isClosed = freshStatus == AmenityStatus.CLOSED ||
                               freshStatus == AmenityStatus.OUT_OF_SERVICE

                if (isClosed) {
                    // Move user node to current step so alternatives rank from the
                    // passenger's actual position mid-navigation.
                    activeRoute?.routeNodeIds?.getOrNull(_currentStepIndex.value)
                        ?.let { node -> repository.setUserNode(node) }
                    repository.clearCaches()

                    val alternatives = try {
                        repository.getAmenitiesByType(amenity.type, preferences).first()
                            .filter { it.id != amenity.id && it.status == AmenityStatus.OPEN }
                    } catch (_: Exception) { emptyList() }

                    _navState.value = NavigationUiState.ClosureRerouting(
                        closedAmenity = amenity.copy(status = freshStatus),
                        alternative = alternatives.firstOrNull()
                    )
                    break
                }
            }
        }
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

    /** Target amenity closed mid-navigation; [alternative] is the next nearest open one. */
    data class ClosureRerouting(
        val closedAmenity: Amenity,
        val alternative: Amenity?
    ) : NavigationUiState()
}
