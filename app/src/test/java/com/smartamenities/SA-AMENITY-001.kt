package com.smartamenities

import com.smartamenities.viewmodel.AmenityViewModel
import com.smartamenities.viewmodel.AmenityUiState
import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.data.model.SimulationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class AmenityViewModelTest {

    private lateinit var repository: AmenityRepository
    private lateinit var viewModel: AmenityViewModel

    @Before
    fun setup() {
        repository = mock()
        // Mock the required repository flows
        whenever(repository.userNodeUpdates).thenReturn(MutableStateFlow("COR_C"))
        whenever(repository.observeAdminSimulation()).thenReturn(MutableStateFlow(mock()))

        viewModel = AmenityViewModel(repository)
    }

    @Test
    fun `selectAmenityType updates selectedType state`() {
        // Act
        viewModel.selectAmenityType(AmenityType.LACTATION_ROOM)

        // Assert
        assertEquals(AmenityType.LACTATION_ROOM, viewModel.selectedType.value)
    }

    @Test
    fun `selectAmenityType with null clears type filter`() {
        // Arrange
        viewModel.selectAmenityType(AmenityType.FAMILY_RESTROOM)

        // Act
        viewModel.selectAmenityType(null)

        // Assert
        assertEquals(null, viewModel.selectedType.value)
    }

    @Test
    fun `updatePreferences updates state and triggers reload`() {
        // Arrange
        val newPrefs = UserPreferences(
            requiresWheelchairAccess = true,
            requiresStepFreeRoute = true
        )

        // Act
        viewModel.updatePreferences(newPrefs)

        // Assert [cite: 2432]
        assertEquals(true, viewModel.preferences.value.requiresWheelchairAccess)
        assertEquals(true, viewModel.preferences.value.requiresStepFreeRoute)
    }

    @Test
    fun `updateUserNode updates node state`() {
        // Act
        viewModel.updateUserNode("GATE_D30")

        // Assert [cite: 2394, 2397]
        assertEquals("GATE_D30", viewModel.userNode.value)
    }
}