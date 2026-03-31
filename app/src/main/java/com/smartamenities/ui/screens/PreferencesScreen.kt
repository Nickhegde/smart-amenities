package com.smartamenities.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.ui.theme.DfwBlue
import com.smartamenities.viewmodel.AmenityViewModel
import com.smartamenities.viewmodel.AuthViewModel

/**
 * PreferencesScreen — FR 4.1, FR 4.2
 *
 * Two sections:
 *  1. Account card — shows who is signed in and offers Sign Out (registered)
 *     or Sign In / Create Account (guest).
 *  2. Accessibility preferences — wheelchair, step-free, family, gender-neutral.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack:        () -> Unit,
    onSignOut:     () -> Unit,
    authViewModel: AuthViewModel,
    viewModel:     AmenityViewModel = hiltViewModel()
) {
    val currentPrefs by viewModel.preferences.collectAsState()
    val currentUser  by authViewModel.currentUser.collectAsState()

    // Local copy to edit before saving
    var wheelchair   by remember { mutableStateOf(currentPrefs.requiresWheelchairAccess) }
    var stepFree     by remember { mutableStateOf(currentPrefs.requiresStepFreeRoute) }
    var family       by remember { mutableStateOf(currentPrefs.preferFamilyRestroom) }
    var genderNeutral by remember { mutableStateOf(currentPrefs.preferGenderNeutral) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    // Confirmation dialog for Sign Out
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon  = { Icon(Icons.Default.Logout, contentDescription = null) },
            title = { Text("Sign out?") },
            text  = { Text("You will return to the sign-in screen. Your accessibility preferences will be saved to your account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.logout()
                        onSignOut()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Account card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(DfwBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = currentUser?.initials ?: "?",
                            color      = Color.White,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = currentUser?.displayName ?: "Not signed in",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (currentUser?.isGuest == true) {
                            Text(
                                text  = "Guest account · sign in to save your preferences",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text  = currentUser?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            if (currentUser?.phone?.isNotBlank() == true) {
                                Text(
                                    text  = currentUser!!.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )

                // Account action button
                if (currentUser?.isGuest == true) {
                    // Guest → encourage them to create a real account
                    TextButton(
                        onClick  = { authViewModel.logout(); onSignOut() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign In or Create Account")
                    }
                } else {
                    // Registered user → sign out with confirmation
                    TextButton(
                        onClick  = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Accessibility preferences ─────────────────────────────────────
            Text(
                text  = "Your preferences are stored only on this device and never shared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Mobility & Access",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PreferenceToggleRow(
                label       = "Wheelchair accessible routes only",
                description = "Only show amenities with wheelchair-accessible paths",
                icon        = Icons.Default.Accessible,
                checked     = wheelchair,
                onCheckedChange = { wheelchair = it }
            )

            PreferenceToggleRow(
                label       = "Step-free routes only",
                description = "Avoid stairs and escalators — use elevators only",
                icon        = Icons.Default.Elevator,
                checked     = stepFree,
                onCheckedChange = { stepFree = it }
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Amenity Type",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PreferenceToggleRow(
                label       = "Prefer family restrooms",
                description = "Prioritise family restrooms in recommendations",
                icon        = Icons.Default.FamilyRestroom,
                checked     = family,
                onCheckedChange = { family = it }
            )

            PreferenceToggleRow(
                label       = "Prefer gender-neutral restrooms",
                description = "Prioritise gender-neutral options",
                icon        = Icons.Default.Wc,
                checked     = genderNeutral,
                onCheckedChange = { genderNeutral = it }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updatePreferences(
                        UserPreferences(
                            requiresWheelchairAccess = wheelchair,
                            requiresStepFreeRoute    = stepFree,
                            preferFamilyRestroom     = family,
                            preferGenderNeutral      = genderNeutral
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save preferences")
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label:           String,
    description:     String,
    icon:            androidx.compose.ui.graphics.vector.ImageVector,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label,       style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
