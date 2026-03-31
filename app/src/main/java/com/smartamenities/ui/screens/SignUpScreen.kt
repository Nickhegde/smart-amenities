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
import com.smartamenities.data.model.AmenityType
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.viewmodel.AuthUiState
import com.smartamenities.viewmodel.AuthViewModel

/**
 * Sign-up form.
 *
 * Fields collected:
 *   Personal  : first name, last name
 *   Contact   : email, phone (optional)
 *   Security  : password, confirm password
 *   Accessibility : wheelchair, step-free, family restroom, gender-neutral,
 *                   preferred amenity type
 *
 * All accessibility fields are applied to the AmenityViewModel right after
 * sign-up so the map/list immediately reflect the user's needs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel:  AuthViewModel,
    onBack:         () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val state by authViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    // ── Form state ────────────────────────────────────────────────────────────
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var confirm   by remember { mutableStateOf("") }
    var showPw    by remember { mutableStateOf(false) }
    var showCf    by remember { mutableStateOf(false) }

    // Accessibility preferences
    var wheelchair   by remember { mutableStateOf(false) }
    var stepFree     by remember { mutableStateOf(false) }
    var familyRoom   by remember { mutableStateOf(false) }
    var genderNeutral by remember { mutableStateOf(false) }
    var preferredType by remember { mutableStateOf(AmenityType.RESTROOM) }
    var typeMenuOpen  by remember { mutableStateOf(false) }

    // Navigate as soon as sign-up succeeds
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onSignUpSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text  = "Your profile helps us personalise routes and amenity recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Error banner
            if (state is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text     = (state as AuthUiState.Error).message,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Section: Personal info ────────────────────────────────────────
            SectionHeader("Personal Information")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = firstName,
                    onValueChange = { firstName = it; authViewModel.clearError() },
                    label         = { Text("First name *") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Right)
                    })
                )
                OutlinedTextField(
                    value         = lastName,
                    onValueChange = { lastName = it; authViewModel.clearError() },
                    label         = { Text("Last name *") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    })
                )
            }

            // ── Section: Contact ──────────────────────────────────────────────
            SectionHeader("Contact")

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it; authViewModel.clearError() },
                label         = { Text("Email *") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            )

            OutlinedTextField(
                value         = phone,
                onValueChange = { phone = it },
                label         = { Text("Phone (optional)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                supportingText = { Text("For future gate-change or closure alerts") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            )

            // ── Section: Security ─────────────────────────────────────────────
            SectionHeader("Security")

            OutlinedTextField(
                value         = password,
                onValueChange = { password = it; authViewModel.clearError() },
                label         = { Text("Password *") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                visualTransformation = if (showPw) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(
                            if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                supportingText = { Text("Minimum 6 characters") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                })
            )

            OutlinedTextField(
                value         = confirm,
                onValueChange = { confirm = it; authViewModel.clearError() },
                label         = { Text("Confirm password *") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                visualTransformation = if (showCf) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { showCf = !showCf }) {
                        Icon(
                            if (showCf) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                isError = confirm.isNotBlank() && confirm != password,
                supportingText = if (confirm.isNotBlank() && confirm != password) {
                    { Text("Passwords do not match", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            // ── Section: Accessibility ────────────────────────────────────────
            SectionHeader("Accessibility Needs")
            Text(
                text  = "These settings personalise which amenities are shown first and how routes are planned for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            AccessibilityToggle(
                label    = "Wheelchair accessible routes only",
                subLabel = "Filters out amenities without wheelchair access",
                checked  = wheelchair,
                onCheckedChange = { wheelchair = it }
            )
            AccessibilityToggle(
                label    = "Step-free routes only",
                subLabel = "Avoids stairs and escalators",
                checked  = stepFree,
                onCheckedChange = { stepFree = it }
            )
            AccessibilityToggle(
                label    = "Prefer family restrooms",
                subLabel = "Ranks family rooms higher in results",
                checked  = familyRoom,
                onCheckedChange = { familyRoom = it }
            )
            AccessibilityToggle(
                label    = "Prefer gender-neutral restrooms",
                subLabel = "Ranks gender-neutral facilities higher",
                checked  = genderNeutral,
                onCheckedChange = { genderNeutral = it }
            )

            // Preferred amenity type dropdown
            Text(
                "Preferred amenity type",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            ExposedDropdownMenuBox(
                expanded         = typeMenuOpen,
                onExpandedChange = { typeMenuOpen = it }
            ) {
                OutlinedTextField(
                    value         = preferredType.displayName,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Default filter on map") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuOpen) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded         = typeMenuOpen,
                    onDismissRequest = { typeMenuOpen = false }
                ) {
                    AmenityType.values().forEach { type ->
                        DropdownMenuItem(
                            text    = { Text(type.displayName) },
                            onClick = { preferredType = type; typeMenuOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Submit button ─────────────────────────────────────────────────
            Button(
                onClick  = {
                    focusManager.clearFocus()
                    authViewModel.signUp(
                        firstName       = firstName,
                        lastName        = lastName,
                        email           = email,
                        phone           = phone,
                        password        = password,
                        confirmPassword = confirm,
                        accessibilityPrefs = UserPreferences(
                            requiresWheelchairAccess = wheelchair,
                            requiresStepFreeRoute    = stepFree,
                            preferFamilyRestroom     = familyRoom,
                            preferGenderNeutral      = genderNeutral,
                            preferredAmenityType     = preferredType
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled  = state !is AuthUiState.Loading
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Small helper composables ───────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AccessibilityToggle(
    label:           String,
    subLabel:        String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label,    style = MaterialTheme.typography.bodyMedium)
            Text(subLabel, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
