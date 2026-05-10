package com.smartamenities

import com.smartamenities.data.model.AdminSimulationState
import com.smartamenities.data.model.SimulationConfig
import com.smartamenities.data.model.SimulationPreset
import com.smartamenities.data.repository.AmenityRepository
import com.smartamenities.viewmodel.AmenityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class AdminSimulationViewModelTest {

    private lateinit var repository: AmenityRepository
    private lateinit var viewModel: AmenityViewModel

    @Before
    fun setup() {
        repository = mock()
        // Mock the required repository flows to avoid NPEs during init
        whenever(repository.userNodeUpdates).thenReturn(MutableStateFlow("COR_C"))
        whenever(repository.observeAdminSimulation()).thenReturn(MutableStateFlow(AdminSimulationState()))

        viewModel = AmenityViewModel(repository)
    }

    @Test
    fun `updateSimulationConfig sets success message on successful update`() = runTest {
        // Arrange
        val config = SimulationConfig(isSystemOpen = false)

        // Act
        viewModel.updateSimulationConfig(config)

        // Assert [cite: 2467, 2470]
        assertEquals("Zone settings applied successfully", viewModel.adminOperationSuccess.value)
    }

    @Test
    fun `applySimulationPreset sets success message on successful application`() = runTest {
        // Act
        viewModel.applySimulationPreset(SimulationPreset.HIGH_TRAFFIC)

        // Assert [cite: 2468, 2469]
        assertEquals("'High Traffic' preset applied successfully", viewModel.adminOperationSuccess.value)
    }
}
