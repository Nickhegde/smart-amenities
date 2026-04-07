package com.smartamenities.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartamenities.data.graph.TerminalDGraph
import com.smartamenities.data.local.MockAmenityDataSource
import com.smartamenities.data.model.AdminSimulationState
import com.smartamenities.data.model.Amenity
import com.smartamenities.data.model.AmenitySimulationOverride
import com.smartamenities.data.model.AmenityStatus
import com.smartamenities.data.model.CrowdLevel
import com.smartamenities.data.model.SimulationConfig
import com.smartamenities.data.model.SimulationLocation
import com.smartamenities.data.model.SimulationPreset
import com.smartamenities.viewmodel.AmenityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSimulatorScreen(
    viewModel: AmenityViewModel,
    onBack: () -> Unit
) {
    val adminState by viewModel.adminState.collectAsState()
    val currentUserNode by viewModel.userNode.collectAsState()
    var averageUsageInput by remember { mutableStateOf(adminState.config.averageUsageTimeMinutes.toString()) }
    var draftLocation by remember { mutableStateOf(adminState.config.selectedLocation) }
    var draftCrowd by remember { mutableStateOf(adminState.config.crowdLevel.toFloat()) }
    var systemOpen by remember { mutableStateOf(adminState.config.isSystemOpen) }

    LaunchedEffect(adminState.config) {
        averageUsageInput = adminState.config.averageUsageTimeMinutes.toString()
        draftLocation = adminState.config.selectedLocation
        draftCrowd = adminState.config.crowdLevel.toFloat()
        systemOpen = adminState.config.isSystemOpen
    }

    val visibleAmenities = remember(adminState) {
        adminState.amenities.filter { amenity ->
            adminState.config.selectedLocation == SimulationLocation.TERMINAL_D_ALL ||
                MockAmenityDataSource.getSimulationLocation(amenity) == adminState.config.selectedLocation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Simulator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Set zone-level crowd and usage conditions, or override individual amenity states. Backend API support for zone controls coming soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                AdminSummaryCard(adminState = adminState)
            }

            item {
                UserLocationCard(
                    currentNode = currentUserNode,
                    onNodeSelected = { viewModel.updateUserNode(it) }
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Zone Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Controls apply to all amenities in the selected zone. Backend API wiring pending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LocationDropdown(
                            selectedLocation = draftLocation,
                            onLocationSelected = { draftLocation = it }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Crowd level: ${crowdLabel(draftCrowd.toInt())}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Sets the crowd level for all amenities in the selected zone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = draftCrowd,
                                onValueChange = { draftCrowd = it },
                                valueRange = 0f..3f,
                                steps = 2
                            )
                        }

                        OutlinedTextField(
                            value = averageUsageInput,
                            onValueChange = { averageUsageInput = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Avg. usage time per person (minutes)") },
                            supportingText = { Text("Applied to all amenities in the selected zone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        ToggleRow(
                            label = if (systemOpen) "Zone open" else "Zone closed (all amenities closed)",
                            checked = systemOpen,
                            onCheckedChange = { systemOpen = it }
                        )

                        Button(
                            onClick = {
                                viewModel.updateSimulationConfig(
                                    SimulationConfig(
                                        selectedLocation = draftLocation,
                                        gateCount = adminState.config.gateCount,
                                        crowdLevel = draftCrowd.toInt(),
                                        averageUsageTimeMinutes = averageUsageInput.toIntOrNull()
                                            ?: adminState.config.averageUsageTimeMinutes,
                                        isSystemOpen = systemOpen,
                                        isSimulationModeEnabled = true
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply to zone")
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Preset Scenarios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        var selectedPreset by remember { mutableStateOf<SimulationPreset?>(null) }

                        SimulationPreset.entries.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            if (isSelected) {
                                Button(
                                    onClick = { selectedPreset = preset },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(preset.displayName)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedPreset = preset },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(preset.displayName)
                                }
                            }
                        }

                        if (selectedPreset != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "\"${selectedPreset!!.displayName}\" selected — tap Apply to activate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.applySimulationPreset(selectedPreset!!)
                                    selectedPreset = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply preset")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Amenity Overrides",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(visibleAmenities, key = { it.id }) { amenity ->
                AmenityOverrideCard(
                    amenity = amenity,
                    override = adminState.overrides[amenity.id],
                    onUpdate = { status, crowd ->
                        viewModel.updateAmenityOverride(
                            amenityId = amenity.id,
                            status = status,
                            crowdLevel = crowd
                        )
                    },
                    onReset = { viewModel.clearAmenityOverride(amenity.id) }
                )
            }
        }
    }
}

@Composable
private fun AdminSummaryCard(adminState: AdminSimulationState) {
    val openCount = adminState.amenities.count { it.status == AmenityStatus.OPEN }
    val unavailableCount = adminState.amenities.count {
        it.status == AmenityStatus.OUT_OF_SERVICE || it.status == AmenityStatus.CLOSED
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Control Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Location: ${adminState.config.selectedLocation.displayName}")
            Text("Open amenities: $openCount")
            Text("Unavailable amenities: $unavailableCount")
            Text("Overrides active: ${adminState.overrides.size}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdown(
    selectedLocation: SimulationLocation,
    onLocationSelected: (SimulationLocation) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLocation.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Location") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SimulationLocation.entries.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.displayName) },
                    onClick = {
                        onLocationSelected(location)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmenityOverrideCard(
    amenity: Amenity,
    override: AmenitySimulationOverride?,
    onUpdate: (AmenityStatus?, CrowdLevel?) -> Unit,
    onReset: () -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var crowdExpanded by remember { mutableStateOf(false) }

    // Draft state — selections are staged here until admin taps Save
    var draftStatus by remember(amenity.status) { mutableStateOf(amenity.status) }
    var draftCrowd by remember(amenity.crowdLevel) { mutableStateOf(amenity.crowdLevel) }

    val hasPendingChanges = draftStatus != amenity.status || draftCrowd != amenity.crowdLevel

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = amenity.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = amenity.gateProximity,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status dropdown — stages selection in draftStatus, does not save yet
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it }
            ) {
                OutlinedTextField(
                    value = draftStatus.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    AmenityStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayName) },
                            onClick = {
                                draftStatus = status
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            // Crowd dropdown — stages selection in draftCrowd, does not save yet
            ExposedDropdownMenuBox(
                expanded = crowdExpanded,
                onExpandedChange = { crowdExpanded = it }
            ) {
                OutlinedTextField(
                    value = draftCrowd.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Crowd level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = crowdExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = crowdExpanded, onDismissRequest = { crowdExpanded = false }) {
                    listOf(CrowdLevel.EMPTY, CrowdLevel.SHORT, CrowdLevel.MEDIUM, CrowdLevel.LONG, CrowdLevel.UNKNOWN)
                        .forEach { crowdLevel ->
                            DropdownMenuItem(
                                text = { Text(crowdLevel.displayName) },
                                onClick = {
                                    draftCrowd = crowdLevel
                                    crowdExpanded = false
                                }
                            )
                        }
                }
            }

            if (hasPendingChanges) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Unsaved changes — tap Save to apply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save — commits draft to backend (currently wired to mock; backend API next)
                Button(
                    onClick = { onUpdate(draftStatus, draftCrowd) },
                    enabled = hasPendingChanges,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }

                // Reset — reverts drafts and clears any active override
                TextButton(
                    onClick = {
                        draftStatus = amenity.status
                        draftCrowd = amenity.crowdLevel
                        onReset()
                    }
                ) {
                    Icon(Icons.Default.LockReset, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}

private fun crowdLabel(level: Int): String = when (level.coerceIn(0, 3)) {
    0 -> "Empty"
    1 -> "Light"
    2 -> "Moderate"
    else -> "Heavy"
}

// ── User location nodes available for selection (gates + corridor + skylink) ────
// Amenity leaf nodes (REST_*, FAM_*, LAC_*) are excluded — users stand at gates/corridors.
private val USER_LOCATION_NODES: List<String> by lazy {
    TerminalDGraph.nodes
        .map { it.id }
        .filter { id ->
            !id.startsWith("REST_") && !id.startsWith("FAM_") && !id.startsWith("LAC_")
        }
        .sorted()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserLocationCard(
    currentNode: String,
    onNodeSelected: (String) -> Unit
) {
    // Draft — staged locally until admin taps Save
    var draftNode by remember(currentNode) { mutableStateOf(currentNode) }
    val hasPendingChange = draftNode != currentNode

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Simulated User Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sets the starting node for route recommendations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            NodeDropdown(
                selectedNode = draftNode,
                nodes = USER_LOCATION_NODES,
                onNodeSelected = { draftNode = it }
            )

            if (hasPendingChange) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Location changed to $draftNode — tap Save to apply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Button(
                onClick = { onNodeSelected(draftNode) },
                enabled = hasPendingChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save location")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeDropdown(
    selectedNode: String,
    nodes: List<String>,
    onNodeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedNode,
            onValueChange = {},
            readOnly = true,
            label = { Text("Starting node") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = { Text(node) },
                    onClick = {
                        onNodeSelected(node)
                        expanded = false
                    }
                )
            }
        }
    }
}
