package com.smartamenities.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.smartamenities.viewmodel.NavigationUiState
import com.smartamenities.viewmodel.NavigationViewModel

/**
 * NavigationScreen — implements the navigation loop from the Sequence Diagram
 *
 * States handled:
 *   Idle             → should not normally appear (navigated away before idle)
 *   Loading          → generating route
 *   Navigating       → active turn-by-turn, step shown prominently at top
 *   Rerouting        → recalculating after disruption
 *   AmenityUnavailable → amenity closed when we arrived, show alternative
 *   Arrived          → navigation complete
 *
 * Key SRS requirements covered:
 *   FR 2.2.1  – instructions displayed graphically and textually
 *   FR 2.6    – End Navigation button returns to map
 *   FR 2.7    – floor transitions shown with elevator/escalator icons
 *   FR 2.8    – auto-stop at destination
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    amenity: Amenity,
    preferences: UserPreferences,
    onEndNavigation: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val navState by viewModel.navState.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()

    // Begin navigation as soon as this screen appears
    LaunchedEffect(amenity.id) {
        viewModel.beginNavigation(amenity, preferences)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigating to ${amenity.type.displayName}") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.endNavigation()
                        onEndNavigation()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "End navigation")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = navState) {

                // Idle means endNavigation() was called — compose nav will pop this screen.
                // Show nothing rather than the spinner to avoid a flash of "calculating".
                is NavigationUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                is NavigationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calculating your route…")
                        }
                    }
                }

                is NavigationUiState.Rerouting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Recalculating route…", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                is NavigationUiState.AmenityUnavailable -> {
                    AmenityUnavailableContent(
                        amenity = state.amenity,
                        onEndNavigation = {
                            viewModel.endNavigation()
                            onEndNavigation()
                        }
                    )
                }

                is NavigationUiState.Arrived -> {
                    ArrivedContent(
                        amenity = state.amenity,
                        onDone = {
                            viewModel.endNavigation()
                            onEndNavigation()
                        }
                    )
                }

                is NavigationUiState.Navigating -> {
                    NavigatingContent(
                        state = state,
                        currentStepIndex = currentStepIndex,
                        onStepComplete = { viewModel.completeStep() },
                        onReroute = { viewModel.triggerReroute(preferences) },
                        onEndNavigation = {
                            viewModel.endNavigation()
                            onEndNavigation()
                        }
                    )
                }
            }
        }
    }
}

// ── Active navigation content ──────────────────────────────────────────────────

@Composable
private fun NavigatingContent(
    state: NavigationUiState.Navigating,
    currentStepIndex: Int,
    onStepComplete: () -> Unit,
    onReroute: () -> Unit,
    onEndNavigation: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Current step — shown prominently (FR 2.2.1) ──────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DirectionIconDisplay(state.currentStep.directionIcon)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Step ${state.currentStep.stepNumber} of ${state.route.steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = state.currentStep.instruction,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (state.currentStep.distanceMeters > 0) {
                            Text(
                                text = "${state.currentStep.distanceMeters.toInt()} m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Floor transition alert (FR 2.7)
                state.currentStep.floorTransition?.let { transition ->
                    Spacer(modifier = Modifier.height(8.dp))
                    FloorTransitionBanner(transition)
                }
            }
        }

        // ── Progress indicator ─────────────────────────────────────────────────
        LinearProgressIndicator(
            progress = { (currentStepIndex + 1f) / state.route.steps.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // ── Remaining steps list ───────────────────────────────────────────────
        Text(
            text = "Upcoming steps",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(state.route.steps) { index, step ->
                if (index > currentStepIndex) {
                    StepListItem(step = step, isNext = index == currentStepIndex + 1)
                }
            }
        }

        // ── Action buttons ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStepComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Step complete – Next")
            }
            OutlinedButton(
                onClick = onReroute,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reroute")
            }
        }
    }
}

// ── Step list item ─────────────────────────────────────────────────────────────

@Composable
private fun StepListItem(step: NavigationStep, isNext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${step.stepNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isNext) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = step.instruction,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isNext) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Direction icon display ─────────────────────────────────────────────────────

@Composable
private fun DirectionIconDisplay(direction: DirectionIcon) {
    val icon = when (direction) {
        DirectionIcon.STRAIGHT       -> Icons.Default.ArrowUpward
        DirectionIcon.TURN_LEFT      -> Icons.Default.TurnLeft
        DirectionIcon.TURN_RIGHT     -> Icons.Default.TurnRight
        DirectionIcon.SLIGHT_LEFT    -> Icons.Default.TurnSlightLeft
        DirectionIcon.SLIGHT_RIGHT   -> Icons.Default.TurnSlightRight
        DirectionIcon.ELEVATOR_UP    -> Icons.Default.Elevator
        DirectionIcon.ELEVATOR_DOWN  -> Icons.Default.Elevator
        DirectionIcon.ESCALATOR_UP   -> Icons.Default.Escalator
        DirectionIcon.ESCALATOR_DOWN -> Icons.Default.EscalatorWarning
        DirectionIcon.STAIRS_UP      -> Icons.Default.Stairs
        DirectionIcon.STAIRS_DOWN    -> Icons.Default.Stairs
        DirectionIcon.ARRIVE         -> Icons.Default.Place
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = direction.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ── Floor transition banner ────────────────────────────────────────────────────

@Composable
private fun FloorTransitionBanner(transition: FloorTransition) {
    val description = when (transition.transitionType) {
        TransitionType.ELEVATOR  -> "Take elevator from Floor ${transition.fromFloor} to Floor ${transition.toFloor}"
        TransitionType.ESCALATOR -> "Take escalator from Floor ${transition.fromFloor} to Floor ${transition.toFloor}"
        TransitionType.STAIRS    -> "Use stairs from Floor ${transition.fromFloor} to Floor ${transition.toFloor}"
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Elevator, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Arrived screen ─────────────────────────────────────────────────────────────

@Composable
private fun ArrivedContent(amenity: Amenity, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("You've arrived!", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = amenity.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

// ── Amenity unavailable ────────────────────────────────────────────────────────

@Composable
private fun AmenityUnavailableContent(amenity: Amenity, onEndNavigation: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text("Amenity Unavailable", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${amenity.name} is currently ${amenity.status.displayName.lowercase()}.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Return to the list to find an alternative.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Button(onClick = onEndNavigation, modifier = Modifier.fillMaxWidth()) {
                Text("Find alternative")
            }
        }
    }
}
