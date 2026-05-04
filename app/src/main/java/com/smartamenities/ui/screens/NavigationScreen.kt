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

                is NavigationUiState.ClosureRerouting -> {
                    ClosureReroutingContent(
                        state = state,
                        onNavigateToAlternative = { alt ->
                            viewModel.navigateToAlternative(alt)
                        },
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
                .height(220.dp)
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

// ── Route mini-map — real map background, auto-zoomed to route ────────────────

// Image-coordinate (x,y) lookup for every graph node.
// Matches the 431×793 terminal_d.png; restroom nodes use the same values as
// STATIC_LOCATIONS in MapScreen so they align with the visible amenity pins.
private val NAV_IMAGE_COORDS: Map<String, Pair<Float, Float>> = mapOf(
    // ── Corridor spine ────────────────────────────────────────────────────────
    "COR_W"   to Pair(0.75f, 0.93f),
    "SKY_W"   to Pair(0.62f, 0.90f),
    "SKY_E"   to Pair(0.46f, 0.83f),
    "SEC_D18" to Pair(0.40f, 0.79f),
    "COR_C"   to Pair(0.37f, 0.68f),
    "COR_E"   to Pair(0.33f, 0.56f),
    "SEC_D30" to Pair(0.34f, 0.32f),
    // ── Gate nodes ────────────────────────────────────────────────────────────
    "D5"  to Pair(0.82f, 0.97f),
    "D6"  to Pair(0.80f, 0.95f),
    "D7"  to Pair(0.80f, 0.94f),
    "D8"  to Pair(0.88f, 0.93f),
    "D9"  to Pair(0.77f, 0.92f),
    "D10" to Pair(0.68f, 0.91f),
    "D11" to Pair(0.58f, 0.93f),
    "D12" to Pair(0.42f, 0.94f),
    "D14" to Pair(0.52f, 0.88f),
    "D15" to Pair(0.58f, 0.86f),
    "D16" to Pair(0.62f, 0.85f),
    "D17" to Pair(0.66f, 0.84f),
    "D18" to Pair(0.38f, 0.81f),
    "D19" to Pair(0.35f, 0.76f),
    "D20" to Pair(0.32f, 0.73f),
    "D21" to Pair(0.28f, 0.67f),
    "D22" to Pair(0.22f, 0.57f),
    "D23" to Pair(0.22f, 0.47f),
    "D24" to Pair(0.22f, 0.37f),
    "D25" to Pair(0.22f, 0.31f),
    "D26" to Pair(0.22f, 0.27f),
    "D27" to Pair(0.22f, 0.24f),
    "D28" to Pair(0.28f, 0.20f),
    "D29" to Pair(0.22f, 0.16f),
    "D30" to Pair(0.38f, 0.28f),
    "D31" to Pair(0.50f, 0.22f),
    "D33" to Pair(0.40f, 0.08f),
    "D34" to Pair(0.52f, 0.06f),
    "D36" to Pair(0.73f, 0.04f),
    "D37" to Pair(0.88f, 0.03f),
    "D38" to Pair(0.88f, 0.06f),
    "D39" to Pair(0.82f, 0.09f),
    "D40" to Pair(0.75f, 0.12f),
    // ── Amenity nodes (calibrated to match MapScreen pin positions) ───────────
    "REST_D6"  to Pair(0.80f, 0.95f),
    "REST_D10" to Pair(0.68f, 0.93f),
    "REST_D17" to Pair(0.74f, 0.87f),
    "REST_D20" to Pair(0.27f, 0.71f),
    "REST_D22" to Pair(0.27f, 0.63f),
    "REST_D24" to Pair(0.22f, 0.36f),
    "REST_D27" to Pair(0.22f, 0.22f),
    "REST_D29" to Pair(0.22f, 0.14f),
    "REST_D36" to Pair(0.75f, 0.06f),
    "REST_D40" to Pair(0.75f, 0.12f),
    "FAM_D18"  to Pair(0.24f, 0.84f),
    "FAM_D25"  to Pair(0.22f, 0.30f),
    "FAM_D28"  to Pair(0.35f, 0.18f),
    "LAC_D22"  to Pair(0.24f, 0.66f),
)

@Composable
private fun RouteMapPreview(
    routeNodeIds: List<String>,
    destinationAmenity: Amenity,
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "routePath")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 30f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "dashPhase"
    )

    val imgCoords = remember(routeNodeIds) {
        routeNodeIds.mapNotNull { NAV_IMAGE_COORDS[it] }
    }
    val currentNodeIndex = currentStepIndex.coerceIn(0, (imgCoords.size - 1).coerceAtLeast(0))

    Canvas(modifier = modifier) {
        if (imgCoords.size < 2) return@Canvas
        val cw = size.width
        val ch = size.height

        // ── Viewport: bounding box of route + padding ─────────────────────────
        val pad = 0.09f
        var minX = imgCoords.minOf { it.first }
        var maxX = imgCoords.maxOf { it.first }
        var minY = imgCoords.minOf { it.second }
        var maxY = imgCoords.maxOf { it.second }

        val minSpan = 0.22f
        if (maxX - minX < minSpan) { val cx = (minX + maxX) / 2f; minX = cx - minSpan/2f; maxX = cx + minSpan/2f }
        if (maxY - minY < minSpan) { val cy = (minY + maxY) / 2f; minY = cy - minSpan/2f; maxY = cy + minSpan/2f }

        var cropL = (minX - pad).coerceIn(0f, 1f)
        var cropR = (maxX + pad).coerceIn(0f, 1f)
        var cropT = (minY - pad).coerceIn(0f, 1f)
        var cropB = (maxY + pad).coerceIn(0f, 1f)

        // Expand to match canvas aspect ratio (prevents distortion)
        val canvasAspect = cw / ch
        val cropWFrac = cropR - cropL
        val cropHFrac = cropB - cropT
        if (cropWFrac / cropHFrac < canvasAspect) {
            val extra = (cropHFrac * canvasAspect - cropWFrac) / 2f
            cropL = (cropL - extra).coerceIn(0f, 1f)
            cropR = (cropR + extra).coerceIn(0f, 1f)
        } else {
            val extra = (cropWFrac / canvasAspect - cropHFrac) / 2f
            cropT = (cropT - extra).coerceIn(0f, 1f)
            cropB = (cropB + extra).coerceIn(0f, 1f)
        }

        // Inset all mapped coordinates by edgeMargin so nodes never land exactly
        // at the canvas boundary — prevents labels/strokes clipping at the edges.
        val edgeMargin = 16f
        fun toCanvas(ix: Float, iy: Float): Offset = Offset(
            edgeMargin + (ix - cropL) / (cropR - cropL) * (cw - 2 * edgeMargin),
            edgeMargin + (iy - cropT) / (cropB - cropT) * (ch - 2 * edgeMargin)
        )
        fun inView(ix: Float, iy: Float): Boolean =
            ix in (cropL - 0.04f)..(cropR + 0.04f) && iy in (cropT - 0.04f)..(cropB + 0.04f)

        // ── Schematic background ──────────────────────────────────────────────
        drawRect(Color(0xFFEDF2F7))

        // ── Terminal corridor skeleton (vector "roads", infinite sharpness) ────
        // Build a Path from a sequence of node IDs, drawing through all nodes
        // even those outside the viewport (canvas clips naturally).
        fun buildRoadPath(ids: List<String>): Path {
            val p = Path()
            var started = false
            for (id in ids) {
                val c = NAV_IMAGE_COORDS[id] ?: continue
                val pt = toCanvas(c.first, c.second)
                if (!started) { p.moveTo(pt.x, pt.y); started = true } else p.lineTo(pt.x, pt.y)
            }
            return p
        }

        val roadBorder = Color(0xFFBBC8D4)
        val roadFill   = Color(0xFFFFFFFF)

        // Main spine: right/bottom cluster up through central corridor to upper-left
        val spinePath = buildRoadPath(
            listOf("COR_W", "SKY_W", "SKY_E", "SEC_D18", "COR_C", "COR_E", "SEC_D30")
        )
        drawPath(spinePath, roadBorder, style = Stroke(28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(spinePath, roadFill,   style = Stroke(18f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Top loop: upper left (D29) curving right to D40
        val topPath = buildRoadPath(
            listOf("SEC_D30", "D29", "D31", "D33", "D34", "D36", "D37", "D38", "D39", "D40")
        )
        drawPath(topPath, roadBorder, style = Stroke(28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(topPath, roadFill,   style = Stroke(18f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // ── Gate markers — orange boxes matching the real map's gate label style ─
        val gateIds = NAV_IMAGE_COORDS.keys.filter { it.matches(Regex("D\\d+")) }
        val labelSizePx = (ch * 0.062f).coerceIn(16f, 34f)
        drawIntoCanvas { canvas ->
            val boxPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.argb(255, 220, 95, 20)  // DFW orange
            }
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = labelSizePx
                color = android.graphics.Color.WHITE
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            for (id in gateIds) {
                val c = NAV_IMAGE_COORDS[id] ?: continue
                if (!inView(c.first, c.second)) continue
                val pt = toCanvas(c.first, c.second)
                val tw = textPaint.measureText(id)
                val boxW = tw + 10f
                val boxH = labelSizePx * 1.35f
                val boxL = pt.x - boxW / 2f
                val boxTop = pt.y - boxH / 2f
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(boxL, boxTop, boxL + boxW, boxTop + boxH),
                    4f, 4f, boxPaint
                )
                canvas.nativeCanvas.drawText(id, pt.x, pt.y + labelSizePx * 0.38f, textPaint)
            }
        }

        // ── Completed segment (gray) ──────────────────────────────────────────
        if (currentNodeIndex > 0) {
            val donePath = Path().apply {
                val s = toCanvas(imgCoords[0].first, imgCoords[0].second)
                moveTo(s.x, s.y)
                for (i in 1..currentNodeIndex) {
                    val p = toCanvas(imgCoords[i].first, imgCoords[i].second)
                    lineTo(p.x, p.y)
                }
            }
            drawPath(donePath, Color(0xBB9E9E9E),
                style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ── Ahead segment (animated blue glow + dashes) ───────────────────────
        val aheadPath = Path().apply {
            val s = toCanvas(imgCoords[currentNodeIndex].first, imgCoords[currentNodeIndex].second)
            moveTo(s.x, s.y)
            for (i in currentNodeIndex + 1 until imgCoords.size) {
                val p = toCanvas(imgCoords[i].first, imgCoords[i].second)
                lineTo(p.x, p.y)
            }
        }
        drawPath(aheadPath, Color(0x601A73E8), style = Stroke(16f, cap = StrokeCap.Round))
        drawPath(aheadPath, Color(0xFF1A73E8),
            style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(aheadPath, Color(0xCCFFFFFF),
            style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f), dashPhase)))

        // ── "You" marker ──────────────────────────────────────────────────────
        val curPt = toCanvas(imgCoords[currentNodeIndex].first, imgCoords[currentNodeIndex].second)
        drawCircle(Color(0x501A73E8), 22f, curPt)
        drawCircle(Color(0xFF1A73E8), 13f, curPt)
        drawCircle(Color.White, 6f, curPt)
        val youTextPx = ch * 0.075f
        drawIntoCanvas { canvas ->
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER
                textSize = youTextPx; isFakeBoldText = true
                color = android.graphics.Color.argb(255, 26, 115, 232)
            }
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true; style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.argb(220, 255, 255, 255)
            }
            val labelY = curPt.y - 30f
            val tw = textPaint.measureText("You")
            canvas.nativeCanvas.drawRoundRect(
                android.graphics.RectF(curPt.x - tw/2f - 6f, labelY - youTextPx * 0.9f,
                    curPt.x + tw/2f + 6f, labelY + youTextPx * 0.2f), 5f, 5f, bgPaint)
            canvas.nativeCanvas.drawText("You", curPt.x, labelY, textPaint)
        }

        // ── Destination marker ────────────────────────────────────────────────
        val destPt = toCanvas(imgCoords.last().first, imgCoords.last().second)
        val destColor = when (destinationAmenity.status) {
            AmenityStatus.OPEN           -> Color(0xFF2E7D32)
            AmenityStatus.CLOSED         -> Color(0xFFC62828)
            AmenityStatus.OUT_OF_SERVICE -> Color(0xFFE65100)
            AmenityStatus.UNKNOWN        -> Color(0xFF757575)
        }
        val destAndroidColor = when (destinationAmenity.status) {
            AmenityStatus.OPEN           -> android.graphics.Color.argb(255, 27,  94,  32)
            AmenityStatus.CLOSED         -> android.graphics.Color.argb(255, 183, 28,  28)
            AmenityStatus.OUT_OF_SERVICE -> android.graphics.Color.argb(255, 191, 54,  12)
            AmenityStatus.UNKNOWN        -> android.graphics.Color.argb(255, 66,  66,  66)
        }
        val destLabel = when (destinationAmenity.type) {
            AmenityType.RESTROOM                -> "Restroom"
            AmenityType.FAMILY_RESTROOM         -> "Family"
            AmenityType.LACTATION_ROOM          -> "Lactation"
            AmenityType.GENDER_NEUTRAL_RESTROOM -> "GN Restroom"
            AmenityType.WATER_FOUNTAIN          -> "Fountain"
        }
        drawCircle(Color(0x50000000), 16f, Offset(destPt.x + 2f, destPt.y + 2f))
        drawCircle(destColor, 15f, destPt)
        drawCircle(Color.White, 15f, destPt, style = Stroke(3f))
        drawCircle(Color.White, 6f, destPt)
        val destTextPx = ch * 0.075f
        drawIntoCanvas { canvas ->
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER
                textSize = destTextPx; isFakeBoldText = true; color = destAndroidColor
            }
            val bgPaint = android.graphics.Paint().apply {
                isAntiAlias = true; style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.argb(220, 255, 255, 255)
            }
            val labelY = destPt.y + 24f + destTextPx
            val tw = textPaint.measureText(destLabel)
            canvas.nativeCanvas.drawRoundRect(
                android.graphics.RectF(destPt.x - tw/2f - 6f, labelY - destTextPx * 0.9f,
                    destPt.x + tw/2f + 6f, labelY + destTextPx * 0.2f), 5f, 5f, bgPaint)
            canvas.nativeCanvas.drawText(destLabel, destPt.x, labelY, textPaint)
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

// ── Closure detected mid-navigation ──────────────────────────────────────────

@Composable
private fun ClosureReroutingContent(
    state: NavigationUiState.ClosureRerouting,
    onNavigateToAlternative: (Amenity) -> Unit,
    onEndNavigation: () -> Unit
) {
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
            Text("Destination Closed", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(
                text = "${state.closedAmenity.name} is now ${state.closedAmenity.status.displayName.lowercase()}.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.alternative != null) {
                val totalMin = state.alternative.estimatedWalkMinutes +
                               state.alternative.crowdLevel.waitEstimateMinutes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Nearest alternative",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            state.alternative.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${state.alternative.gateProximity} · $totalMin min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Button(
                            onClick = { onNavigateToAlternative(state.alternative) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Navigate here")
                        }
                    }
                }
            } else {
                Text(
                    "No open alternatives found nearby.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            OutlinedButton(onClick = onEndNavigation, modifier = Modifier.fillMaxWidth()) {
                Text("Return to map")
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
