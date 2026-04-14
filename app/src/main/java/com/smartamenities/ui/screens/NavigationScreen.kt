package com.smartamenities.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartamenities.data.graph.TerminalDGraph
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

        // ── Route mini-map with animated path ─────────────────────────────────
        RouteMapPreview(
            routeNodeIds      = state.route.routeNodeIds,
            destinationAmenity = state.amenity,
            currentStepIndex  = currentStepIndex,
            totalSteps        = state.route.steps.size,
            modifier          = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

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

// ── Route mini-map with animated path ─────────────────────────────────────────

@Composable
private fun RouteMapPreview(
    routeNodeIds: List<String>,
    destinationAmenity: Amenity,
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val nodeMap = remember { TerminalDGraph.nodes.associateBy { it.id } }
    val pathNodes = remember(routeNodeIds) { routeNodeIds.mapNotNull { nodeMap[it] } }

    val infiniteTransition = rememberInfiniteTransition(label = "routePath")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 38f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )

    // pathToNavigationSteps() uses path.zipWithNext(), so step k = edge node[k]→node[k+1].
    // Therefore the user's current position in the graph is node[currentStepIndex].
    val currentNodeIndex = currentStepIndex.coerceIn(0, (pathNodes.size - 1).coerceAtLeast(0))

    Canvas(modifier = modifier.background(Color(0xFFCFCFCA))) {
        val w  = size.width
        val h  = size.height
        val bL = w * 0.02f
        val bR = w * 0.98f
        val bW = bR - bL

        val topArmT = h * 0.08f
        val topArmB = h * 0.42f
        val botArmT = h * 0.58f
        val botArmB = h * 0.92f
        val eastX   = bL + bW * 0.84f

        // ── Simplified floor plan ─────────────────────────────────────────────
        val floorColor = Color(0xFFF2F2EE)
        drawRect(floorColor, topLeft = Offset(bL, topArmT), size = Size(bW, topArmB - topArmT))
        drawRect(floorColor, topLeft = Offset(bL, botArmT), size = Size(bW, botArmB - botArmT))
        drawRect(floorColor, topLeft = Offset(eastX, topArmB), size = Size(bR - eastX, botArmT - topArmB))
        drawRect(Color(0xFFD8E8F8), topLeft = Offset(bL, topArmB), size = Size(eastX - bL, botArmT - topArmB))

        val uPath = Path().apply {
            moveTo(bL, topArmT); lineTo(bR, topArmT)
            lineTo(bR, botArmB); lineTo(bL, botArmB)
            lineTo(bL, botArmT); lineTo(eastX, botArmT)
            lineTo(eastX, topArmB); lineTo(bL, topArmB)
            close()
        }
        drawPath(uPath, Color(0xFF707070), style = Stroke(width = 4f))

        if (pathNodes.size < 2) return@Canvas

        fun nodeX(nx: Float) = bL + nx * bW
        fun nodeY(ny: Float) = ny * h

        // ── Completed segment: origin → currentNodeIndex (gray) ──────────────
        if (currentNodeIndex > 0) {
            val donePath = Path().apply {
                moveTo(nodeX(pathNodes[0].x), nodeY(pathNodes[0].y))
                for (i in 1..currentNodeIndex) {
                    lineTo(nodeX(pathNodes[i].x), nodeY(pathNodes[i].y))
                }
            }
            drawPath(donePath, Color(0xFF9E9E9E),
                style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ── Remaining segment: currentNodeIndex → destination (animated blue) ─
        val aheadPath = Path().apply {
            moveTo(nodeX(pathNodes[currentNodeIndex].x), nodeY(pathNodes[currentNodeIndex].y))
            for (i in currentNodeIndex + 1 until pathNodes.size) {
                lineTo(nodeX(pathNodes[i].x), nodeY(pathNodes[i].y))
            }
        }
        // Glow shadow
        drawPath(aheadPath, Color(0x601A73E8),
            style = Stroke(width = 14f, cap = StrokeCap.Round))
        // Solid blue fill
        drawPath(aheadPath, Color(0xFF1A73E8),
            style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // White dashes flowing toward destination
        drawPath(aheadPath, Color(0xCCFFFFFF),
            style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 18f), dashPhase)))

        // ── Gate labels along the path ────────────────────────────────────────
        val gateLabelPx = h * 0.085f
        drawIntoCanvas { canvas ->
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign   = android.graphics.Paint.Align.CENTER
                textSize    = gateLabelPx
                isFakeBoldText = true
                color       = android.graphics.Color.argb(190, 40, 40, 40)
            }
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color       = android.graphics.Color.argb(190, 255, 255, 255)
                style       = android.graphics.Paint.Style.FILL
            }
            var lastLabelX = -999f
            pathNodes.forEachIndexed { idx, node ->
                val gateNum = node.id.removePrefix("D").toIntOrNull() ?: return@forEachIndexed
                val cx = nodeX(node.x)
                // Suppress labels too close together horizontally
                if (kotlin.math.abs(cx - lastLabelX) < bW * 0.10f) return@forEachIndexed
                lastLabelX = cx
                val cy = nodeY(node.y)
                // Top arm → label below node; bottom arm → label above node
                val isTopArm = node.y < 0.45f
                val labelY = if (isTopArm) cy + gateLabelPx * 1.5f else cy - gateLabelPx * 0.4f
                val tw = labelPaint.measureText(node.id)
                val pill = android.graphics.RectF(
                    cx - tw / 2f - 5f, labelY - gateLabelPx * 0.9f,
                    cx + tw / 2f + 5f, labelY + gateLabelPx * 0.15f
                )
                canvas.nativeCanvas.drawRoundRect(pill, 5f, 5f, bgPaint)
                canvas.nativeCanvas.drawText(node.id, cx, labelY, labelPaint)
            }
        }

        // ── "You are here" marker at current position ─────────────────────────
        val curNode = pathNodes[currentNodeIndex]
        val curX    = nodeX(curNode.x)
        val curY    = nodeY(curNode.y)
        drawCircle(Color(0x501A73E8), radius = 22f, center = Offset(curX, curY))
        drawCircle(Color(0xFF1A73E8), radius = 13f, center = Offset(curX, curY))
        drawCircle(Color.White,       radius =  6f, center = Offset(curX, curY))

        val youLabelPx = h * 0.10f
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign   = android.graphics.Paint.Align.CENTER
                textSize    = youLabelPx
                isFakeBoldText = true
                color       = android.graphics.Color.argb(255, 26, 115, 232)
            }
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color       = android.graphics.Color.argb(230, 255, 255, 255)
                style       = android.graphics.Paint.Style.FILL
            }
            val isTopArm = curNode.y < 0.45f
            val labelY = if (isTopArm) curY - 26f else curY + 26f + youLabelPx
            val tw = paint.measureText("You")
            val pill = android.graphics.RectF(
                curX - tw / 2f - 7f, labelY - youLabelPx * 0.9f,
                curX + tw / 2f + 7f, labelY + youLabelPx * 0.15f
            )
            canvas.nativeCanvas.drawRoundRect(pill, 6f, 6f, bgPaint)
            canvas.nativeCanvas.drawText("You", curX, labelY, paint)
        }

        // ── Destination marker ────────────────────────────────────────────────
        val destNode  = pathNodes.last()
        val destX     = nodeX(destNode.x)
        val destY     = nodeY(destNode.y)
        val destColor = when (destinationAmenity.status) {
            AmenityStatus.OPEN           -> Color(0xFF2E7D32)
            AmenityStatus.CLOSED         -> Color(0xFFC62828)
            AmenityStatus.OUT_OF_SERVICE -> Color(0xFFE65100)
            AmenityStatus.UNKNOWN        -> Color(0xFF757575)
        }
        drawCircle(Color(0x50000000), radius = 16f, center = Offset(destX + 3f, destY + 3f))
        drawCircle(destColor, radius = 15f, center = Offset(destX, destY))
        drawCircle(Color.White, radius = 15f, center = Offset(destX, destY), style = Stroke(3.5f))
        drawCircle(Color.White, radius =  6f, center = Offset(destX, destY))

        val destLabel = when (destinationAmenity.type) {
            AmenityType.RESTROOM                -> "Restroom"
            AmenityType.FAMILY_RESTROOM         -> "Family"
            AmenityType.LACTATION_ROOM          -> "Lactation"
            AmenityType.GENDER_NEUTRAL_RESTROOM -> "Gender-Neutral"
            AmenityType.WATER_FOUNTAIN          -> "Fountain"
        }
        val destLabelPx = h * 0.10f
        val destAndroidColor = when (destinationAmenity.status) {
            AmenityStatus.OPEN           -> android.graphics.Color.argb(255, 27, 94, 32)
            AmenityStatus.CLOSED         -> android.graphics.Color.argb(255, 183, 28, 28)
            AmenityStatus.OUT_OF_SERVICE -> android.graphics.Color.argb(255, 191, 54, 12)
            AmenityStatus.UNKNOWN        -> android.graphics.Color.argb(255, 66, 66, 66)
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign   = android.graphics.Paint.Align.CENTER
                textSize    = destLabelPx
                isFakeBoldText = true
                color       = destAndroidColor
            }
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color       = android.graphics.Color.argb(230, 255, 255, 255)
                style       = android.graphics.Paint.Style.FILL
            }
            val isTopArm = destNode.y < 0.45f
            val labelY = if (isTopArm) destY - 22f else destY + 22f + destLabelPx
            val tw = paint.measureText(destLabel)
            val pill = android.graphics.RectF(
                destX - tw / 2f - 7f, labelY - destLabelPx * 0.9f,
                destX + tw / 2f + 7f, labelY + destLabelPx * 0.15f
            )
            canvas.nativeCanvas.drawRoundRect(pill, 6f, 6f, bgPaint)
            canvas.nativeCanvas.drawText(destLabel, destX, labelY, paint)
        }
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
