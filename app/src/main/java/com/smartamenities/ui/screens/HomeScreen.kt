package com.smartamenities.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartamenities.data.model.User
import com.smartamenities.ui.components.AccountIconButton

private data class TerminalInfo(
    val name: String,
    val label: String,
    val isActive: Boolean
)

private val terminals = listOf(
    TerminalInfo("A", "Terminal A", isActive = false),
    TerminalInfo("B", "Terminal B", isActive = false),
    TerminalInfo("C", "Terminal C", isActive = false),
    TerminalInfo("D", "Terminal D", isActive = true),
    TerminalInfo("E", "Terminal E", isActive = false),
)

private const val ADMIN_PASSCODE = "DFW-ADMIN"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTerminalSelected: (String) -> Unit,
    onAdminAuthenticated: () -> Unit,
    currentUser: User? = null,
    onSignOut: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPasscode by remember { mutableStateOf("") }
    var showAuthError by remember { mutableStateOf(false) }

    fun openAdminDialog() {
        if (currentUser == null || currentUser.isGuest) return
        showAdminDialog = true
        showAuthError = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DFW Airport",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    AccountIconButton(
                        currentUser = currentUser,
                        onSignOut = onSignOut,
                        onNavigateToAuth = onNavigateToAuth,
                        onOpenSettings = onOpenSettings
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Select a Terminal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Choose your terminal to find nearby amenities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(terminals) { terminal ->
                        TerminalCard(
                            terminal = terminal,
                            onClick = { if (terminal.isActive) onTerminalSelected(terminal.name) },
                            onLongPress = {
                                if (terminal.name == "D") openAdminDialog()
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .graphicsLayer(alpha = 0.01f)
                    .combinedClickable(onClick = { openAdminDialog() })
            )
        }

        if (showAdminDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAdminDialog = false
                    adminPasscode = ""
                    showAuthError = false
                },
                title = { Text("Admin Access") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter the admin passcode to open the simulator.")
                        OutlinedTextField(
                            value = adminPasscode,
                            onValueChange = {
                                adminPasscode = it
                                if (showAuthError) showAuthError = false
                            },
                            singleLine = true,
                            label = { Text("Passcode") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        if (showAuthError) {
                            Text(
                                text = "Incorrect passcode.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (adminPasscode == ADMIN_PASSCODE) {
                            showAdminDialog = false
                            adminPasscode = ""
                            onAdminAuthenticated()
                        } else {
                            showAuthError = true
                        }
                    }) {
                        Text("Enter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAdminDialog = false
                        adminPasscode = ""
                        showAuthError = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalCard(
    terminal: TerminalInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val containerColor = if (terminal.isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (terminal.isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                enabled = terminal.isActive,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (terminal.isActive) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = terminal.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = terminal.label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (!terminal.isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
