package com.smartamenities.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartamenities.data.local.UserDataStore
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.User
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.data.remote.ApiService
import com.smartamenities.data.remote.LoginRequestDto
import com.smartamenities.data.remote.SignupRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject

// ── Auth UI state ─────────────────────────────────────────────────────────────

sealed class AuthUiState {
    data object Idle    : AuthUiState()
    data object Loading : AuthUiState()
    data class  Success(val user: User)     : AuthUiState()
    data class  Error  (val message: String) : AuthUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val store: UserDataStore,
    private val apiService: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Restore a persisted session (registered user or guest) on app start
        store.restoreSession()?.let { user ->
            _currentUser.value = user
            _state.value = AuthUiState.Success(user)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState.Error("Please fill in all fields.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val response = apiService.login(LoginRequestDto(email.trim(), password))
                store.saveToken(response.accessToken)

                // Use locally cached profile if available (preserves name, prefs, etc.).
                // Fall back to a minimal user object when logging in from a new device.
                val user = store.findUserByEmail(email.trim())
                    ?: User(
                        firstName = "",
                        lastName  = "",
                        email     = email.trim().lowercase(),
                        passwordHash = ""
                    )
                store.saveSession(user)
                _currentUser.value = user
                _state.value = AuthUiState.Success(user)
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(authErrorMessage(e))
            }
        }
    }

    fun signUp(
        firstName:       String,
        lastName:        String,
        email:           String,
        phone:           String,
        password:        String,
        confirmPassword: String,
        accessibilityPrefs: UserPreferences
    ) {
        val err = validate(firstName, lastName, email, password, confirmPassword)
        if (err != null) { _state.value = AuthUiState.Error(err); return }

        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val response = apiService.signup(
                    SignupRequestDto(
                        firstName = firstName.trim(),
                        lastName  = lastName.trim(),
                        email     = email.trim(),
                        password  = password
                    )
                )
                store.saveToken(response.accessToken)

                val user = User(
                    firstName          = firstName.trim(),
                    lastName           = lastName.trim(),
                    email              = email.trim().lowercase(),
                    passwordHash       = "",
                    phone              = phone.trim(),
                    accessibilityPrefs = accessibilityPrefs
                )
                // Cache the profile locally so login from this device can restore it.
                store.createUser(user)
                store.saveSession(user)
                _currentUser.value = user
                _state.value = AuthUiState.Success(user)
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(authErrorMessage(e))
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val guest = User(
                id           = "guest_${UUID.randomUUID()}",
                firstName    = "Guest",
                lastName     = "",
                email        = "guest@local",
                passwordHash = "",
                isGuest      = true
            )
            store.saveSession(guest)
            _currentUser.value = guest
            _state.value = AuthUiState.Success(guest)
        }
    }

    fun logout() {
        store.clearSession()   // also clears the JWT token
        _currentUser.value = null
        _state.value = AuthUiState.Idle
    }

    fun clearError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }

    /** True when a valid session already exists — used to skip the auth screens. */
    fun hasActiveSession(): Boolean = store.hasActiveSession()

    // ── Error mapping ─────────────────────────────────────────────────────────

    private fun authErrorMessage(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            400  -> "Email already registered."
            401  -> "Incorrect email or password."
            else -> "Something went wrong (${e.code()}). Please try again."
        }
        else -> "Unable to connect. Check your network connection."
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate(
        firstName: String, lastName: String,
        email: String, password: String, confirmPassword: String
    ): String? = when {
        firstName.isBlank() || lastName.isBlank() ->
            "Please enter your first and last name."
        email.isBlank() ||
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
            "Please enter a valid email address."
        password.length < 6 ->
            "Password must be at least 6 characters."
        password != confirmPassword ->
            "Passwords do not match."
        else -> null
    }
}
