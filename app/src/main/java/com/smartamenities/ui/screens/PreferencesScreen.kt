package com.smartamenities.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartamenities.data.model.UserPreferences
import com.smartamenities.viewmodel.AmenityViewModel

/**
 * PreferencesScreen — FR 4.1, FR 4.2
 *
 * Lets passengers set accessibility preferences that filter and rank
 * amenities throughout the app. Stored locally only — no PII (NFR 4.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    viewModel: AmenityViewModel = hiltViewModel()
) {
    val currentPrefs by viewModel.preferences.collectAsState()

    // Local copy to edit before saving
    var wheelchair by remember { mutableStateOf(currentPrefs.requiresWheelchairAccess) }
    var stepFree by remember { mutableStateOf(currentPrefs.requiresStepFreeRoute) }
    var family by remember { mutableStateOf(currentPrefs.preferFamilyRestroom) }
    var genderNeutral by remember { mutableStateOf(currentPrefs.preferGenderNeutral) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Preferences") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your preferences are stored only on this device and never shared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mobility & Access",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PreferenceToggleRow(
                label = "Wheelchair accessible routes only",
                description = "Only show amenities with wheelchair-accessible paths",
                icon = Icons.Default.Accessible,
                checked = wheelchair,
                onCheckedChange = { wheelchair = it }
            )

            PreferenceToggleRow(
                label = "Step-free routes only",
                description = "Avoid stairs and escalators — use elevators only",
                icon = Icons.Default.Elevator,
                checked = stepFree,
                onCheckedChange = { stepFree = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Amenity Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PreferenceToggleRow(
                label = "Prefer family restrooms",
                description = "Prioritise family restrooms in recommendations",
                icon = Icons.Default.FamilyRestroom,
                checked = family,
                onCheckedChange = { family = it }
            )

            PreferenceToggleRow(
                label = "Prefer gender-neutral restrooms",
                description = "Prioritise gender-neutral options",
                icon = Icons.Default.Wc,
                checked = genderNeutral,
                onCheckedChange = { genderNeutral = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    viewModel.updatePreferences(
                        UserPreferences(
                            requiresWheelchairAccess = wheelchair,
                            requiresStepFreeRoute = stepFree,
                            preferFamilyRestroom = family,
                            preferGenderNeutral = genderNeutral
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
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
