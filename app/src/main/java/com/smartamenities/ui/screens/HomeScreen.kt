package com.smartamenities.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTerminalSelected: (String) -> Unit,
    currentUser: User? = null,
    onSignOut: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
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
                        currentUser     = currentUser,
                        onSignOut       = onSignOut,
                        onNavigateToAuth = onNavigateToAuth,
                        onOpenSettings  = onOpenSettings
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        onClick = { if (terminal.isActive) onTerminalSelected(terminal.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalCard(terminal: TerminalInfo, onClick: () -> Unit) {
    val containerColor = if (terminal.isActive)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (terminal.isActive)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Card(
        onClick = onClick,
        enabled = terminal.isActive,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
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
