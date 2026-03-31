package com.smartamenities.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartamenities.data.model.User

/**
 * Top-bar account button — shows a direct action button (no dropdown):
 *
 *  • Registered user  → "Sign Out" text button (red); tap shows confirmation dialog
 *  • Guest / not logged in → "Sign In" text button; tap calls onSignOut which clears
 *    the guest session before navigating to the auth screen
 */
@Composable
fun AccountIconButton(
    currentUser:      User?,
    onSignOut:        () -> Unit,
    onNavigateToAuth: () -> Unit,
    onOpenSettings:   () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon  = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Sign out?") },
            text  = { Text("You'll be taken back to the sign-in screen.") },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onSignOut() },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (currentUser == null || currentUser.isGuest) {
        // Guest / unauthenticated — must call onSignOut (not onNavigateToAuth) so the
        // guest session is cleared before AuthScreen loads; otherwise AuthUiState stays
        // Success and AuthScreen immediately bounces back to Home.
        TextButton(
            onClick = onSignOut,
            colors  = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Sign In")
        }
    } else {
        TextButton(
            onClick = { showConfirm = true },
            colors  = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Sign Out")
        }
    }
}
