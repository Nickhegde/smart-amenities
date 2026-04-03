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
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var gateCountInput by remember { mutableStateOf(adminState.config.gateCount.toString()) }
    var averageUsageInput by remember { mutableStateOf(adminState.config.averageUsageTimeMinutes.toString()) }
    var draftLocation by remember { mutableStateOf(adminState.config.selectedLocation) }
    var draftCrowd by remember { mutableStateOf(adminState.config.crowdLevel.toFloat()) }
    var systemOpen by remember { mutableStateOf(adminState.config.isSystemOpen) }
    var simulationEnabled by remember { mutableStateOf(adminState.config.isSimulationModeEnabled) }

    LaunchedEffect(adminState.config) {
        gateCountInput = adminState.config.gateCount.toString()
        averageUsageInput = adminState.config.averageUsageTimeMinutes.toString()
        draftLocation = adminState.config.selectedLocation
        draftCrowd = adminState.config.crowdLevel.toFloat()
        systemOpen = adminState.config.isSystemOpen
        simulationEnabled = adminState.config.isSimulationModeEnabled
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
                    text = "Adjust live Terminal D conditions, load scenarios, and override individual restroom states.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                AdminSummaryCard(adminState = adminState)
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
                            text = "Simulation Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        LocationDropdown(
                            selectedLocation = draftLocation,
                            onLocationSelected = { draftLocation = it }
                        )

                        OutlinedTextField(
                            value = gateCountInput,
                            onValueChange = { gateCountInput = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Gate count") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Crowd level: ${crowdLabel(draftCrowd.toInt())}",
                                style = MaterialTheme.typography.labelLarge
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
                            label = { Text("Average usage time (minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        ToggleRow(
                            label = if (systemOpen) "System open" else "System closed",
                            checked = systemOpen,
                            onCheckedChange = { systemOpen = it }
                        )

                        ToggleRow(
                            label = if (simulationEnabled) "Simulation mode enabled" else "Simulation mode disabled",
                            checked = simulationEnabled,
                            onCheckedChange = { simulationEnabled = it }
                        )

                        Button(
                            onClick = {
                                viewModel.updateSimulationConfig(
                                    SimulationConfig(
                                        selectedLocation = draftLocation,
                                        gateCount = gateCountInput.toIntOrNull() ?: adminState.config.gateCount,
                                        crowdLevel = draftCrowd.toInt(),
                                        averageUsageTimeMinutes = averageUsageInput.toIntOrNull()
                                            ?: adminState.config.averageUsageTimeMinutes,
                                        isSystemOpen = systemOpen,
                                        isSimulationModeEnabled = simulationEnabled
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply controls")
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
                        SimulationPreset.entries.forEach { preset ->
                            OutlinedButton(
                                onClick = { viewModel.applySimulationPreset(preset) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(preset.displayName)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Restroom Overrides",
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

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it }
            ) {
                OutlinedTextField(
                    value = amenity.status.displayName,
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
                                onUpdate(status, null)
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = crowdExpanded,
                onExpandedChange = { crowdExpanded = it }
            ) {
                OutlinedTextField(
                    value = amenity.crowdLevel.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Crowd") },
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
                                    onUpdate(null, crowdLevel)
                                    crowdExpanded = false
                                }
                            )
                        }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (override == null) "Using global simulation values" else "Custom override active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onReset) {
                    Icon(Icons.Default.LockReset, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
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
