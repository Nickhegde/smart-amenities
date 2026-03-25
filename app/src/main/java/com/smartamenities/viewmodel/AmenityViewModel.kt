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
 * AmenityViewModel — owns the UI state for AmenityListScreen and MapScreen
 *
 * Responsibilities (from HLA doc):
 *  - observeAmenities()   : keeps the amenity list up to date
 *  - observeCrowdLevel()  : reflects real-time crowd changes
 *  - triggerRefresh()     : forces a manual data reload
 *
 * The ViewModel never imports Android UI classes — it only works with
 * plain Kotlin data. The Composable screens observe the StateFlows below.
 */
@HiltViewModel
class AmenityViewModel @Inject constructor(
    private val repository: AmenityRepository
) : ViewModel() {

    // ── User preferences (stored in memory; Room persistence comes in Iteration 2) ──

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    // ── Active type filter (null = show all types) ────────────────────────────

    private val _selectedType = MutableStateFlow<AmenityType?>(null)
    val selectedType: StateFlow<AmenityType?> = _selectedType.asStateFlow()

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<AmenityUiState>(AmenityUiState.Loading)
    val uiState: StateFlow<AmenityUiState> = _uiState.asStateFlow()

    // Selected amenity for the detail panel (FR 1.1.2)
    private val _selectedAmenity = MutableStateFlow<Amenity?>(null)
    val selectedAmenity: StateFlow<Amenity?> = _selectedAmenity.asStateFlow()

    init {
        loadAmenities()
    }

    // ── Public API called by the UI ───────────────────────────────────────────

    fun selectAmenityType(type: AmenityType?) {
        _selectedType.value = type
        loadAmenities()
    }

    fun selectAmenity(amenity: Amenity) {
        _selectedAmenity.value = amenity
    }

    fun clearSelectedAmenity() {
        _selectedAmenity.value = null
    }

    fun updatePreferences(prefs: UserPreferences) {
        _preferences.value = prefs
        loadAmenities()
    }

    fun triggerRefresh() {
        loadAmenities()
    }

    fun reportStatus(amenityId: String, status: AmenityStatus) {
        viewModelScope.launch {
            repository.reportAmenityStatus(amenityId, status)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadAmenities() {
        viewModelScope.launch {
            _uiState.value = AmenityUiState.Loading

            val flow = selectedType.value
                ?.let { repository.getAmenitiesByType(it, preferences.value) }
                ?: repository.getAmenities(preferences.value)

            flow
                .catch { e -> _uiState.value = AmenityUiState.Error(e.message ?: "Unknown error") }
                .collect { amenities ->
                    _uiState.value = if (amenities.isEmpty()) {
                        AmenityUiState.Empty
                    } else {
                        AmenityUiState.Success(amenities)
                    }
                }
        }
    }
}

// ── UI state sealed class — exhaustive when-expression in the UI ──────────────

sealed class AmenityUiState {
    data object Loading : AmenityUiState()
    data object Empty : AmenityUiState()
    data class Success(val amenities: List<Amenity>) : AmenityUiState()
    data class Error(val message: String) : AmenityUiState()
}
