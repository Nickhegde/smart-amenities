package com.smartamenities.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartamenities.ui.theme.DfwBlue
import com.smartamenities.ui.theme.DfwBlueDark
import com.smartamenities.ui.theme.DfwOrange
import com.smartamenities.viewmodel.AuthUiState
import com.smartamenities.viewmodel.AuthViewModel

/**
 * Landing / welcome screen.
 *
 * Three paths:
 *  1. Sign In  → LoginScreen
 *  2. Create Account → SignUpScreen
 *  3. Continue as Guest → skips auth, loads the app with default preferences
 *
 * If a persisted session already exists (app was not force-quit or logged out)
 * this screen is never shown — MainActivity sets the start destination to Home.
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onLoginClick:   () -> Unit,
    onSignUpClick:  () -> Unit,
    onGuestSuccess: () -> Unit   // called after guest session is created
) {
    val state by authViewModel.state.collectAsState()

    // Guest creation is async — navigate as soon as Success arrives
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onGuestSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DfwBlueDark, DfwBlue, Color(0xFF1565C0))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.18f))

            // ── Logo area ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "DFW",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "SmartAmenities",
                style      = MaterialTheme.typography.headlineLarge,
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = "Terminal D · DFW Airport",
                style     = MaterialTheme.typography.titleSmall,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Tagline
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text      = "Find restrooms, family rooms & more\nwith real-time status & turn-by-turn directions",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.weight(0.25f))

            // ── Action buttons ────────────────────────────────────────────────
            Button(
                onClick  = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DfwOrange,
                    contentColor   = Color.White
                )
            ) {
                Text("Create Account", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick  = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Text("Sign In", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(
                color     = Color.White.copy(alpha = 0.25f),
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Guest option
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
            } else {
                TextButton(onClick = { authViewModel.continueAsGuest() }) {
                    Text(
                        "Continue as Guest",
                        style  = MaterialTheme.typography.bodyMedium,
                        color  = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text  = "Guest mode uses default accessibility settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(0.1f))
        }
    }
}
