package com.smartamenities.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartamenities.data.model.*
import com.smartamenities.ui.components.*
import com.smartamenities.viewmodel.AmenityViewModel
import com.smartamenities.ui.components.AccountIconButton

/**
 * AmenityDetailScreen — FR 1.1.4
 *
 * Shows all metadata for a selected amenity:
 *  - Status, crowd level, time-to-reach
 *  - Data freshness timestamp + confidence
 *  - Accessibility features
 *  - "Navigate" CTA that triggers UC10
 *
 * The amenity is passed in by ID and fetched from the ViewModel
 * so the data is always fresh (not stale from the list view).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenityDetailScreen(
    amenityId: String,
    onNavigate: (Amenity) -> Unit,
    onBack: () -> Unit,
    viewModel: AmenityViewModel = hiltViewModel(),
    currentUser: com.smartamenities.data.model.User? = null,
    onSignOut: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    // Fetch the specific amenity from the ViewModel
    LaunchedEffect(amenityId) {
        // In a real app: viewModel.loadAmenityById(amenityId)
        // For now we find it from the already-loaded list
    }

    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val preSelectedAmenity by viewModel.selectedAmenity.collectAsState()

    // Find the amenity: first try the loaded list, then fall back to whatever the
    // caller stored in the ViewModel (covers map-pin placeholders whose IDs don't
    // appear in the repository's list).
    val amenity = when (val state = uiState) {
        is com.smartamenities.viewmodel.AmenityUiState.Success ->
            state.amenities.find { it.id == amenityId }
        else -> null
    } ?: preSelectedAmenity?.takeIf { it.id == amenityId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(amenity?.name ?: "Amenity Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AccountIconButton(
                        currentUser      = currentUser,
                        onSignOut        = onSignOut,
                        onNavigateToAuth = onNavigateToAuth,
                        onOpenSettings   = onOpenSettings
                    )
                }
            )
        }
    ) { paddingValues ->

        if (amenity == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Status summary card ────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = amenity.type.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = amenity.gateProximity,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Floor ${amenity.floor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        TimeToReachBadge(
                            walkMinutes = amenity.estimatedWalkMinutes,
                            crowdLevel = amenity.crowdLevel
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmenityStatusChip(amenity.status)
                        CrowdLevelChip(amenity.crowdLevel)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DataFreshnessIndicator(
                        timestampMillis = amenity.dataFreshnessTimestamp,
                        confidenceScore = amenity.confidenceScore
                    )
                }
            }

            // ── Walk time breakdown ────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time estimate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeBreakdownRow(
                        label = "Walk time",
                        value = "${amenity.estimatedWalkMinutes} min",
                        icon = Icons.Default.DirectionsWalk
                    )
                    if (amenity.crowdLevel != CrowdLevel.UNKNOWN && amenity.crowdLevel.waitEstimateMinutes > 0) {
                        TimeBreakdownRow(
                            label = "Estimated wait",
                            value = "~${amenity.crowdLevel.waitEstimateMinutes} min",
                            icon = Icons.Default.HourglassBottom
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TimeBreakdownRow(
                        label = "Total",
                        value = "${amenity.estimatedWalkMinutes + amenity.crowdLevel.waitEstimateMinutes} min",
                        icon = Icons.Default.Schedule,
                        bold = true
                    )
                }
            }

            // ── Accessibility features ─────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Accessibility",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AccessibilityRow("Wheelchair accessible", amenity.isWheelchairAccessible)
                    AccessibilityRow("Step-free route", amenity.isStepFreeRoute)
                    AccessibilityRow("Family restroom", amenity.isFamilyRestroom)
                    AccessibilityRow("Gender neutral", amenity.isGenderNeutral)
                }
            }

            // ── Unavailability warning ─────────────────────────────────────────
            if (amenity.status != AmenityStatus.OPEN) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "This amenity is currently ${amenity.status.displayName.lowercase()}. " +
                                   "Navigation will route you to an alternative.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── Navigate CTA — the main action (UC10) ─────────────────────────
            Button(
                onClick = { onNavigate(amenity) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = amenity.status != AmenityStatus.OUT_OF_SERVICE
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (amenity.status == AmenityStatus.OPEN)
                        "Navigate to ${amenity.type.displayName}"
                    else
                        "Navigate to alternative",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Report status link (user reporting — SRS area FR 3.5)
            TextButton(
                onClick = { viewModel.reportStatus(amenity.id, AmenityStatus.CLOSED) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Report incorrect status", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Small helper composables ───────────────────────────────────────────────────

@Composable
private fun TimeBreakdownRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AccessibilityRow(label: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (supported) "Yes" else "No",
            tint = if (supported) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
