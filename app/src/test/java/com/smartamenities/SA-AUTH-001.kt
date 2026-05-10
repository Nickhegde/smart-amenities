package com.smartamenities

import com.smartamenities.viewmodel.AuthViewModel
import com.smartamenities.viewmodel.AuthUiState
import com.smartamenities.data.local.UserDataStore
import com.smartamenities.data.remote.ApiService
import com.smartamenities.data.model.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class AuthViewModelTest {

    private lateinit var store: UserDataStore
    private lateinit var apiService: ApiService
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        store = mock()
        apiService = mock()
        // Assume no active session on startup for these tests
        whenever(store.restoreSession()).thenReturn(null)
        viewModel = AuthViewModel(store, apiService)
    }

    @Test
    fun `login with empty fields returns error state`() = runTest {
        // Act
        viewModel.login(email = "", password = "")

        // Assert [cite: 2365, 2366]
        val state = viewModel.state.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Please fill in all fields.", (state as AuthUiState.Error).message)
    }

    @Test
    fun `signUp with mismatched passwords returns error`() = runTest {
        // Act
        viewModel.signUp(
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            phone = "1234567890",
            password = "securePassword123",
            confirmPassword = "differentPassword",
            accessibilityPrefs = UserPreferences()
        )

        // Assert [cite: 2364, 2372]
        val state = viewModel.state.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Passwords do not match.", (state as AuthUiState.Error).message)
    }

    @Test
    fun `signUp with short password returns error`() = runTest {
        // Act
        viewModel.signUp(
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            phone = "1234567890",
            password = "123", // Less than 6 characters
            confirmPassword = "123",
            accessibilityPrefs = UserPreferences()
        )

        // Assert 
        val state = viewModel.state.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Password must be at least 6 characters.", (state as AuthUiState.Error).message)
    }

    @Test
    fun `continueAsGuest creates guest session successfully`() = runTest {
        // Act
        viewModel.continueAsGuest()

        // Assert 
        val state = viewModel.state.value
        assertTrue(state is AuthUiState.Success)
        val user = (state as AuthUiState.Success).user
        assertTrue(user.isGuest)
        assertEquals("Guest", user.firstName)
    }

    @Test
    fun `logout clears session and resets state`() {
        // Act
        viewModel.logout()

        // Assert [cite: 2369]
        val state = viewModel.state.value
        assertEquals(AuthUiState.Idle, state)
        assertEquals(null, viewModel.currentUser.value)
    }
}