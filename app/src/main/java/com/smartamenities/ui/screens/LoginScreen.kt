package com.smartamenities.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.smartamenities.viewmodel.AuthUiState
import com.smartamenities.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onBack:        () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val state by authViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw   by remember { mutableStateOf(false) }

    // Navigate as soon as login succeeds
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onLoginSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { authViewModel.clearError(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text  = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Sign in to restore your accessibility preferences and history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(8.dp))

            // Error banner
            if (state is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text  = (state as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Email ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it; authViewModel.clearError() },
                label         = { Text("Email") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // ── Password ──────────────────────────────────────────────────────
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it; authViewModel.clearError() },
                label         = { Text("Password") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                visualTransformation = if (showPw) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(
                            if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPw) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        authViewModel.login(email, password)
                    }
                )
            )

            Spacer(Modifier.height(8.dp))

            // ── Sign in button ────────────────────────────────────────────────
            Button(
                onClick  = { focusManager.clearFocus(); authViewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled  = state !is AuthUiState.Loading
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
