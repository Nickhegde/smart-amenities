package com.smartamenities.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartamenities.data.model.*
import com.smartamenities.ui.components.*
import com.smartamenities.ui.components.AccountIconButton
import com.smartamenities.ui.theme.*
import com.smartamenities.viewmodel.AmenityUiState
import com.smartamenities.viewmodel.AmenityViewModel
import kotlin.math.sqrt

// ── Private types ─────────────────────────────────────────────────────────────

private data class MapPin(
    val amenity: Amenity,
    /** 0.0–1.0 within the terminal building width */
    val xFraction: Float,
    /** 0.0–1.0 within the Canvas height */
    val yFraction: Float,
    val floor: TerminalFloor
)

private data class StaticLocation(
    val id: String,
    val gate: String,
    val type: AmenityType,
    /** true = top wall (D5–D22), false = bottom wall (D23–D40) */
    val topWall: Boolean,
    val floor: TerminalFloor,
    val xNudge: Float = 0f,
    val yNudge: Float = 0f
)

/**
 * The three physical levels of Terminal D at DFW:
 *
 *  Level 1 · Arrivals   — Baggage claim, Customs & Immigration exit, ground transport.
 *                          This is where international passengers first arrive.
 *                          Amenities here serve arriving passengers before they leave the terminal.
 *
 *  Level 3 · Gates      — Security checkpoints, all departure gates (D5–D40), duty-free shops,
 *                          restaurants and the majority of restrooms. This is the main floor
 *                          passengers use once they clear security.
 *
 *  Level 4 · Mezzanine  — Upper concourse level above the gates: lounge seating, food court
 *                          overflow, and quieter amenities such as lactation rooms.
 */
private enum class TerminalFloor(val label: String, val description: String) {
    ARRIVALS(
        "Level 1 · Arrivals",
        "Baggage claim, Customs exit & ground transport"
    ),
    GATES(
        "Level 3 · Gates",
        "Security, all gates D5–D40, shops & most restrooms"
    ),
    MEZZANINE(
        "Level 4 · Mezzanine",
        "Upper concourse: lounges, food court & lactation rooms"
    )
}

// ── Static layout of Terminal D amenities ────────────────────────────────────
//
// Floor assignment rationale:
//   Level 1 (Arrivals) – restrooms near D22 serve customs-exit passengers ("You are here")
//                        and near D40 (far arrivals end).
//   Level 3 (Gates)    – all main gate-side restrooms and family rooms that departing
//                        passengers use after clearing security.
//   Level 4 (Mezzanine)– lactation room and the restroom cluster at the D22 mezzanine end.

private val STATIC_LOCATIONS: List<StaticLocation> = listOf(
    // ── Level 3 · Gates (main gate floor, most amenities) ────────────────────
    StaticLocation("pin_rest_d6",  "D6",  AmenityType.RESTROOM,        topWall = true,  floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d10", "D10", AmenityType.RESTROOM,        topWall = true,  floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d17", "D17", AmenityType.RESTROOM,        topWall = true,  floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d20", "D20", AmenityType.RESTROOM,        topWall = true,  floor = TerminalFloor.GATES),
    StaticLocation("pin_fam_d18",  "D18", AmenityType.FAMILY_RESTROOM, topWall = true,  floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d24", "D24", AmenityType.RESTROOM,        topWall = false, floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d27", "D27", AmenityType.RESTROOM,        topWall = false, floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d29", "D29", AmenityType.RESTROOM,        topWall = false, floor = TerminalFloor.GATES),
    StaticLocation("pin_rest_d36", "D36", AmenityType.RESTROOM,        topWall = false, floor = TerminalFloor.GATES),
    StaticLocation("pin_fam_d25",  "D25", AmenityType.FAMILY_RESTROOM, topWall = false, floor = TerminalFloor.GATES),
    StaticLocation("pin_fam_d28",  "D28", AmenityType.FAMILY_RESTROOM, topWall = false, floor = TerminalFloor.GATES),

    // ── Level 1 · Arrivals (customs exit area near D22, far end near D40) ───
    StaticLocation("pin_rest_d22_arr", "D22", AmenityType.RESTROOM,   topWall = true,  floor = TerminalFloor.ARRIVALS),
    StaticLocation("pin_rest_d40",     "D40", AmenityType.RESTROOM,   topWall = false, floor = TerminalFloor.ARRIVALS),

    // ── Level 4 · Mezzanine (upper concourse, quieter amenities) ─────────────
    StaticLocation("pin_rest_d22_mez", "D22", AmenityType.RESTROOM,      topWall = true,  floor = TerminalFloor.MEZZANINE,
        xNudge = 0.02f),
    StaticLocation("pin_lac_d22",      "D22", AmenityType.LACTATION_ROOM, topWall = true,  floor = TerminalFloor.MEZZANINE,
        xNudge = -0.025f, yNudge = 0.07f),
)

private fun gateToXFraction(gate: String): Float {
    val num = gate.removePrefix("D").toIntOrNull() ?: return 0.5f
    return when {
        num in 5..22  -> (num - 5).toFloat() / 17f
        num in 23..40 -> 1f - (num - 23).toFloat() / 17f
        else          -> 0.5f
    }
}

// ── Public screen composable ──────────────────────────────────────────────────

/**
 * Combined Terminal D map + amenity list screen.
 * Two tabs: "Map" shows the interactive floor-plan canvas;
 * "List" shows the same filtered/sorted amenity list as AmenityListScreen.
 * Both tabs share the same ViewModel so a pin tap and a list tap lead
 * to the same AmenityDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: AmenityViewModel,
    onAmenitySelected: (Amenity) -> Unit,
    onOpenPreferences: () -> Unit,
    currentUser: com.smartamenities.data.model.User? = null,
    onSignOut: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val uiState    by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val preferences  by viewModel.preferences.collectAsState()

    val liveAmenities = remember(uiState) {
        if (uiState is AmenityUiState.Success) (uiState as AmenityUiState.Success).amenities
        else emptyList()
    }

    var selectedFloor by remember { mutableStateOf(TerminalFloor.GATES) }
    var activePin     by remember { mutableStateOf<MapPin?>(null) }
    var selectedTab   by remember { mutableIntStateOf(0) }

    // Switch to List tab whenever preferences change (user saved a filter in Settings).
    // Skip the initial composition so the screen opens on Map by default.
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(preferences) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            selectedTab = 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal D", fontWeight = FontWeight.Bold)
                        Text(
                            text = "DFW Airport",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                actions = {
                    if (preferences.requiresWheelchairAccess) {
                        Icon(Icons.Default.Accessible, contentDescription = "Wheelchair filter active",
                            tint = Color.White, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.requiresStepFreeRoute) {
                        Icon(Icons.Default.Elevator, contentDescription = "Step-free filter active",
                            tint = Color.White, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.preferFamilyRestroom) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = "Family restroom filter active",
                            tint = Color.White, modifier = Modifier.padding(end = 4.dp))
                    }
                    if (preferences.preferGenderNeutral) {
                        Icon(Icons.Default.Wc, contentDescription = "Gender-neutral filter active",
                            tint = Color.White, modifier = Modifier.padding(end = 4.dp))
                    }
                    IconButton(onClick = onOpenPreferences) {
                        Icon(Icons.Default.Settings, contentDescription = "Preferences",
                            tint = Color.White)
                    }
                    AccountIconButton(
                        currentUser      = currentUser,
                        onSignOut        = onSignOut,
                        onNavigateToAuth = onNavigateToAuth,
                        onOpenSettings   = onOpenPreferences
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.primary,
                    titleContentColor     = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Map / List tab row ────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0; activePin = null },
                    text     = { Text("Map") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = { Text("List") }
                )
            }

            // ── Tab content ───────────────────────────────────────────────────
            when (selectedTab) {
                0 -> MapTab(
                    liveAmenities = liveAmenities,
                    selectedFloor = selectedFloor,
                    activePin     = activePin.takeIf { it?.floor == selectedFloor },
                    onFloorSelected = { selectedFloor = it; activePin = null },
                    onPinTapped   = { pin ->
                        activePin = if (activePin?.amenity?.id == pin.amenity.id) null else pin
                    },
                    onNavigate    = { onAmenitySelected(it) },
                    onDismissCard = { activePin = null }
                )
                else -> ListTab(
                    uiState       = uiState,
                    selectedType  = selectedType,
                    onTypeSelected = { viewModel.selectAmenityType(it) },
                    onAmenitySelected = onAmenitySelected,
                    onRefresh     = { viewModel.triggerRefresh() }
                )
            }
        }
    }
}

// ── Map tab ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTab(
    liveAmenities: List<Amenity>,
    selectedFloor: TerminalFloor,
    activePin:     MapPin?,
    onFloorSelected: (TerminalFloor) -> Unit,
    onPinTapped:   (MapPin) -> Unit,
    onNavigate:    (Amenity) -> Unit,
    onDismissCard: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Floor selector chips
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items(TerminalFloor.entries) { floor ->
                FilterChip(
                    selected = selectedFloor == floor,
                    onClick  = { onFloorSelected(floor) },
                    label    = { Text(floor.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // One-line description of the selected floor so the label is never a mystery
        Surface(
            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text     = selectedFloor.description,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            TerminalDFloorPlan(
                liveAmenities = liveAmenities,
                selectedFloor = selectedFloor,
                activePin     = activePin,
                onPinTapped   = onPinTapped,
                modifier      = Modifier.fillMaxSize()
            )

            activePin?.let { pin ->
                PinInfoCard(
                    pin        = pin,
                    onNavigate = { onNavigate(pin.amenity) },
                    onDismiss  = onDismissCard,
                    modifier   = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }
    }
}

// ── List tab ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTab(
    uiState:          AmenityUiState,
    selectedType:     AmenityType?,
    onTypeSelected:   (AmenityType?) -> Unit,
    onAmenitySelected:(Amenity) -> Unit,
    onRefresh:        () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Type filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick  = { onTypeSelected(null) },
                    label    = { Text("All") }
                )
            }
            items(AmenityType.values()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick  = { onTypeSelected(type) },
                    label    = { Text(type.displayName) }
                )
            }
        }

        HorizontalDivider()

        when (val state = uiState) {
            is AmenityUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Finding nearby amenities…")
                    }
                }
            }
            is AmenityUiState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedType != null)
                            "No ${selectedType.displayName.lowercase()}s available"
                        else "No amenities found matching your preferences",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            is AmenityUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("Could not load amenities", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Try again") }
                    }
                }
            }
            is AmenityUiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Sorted by estimated total time (walk + wait)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(state.amenities, key = { it.id }) { amenity ->
                        AmenityListCard(amenity = amenity, onClick = { onAmenitySelected(amenity) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AmenityListCard(amenity: Amenity, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeToReachBadge(
                walkMinutes = amenity.estimatedWalkMinutes,
                crowdLevel  = amenity.crowdLevel
            )
            Spacer(Modifier.width(16.dp))
            VerticalDivider(modifier = Modifier.height(60.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = amenity.name,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text  = amenity.gateProximity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmenityStatusChip(amenity.status)
                    CrowdLevelChip(amenity.crowdLevel)
                }
                Spacer(Modifier.height(4.dp))
                AccessibilityBadgeRow(amenity)
                Spacer(Modifier.height(4.dp))
                DataFreshnessIndicator(
                    timestampMillis = amenity.dataFreshnessTimestamp,
                    confidenceScore = amenity.confidenceScore
                )
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Floor-plan canvas ─────────────────────────────────────────────────────────

@Composable
private fun TerminalDFloorPlan(
    liveAmenities: List<Amenity>,
    selectedFloor: TerminalFloor,
    activePin:     MapPin?,
    onPinTapped:   (MapPin) -> Unit,
    modifier:      Modifier = Modifier
) {
    val density = LocalDensity.current
    val gateLabelPx = with(density) { 9.sp.toPx() }
    val zoneTextPx  = with(density) { 7.5f.sp.toPx() }
    val smallTextPx = with(density) { 6.5f.sp.toPx() }
    val pinTextPx   = with(density) { 7.sp.toPx() }

    // All pins for every floor; filter to the selected floor for display
    val allPins   = remember(liveAmenities) { buildPins(liveAmenities) }
    val pins      = remember(allPins, selectedFloor) { allPins.filter { it.floor == selectedFloor } }
    // "You are here" near D22 customs exit is only relevant on Level 1 (Arrivals)
    val showYouAreHere = selectedFloor == TerminalFloor.ARRIVALS

    val mapWidth  = 920.dp
    val mapHeight = 360.dp
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .background(Color(0xFFF0F4FF))
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(mapWidth)
                .height(mapHeight)
                .pointerInput(pins) {
                    detectTapGestures { offset ->
                        val w  = size.width.toFloat()
                        val h  = size.height.toFloat()
                        val bL = w * 0.01f
                        val bW = w * 0.98f
                        val hit = pins.firstOrNull { pin ->
                            val px = bL + pin.xFraction * bW
                            val py = pin.yFraction * h
                            sqrt((offset.x - px) * (offset.x - px) +
                                 (offset.y - py) * (offset.y - py)) < 42f
                        }
                        hit?.let { onPinTapped(it) }
                    }
                }
        ) {
            val w  = size.width
            val h  = size.height
            val bL = w * 0.01f
            val bR = w * 0.99f
            val bW = bR - bL

            // ── Layout constants (horseshoe / U-shape) ────────────────────────
            val gStubH    = h * 0.11f           // gate stub protrusion (~40dp)
            val topArmT   = h * 0.15f           // top edge of top concourse arm
            val topArmB   = h * 0.42f           // bottom edge of top arm
            val botArmT   = h * 0.58f           // top edge of bottom arm
            val botArmB   = h * 0.85f           // bottom edge of bottom arm
            val eastX     = bL + bW * 0.84f     // east connector starts near D20–D22
            val gSlotW    = bW / 22f            // x-slot width per gate number
            val gHalfW    = gSlotW * 0.34f      // drawn stub half-width (~28dp)
            val topStubTop = topArmT - gStubH
            val corrMidY  = (topArmB + botArmT) / 2f

            // ── Background — outside terminal ─────────────────────────────────
            drawRect(Color(0xFFE8E8E0), topLeft = Offset(0f, 0f), size = Size(w, h))

            // Subtle taxiway curves at bottom of canvas for airport context
            drawTaxiwayLines(w, h)

            // ── Terminal floor fill (3 rects form the U / horseshoe shape) ────
            val floorColor = Color(0xFFF5F5F0)
            // Top concourse arm
            drawRect(floorColor, topLeft = Offset(bL, topArmT), size = Size(bW, topArmB - topArmT))
            // Bottom concourse arm
            drawRect(floorColor, topLeft = Offset(bL, botArmT), size = Size(bW, botArmB - botArmT))
            // East connector hall (links both arms on the right side)
            drawRect(floorColor, topLeft = Offset(eastX, topArmB),
                size = Size(bR - eastX, botArmT - topArmB))

            // ── Corridor (white walkway between the two arms) ─────────────────
            drawRect(Color(0xFFFFFFFF),
                topLeft = Offset(bL, topArmB),
                size    = Size(eastX - bL, botArmT - topArmB))

            // ── Terminal U-shape outline ──────────────────────────────────────
            val uPath = Path().apply {
                moveTo(bL, topArmT)
                lineTo(bR, topArmT)
                lineTo(bR, botArmB)
                lineTo(bL, botArmB)
                lineTo(bL, botArmT)
                lineTo(eastX, botArmT)
                lineTo(eastX, topArmB)
                lineTo(bL, topArmB)
                close()
            }
            drawPath(uPath, Color(0xFF9E9E9E), style = Stroke(width = 3f))

            // ── Skylink 1 zone (D11–D20, top arm) ────────────────────────────
            val sl1X1   = bL + (11 - 5).toFloat() / 17f * bW
            val sl1X2   = bL + (20 - 5).toFloat() / 17f * bW
            val slHTop  = (topArmB - topArmT) * 0.5f
            val slMidTop = (topArmT + topArmB) / 2f
            drawSkylink(sl1X1, slMidTop - slHTop / 2f, sl1X2 - sl1X1, slHTop,
                (sl1X1 + sl1X2) / 2f, slMidTop + 4f, zoneTextPx)

            // ── Skylink 2 zone (D24–D34, bottom arm) ─────────────────────────
            val sl2X1   = bL + (1f - (34 - 23).toFloat() / 17f) * bW
            val sl2X2   = bL + (1f - (24 - 23).toFloat() / 17f) * bW
            val slHBot  = (botArmB - botArmT) * 0.5f
            val slMidBot = (botArmT + botArmB) / 2f
            drawSkylink(sl2X1, slMidBot - slHBot / 2f, sl2X2 - sl2X1, slHBot,
                (sl2X1 + sl2X2) / 2f, slMidBot + 4f, zoneTextPx)

            // ── Top gate stubs (D5–D22, protrude north from top arm) ──────────
            for (n in 5..22) {
                val gx = bL + (n - 5).toFloat() / 17f * bW
                drawRect(Color(0xFFEEEEEE),
                    topLeft = Offset(gx - gHalfW, topStubTop),
                    size    = Size(gHalfW * 2f, gStubH))
                drawRect(Color(0xFF9E9E9E),
                    topLeft = Offset(gx - gHalfW, topStubTop),
                    size    = Size(gHalfW * 2f, gStubH),
                    style   = Stroke(1.5f))
                drawGateLabelInStub("D$n", gx, topStubTop + gStubH * 0.55f, gateLabelPx)
            }

            // ── Bottom gate stubs (D23–D40, protrude south from bottom arm) ───
            for (n in 23..40) {
                val gx = bL + (1f - (n - 23).toFloat() / 17f) * bW
                drawRect(Color(0xFFEEEEEE),
                    topLeft = Offset(gx - gHalfW, botArmB),
                    size    = Size(gHalfW * 2f, gStubH))
                drawRect(Color(0xFF9E9E9E),
                    topLeft = Offset(gx - gHalfW, botArmB),
                    size    = Size(gHalfW * 2f, gStubH),
                    style   = Stroke(1.5f))
                drawGateLabelInStub("D$n", gx, botArmB + gStubH * 0.55f, gateLabelPx)
            }

            // ── Security checkpoints (along corridor) ─────────────────────────
            val corrSpan = botArmT - topArmB
            drawSecurityMarker(bL + (18 - 5).toFloat() / 17f * bW,
                corrMidY - corrSpan * 0.15f, 13f, smallTextPx)
            drawSecurityMarker(bL + (1f - (30 - 23).toFloat() / 17f) * bW,
                corrMidY + corrSpan * 0.15f, 13f, smallTextPx)

            // ── Customs / Immigration zone (east connector) ───────────────────
            val cusX = eastX
            val cusW = bR - eastX
            drawRect(Color(0xFFE8F5E9), topLeft = Offset(cusX, topArmB),
                size = Size(cusW, botArmT - topArmB))
            drawRect(Color(0xFF43A047), topLeft = Offset(cusX, topArmB),
                size = Size(cusW, botArmT - topArmB), style = Stroke(2f))
            drawZoneLabel("CUSTOMS/",    cusX + cusW / 2f, corrMidY - 7f,  zoneTextPx,
                android.graphics.Color.argb(255, 27, 94, 32))
            drawZoneLabel("IMMIGRATION", cusX + cusW / 2f, corrMidY + 9f, zoneTextPx,
                android.graphics.Color.argb(255, 27, 94, 32))

            // ── "You are here" dot (Level 1 · Arrivals only) ─────────────────
            if (showYouAreHere) {
                val youX = bL + (17f / 17f) * bW - w * 0.035f
                val youY = corrMidY
                drawCircle(Color(0x330052A5), 24f, Offset(youX, youY))
                drawCircle(Color(0xFF0052A5), 13f, Offset(youX, youY))
                drawCircle(Color.White,        6f, Offset(youX, youY))
                drawZoneLabel("You are here", youX, youY + 28f, smallTextPx,
                    android.graphics.Color.argb(255, 0, 82, 165))
            }

            // ── Amenity pins ──────────────────────────────────────────────────
            pins.forEach { pin ->
                val px = bL + pin.xFraction * bW
                val py = pin.yFraction * h
                drawAmenityPin(pin.amenity, px, py,
                    isSelected = activePin?.amenity?.id == pin.amenity.id, pinTextPx)
            }
        }
    }
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

// Draw gate label centred inside the gate stub box
private fun DrawScope.drawGateLabelInStub(label: String, cx: Float, cy: Float, textSizePx: Float) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 21, 101, 192)  // #1565C0 DFW blue
            textSize = textSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(label, cx, cy, paint)
    }
}

private fun DrawScope.drawSkylink(
    x: Float, y: Float, w: Float, h: Float,
    labelX: Float, labelY: Float, textSizePx: Float
) {
    drawRect(Color(0xFFFFF3E0), topLeft = Offset(x, y), size = Size(w, h))
    drawRect(Color(0xFFFFB300), topLeft = Offset(x, y), size = Size(w, h), style = Stroke(2f))
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 230, 81, 0)
            textSize = textSizePx; textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true; isAntiAlias = true
        }
        canvas.nativeCanvas.drawText("SKYLINK", labelX, labelY, paint)
    }
}

private fun DrawScope.drawSecurityMarker(cx: Float, cy: Float, radius: Float, textSizePx: Float) {
    drawCircle(Color(0xFFFFCDD2), radius, Offset(cx, cy))
    drawCircle(Color(0xFFC62828), radius, Offset(cx, cy), style = Stroke(2f))
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 198, 40, 40)
            textSize = textSizePx; textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true; isAntiAlias = true
        }
        canvas.nativeCanvas.drawText("SEC", cx, cy + 3f, paint)
        canvas.nativeCanvas.drawText("Security", cx, cy + radius + 13f, paint)
    }
}

private fun DrawScope.drawZoneLabel(
    text: String, cx: Float, cy: Float, textSizePx: Float, argbColor: Int
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = argbColor; textSize = textSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true; isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(text, cx, cy, paint)
    }
}

private fun DrawScope.drawAmenityPin(
    amenity: Amenity, px: Float, py: Float, isSelected: Boolean, textSizePx: Float
) {
    val statusColor = when (amenity.status) {
        AmenityStatus.OPEN           -> Color(0xFF2E7D32)
        AmenityStatus.CLOSED         -> Color(0xFFC62828)
        AmenityStatus.OUT_OF_SERVICE -> Color(0xFFE65100)
        AmenityStatus.UNKNOWN        -> Color(0xFF757575)
    }
    val crowdColor = when (amenity.crowdLevel) {
        CrowdLevel.EMPTY   -> Color(0xFF1565C0)
        CrowdLevel.SHORT   -> Color(0xFF2E7D32)
        CrowdLevel.MEDIUM  -> Color(0xFFF9A825)
        CrowdLevel.LONG    -> Color(0xFFC62828)
        CrowdLevel.UNKNOWN -> Color(0xFF757575)
    }
    val pinR = if (isSelected) 16f else 12f
    val tipY = py + pinR * 1.5f  // tip of teardrop points down from head center

    // Drop shadow under the head
    drawCircle(Color(0x40000000), pinR + 2f, Offset(px + 2f, py + 2f))

    // Teardrop triangle (tip pointing down)
    val tipPath = Path().apply {
        moveTo(px - pinR * 0.6f, py + pinR * 0.45f)
        lineTo(px + pinR * 0.6f, py + pinR * 0.45f)
        lineTo(px, tipY)
        close()
    }
    drawPath(tipPath, statusColor)

    // Circular head (filled + white ring)
    drawCircle(statusColor, pinR, Offset(px, py))
    drawCircle(Color.White, pinR, Offset(px, py),
        style = Stroke(if (isSelected) 3f else 2f))

    // Crowd dot (top-right of head)
    val dotOff = pinR * 0.72f
    drawCircle(crowdColor, 4.5f, Offset(px + dotOff, py - dotOff))
    drawCircle(Color.White, 4.5f, Offset(px + dotOff, py - dotOff), style = Stroke(1f))

    val label = when (amenity.type) {
        AmenityType.RESTROOM                -> "WC"
        AmenityType.FAMILY_RESTROOM         -> "F"
        AmenityType.LACTATION_ROOM          -> "L"
        AmenityType.GENDER_NEUTRAL_RESTROOM -> "GN"
        AmenityType.WATER_FOUNTAIN          -> "W"
    }
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = textSizePx; textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true; isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(label, px, py + textSizePx * 0.38f, paint)
    }
}

// ── Taxiway lines (bottom of canvas, airport context) ────────────────────────

private fun DrawScope.drawTaxiwayLines(w: Float, h: Float) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(100, 158, 158, 158)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
        }
        val path1 = android.graphics.Path().apply {
            moveTo(w * 0.05f, h * 0.975f)
            cubicTo(w * 0.25f, h * 0.935f, w * 0.75f, h * 0.935f, w * 0.95f, h * 0.975f)
        }
        canvas.nativeCanvas.drawPath(path1, paint)
        val path2 = android.graphics.Path().apply {
            moveTo(w * 0.10f, h * 0.998f)
            cubicTo(w * 0.30f, h * 0.958f, w * 0.70f, h * 0.958f, w * 0.90f, h * 0.998f)
        }
        canvas.nativeCanvas.drawPath(path2, paint)
    }
}

// ── Pin builder ───────────────────────────────────────────────────────────────

private fun buildPins(liveAmenities: List<Amenity>): List<MapPin> =
    STATIC_LOCATIONS.map { loc ->
        val xBase = gateToXFraction(loc.gate)
        val yBase = if (loc.topWall) 0.23f else 0.77f
        val match = liveAmenities.firstOrNull { a ->
            a.type == loc.type && a.gateProximity.contains(loc.gate)
        }
        val floorNumber = when (loc.floor) {
            TerminalFloor.ARRIVALS  -> 1
            TerminalFloor.GATES     -> 3
            TerminalFloor.MEZZANINE -> 4
        }
        val amenity = match ?: Amenity(
            id                     = loc.id,
            name                   = "${loc.type.displayName} near ${loc.gate}",
            type                   = loc.type,
            floor                  = floorNumber,
            locationX              = xBase + loc.xNudge,
            locationY              = yBase + loc.yNudge,
            status                 = AmenityStatus.OPEN,
            crowdLevel             = CrowdLevel.SHORT,
            estimatedWalkMinutes   = 3,
            isWheelchairAccessible = true,
            isStepFreeRoute        = true,
            isFamilyRestroom       = loc.type == AmenityType.FAMILY_RESTROOM,
            isGenderNeutral        = false,
            dataFreshnessTimestamp = System.currentTimeMillis(),
            confidenceScore        = 0.9f,
            gateProximity          = "Near Gate ${loc.gate}"
        )
        MapPin(amenity, xBase + loc.xNudge, yBase + loc.yNudge, floor = loc.floor)
    }

// ── Pin info card overlay ─────────────────────────────────────────────────────

@Composable
private fun PinInfoCard(
    pin:       MapPin,
    onNavigate: () -> Unit,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier
) {
    val a = pin.amenity
    val totalMinutes = a.estimatedWalkMinutes + a.crowdLevel.waitEstimateMinutes
    val statusColor = when (a.status) {
        AmenityStatus.OPEN           -> StatusOpen
        AmenityStatus.CLOSED         -> StatusClosed
        AmenityStatus.OUT_OF_SERVICE -> StatusOutOfService
        AmenityStatus.UNKNOWN        -> StatusUnknown
    }
    val crowdColor = when (a.crowdLevel) {
        CrowdLevel.EMPTY   -> Color(0xFF1565C0)
        CrowdLevel.SHORT   -> CrowdShort
        CrowdLevel.MEDIUM  -> CrowdMedium
        CrowdLevel.LONG    -> CrowdLong
        CrowdLevel.UNKNOWN -> CrowdUnknown
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = a.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text  = a.gateProximity,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                PinBadge(a.status.displayName, statusColor)
                PinBadge(a.crowdLevel.displayName, crowdColor)
                Text(
                    text       = "$totalMinutes min",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onNavigate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View details")
            }
        }
    }
}

@Composable
private fun PinBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
