package com.smartamenities.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colors ──────────────────────────────────────────────────────────────
// Using DFW's blue/orange palette as a starting point

val DfwBlue = Color(0xFF0052A5)
val DfwBlueDark = Color(0xFF003A7A)
val DfwOrange = Color(0xFFE87722)
val DfwOrangeDark = Color(0xFFB55B0F)

// Semantic status colors (used by AmenityStatusChip)
val StatusOpen = Color(0xFF2E7D32)         // Green
val StatusClosed = Color(0xFFC62828)       // Red
val StatusOutOfService = Color(0xFFE65100) // Deep orange
val StatusUnknown = Color(0xFF757575)      // Gray

// Crowd level colors
val CrowdShort = Color(0xFF2E7D32)
val CrowdMedium = Color(0xFFF9A825)
val CrowdLong = Color(0xFFC62828)
val CrowdUnknown = Color(0xFF757575)

// ── Light theme ───────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = DfwBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = DfwBlueDark,
    secondary = DfwOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = DfwOrangeDark,
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    background = Color(0xFFF6F8FF),
)

// ── Dark theme ────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BBEFF),
    onPrimary = Color(0xFF002F6B),
    primaryContainer = DfwBlueDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = DfwOrange,
    onSecondary = Color(0xFF4A2600),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF141218),
)

@Composable
fun SmartAmenitiesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),          // Material 3 defaults — customize later
        content = content
    )
}
