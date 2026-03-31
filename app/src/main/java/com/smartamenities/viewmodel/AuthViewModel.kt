package com.smartamenities.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartamenities.data.local.UserDataStore
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.User
import com.smartamenities.data.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── Auth UI state ─────────────────────────────────────────────────────────────

sealed class AuthUiState {
    data object Idle    : AuthUiState()
    data object Loading : AuthUiState()
    data class  Success(val user: User)    : AuthUiState()
    data class  Error  (val message: String) : AuthUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val store: UserDataStore
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
            val hash = store.hashPassword(password)
            val user = store.authenticate(email.trim(), hash)
            if (user != null) {
                store.saveSession(user)
                _currentUser.value = user
                _state.value = AuthUiState.Success(user)
            } else {
                _state.value = AuthUiState.Error("Incorrect email or password.")
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
            val user = User(
                firstName          = firstName.trim(),
                lastName           = lastName.trim(),
                email              = email.trim().lowercase(),
                passwordHash       = store.hashPassword(password),
                phone              = phone.trim(),
                accessibilityPrefs = accessibilityPrefs
            )
            store.createUser(user).fold(
                onSuccess = { created ->
                    store.saveSession(created)
                    _currentUser.value = created
                    _state.value = AuthUiState.Success(created)
                },
                onFailure = { _state.value = AuthUiState.Error(it.message ?: "Sign-up failed.") }
            )
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
        store.clearSession()
        _currentUser.value = null
        _state.value = AuthUiState.Idle
    }

    fun clearError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }

    /** True when a valid session already exists — used to skip the auth screens. */
    fun hasActiveSession(): Boolean = store.hasActiveSession()

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
