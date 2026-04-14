package com.smartamenities.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartamenities.data.model.*
import com.smartamenities.ui.components.*
import com.smartamenities.viewmodel.AmenityUiState
import com.smartamenities.viewmodel.AmenityViewModel

/**
 * AmenityListScreen — FR 1.2
 *
 * Shows a scrollable list of amenities sorted by time-to-reach (FR 1.2.4).
 * The horizontal chip row at the top lets passengers filter by type (FR 1.2.2).
 * Tapping an item opens AmenityDetailScreen.
 *
 * As a beginner, notice the pattern here:
 *   1. We ask Hilt for a ViewModel with hiltViewModel()
 *   2. We collect StateFlow as Compose state with collectAsState()
 *   3. When(uiState) handles Loading / Success / Empty / Error
 *   4. We never touch the repository directly — that's the ViewModel's job
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenityListScreen(
    onAmenitySelected: (Amenity) -> Unit,
    onOpenPreferences: () -> Unit,
    viewModel: AmenityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    // Auto-refresh every 5 minutes while this screen is visible.
    // Cancelled automatically when the user navigates away.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60 * 1_000L)
            viewModel.triggerRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal D Amenities", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "DFW Airport",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // One icon per active filter so the user knows exactly what's on
                    if (preferences.requiresWheelchairAccess) {
                        Icon(Icons.Default.Accessible, contentDescription = "Wheelchair filter active",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.requiresStepFreeRoute) {
                        Icon(Icons.Default.Elevator, contentDescription = "Step-free filter active",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.preferFamilyRestroom) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = "Family restroom filter active",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.preferGenderNeutral) {
                        Icon(Icons.Default.Wc, contentDescription = "Gender-neutral filter active",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                    }
                    IconButton(onClick = onOpenPreferences) {
                        Icon(Icons.Default.Tune, contentDescription = "Preferences")
                    }
                    IconButton(onClick = { viewModel.triggerRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues)) {

            // ── Type filter chips (FR 1.2.2) ──────────────────────────────────
            AmenityTypeFilterRow(
                selectedType = selectedType,
                onTypeSelected = { viewModel.selectAmenityType(it) }
            )

            HorizontalDivider()

            // ── List content ──────────────────────────────────────────────────
            when (val state = uiState) {
                is AmenityUiState.Loading -> LoadingContent()
                is AmenityUiState.Empty   -> EmptyContent(selectedType)
                is AmenityUiState.Error   -> ErrorContent(state.message) { viewModel.triggerRefresh() }
                is AmenityUiState.Success -> AmenityList(
                    amenities = state.amenities,
                    onAmenitySelected = onAmenitySelected
                )
            }
        }
    }
}

// ── Type filter chips ──────────────────────────────────────────────────────────

@Composable
private fun AmenityTypeFilterRow(
    selectedType: AmenityType?,
    onTypeSelected: (AmenityType?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("All") }
            )
        }
        items(AmenityType.values()) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                leadingIcon = if (selectedType == type) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

// ── Amenity list ───────────────────────────────────────────────────────────────

@Composable
private fun AmenityList(
    amenities: List<Amenity>,
    onAmenitySelected: (Amenity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sort label — explains why this order (FR 4.4: explain recommendations)
        item {
            Text(
                text = "Sorted by estimated total time (walk + wait)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(amenities, key = { it.id }) { amenity ->
            AmenityCard(
                amenity = amenity,
                onClick = { onAmenitySelected(amenity) }
            )
        }
    }
}

// ── Amenity card ───────────────────────────────────────────────────────────────

@Composable
private fun AmenityCard(amenity: Amenity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Time-to-reach badge — the primary ranking signal (FR 1.2.4)
            TimeToReachBadge(
                walkMinutes = amenity.estimatedWalkMinutes,
                crowdLevel = amenity.crowdLevel
            )

            Spacer(modifier = Modifier.width(16.dp))
            VerticalDivider(modifier = Modifier.height(60.dp))
            Spacer(modifier = Modifier.width(16.dp))

            // Amenity details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = amenity.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = amenity.gateProximity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmenityStatusChip(amenity.status)
                    CrowdLevelChip(amenity.crowdLevel)
                }
                Spacer(modifier = Modifier.height(4.dp))
                AccessibilityBadgeRow(amenity)
                Spacer(modifier = Modifier.height(4.dp))
                DataFreshnessIndicator(
                    timestampMillis = amenity.dataFreshnessTimestamp,
                    confidenceScore = amenity.confidenceScore
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ── State placeholder screens ──────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Finding nearby amenities…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyContent(selectedType: AmenityType?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedType != null)
                    "No ${selectedType.displayName.lowercase()}s available nearby"
                else
                    "No amenities found matching your preferences",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Could not load amenities", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}
