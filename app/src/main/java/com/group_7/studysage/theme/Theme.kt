package com.group_7.studysage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- 1. DEFINE YOUR NEW PURPLE PALETTE ---

// Your primary color, a deep, rich purple
val PurplePrimary = Color(0xFF652497)
// A more vibrant purple for secondary actions
val PurpleSecondary = Color(0xFF9333EA)
// A contrasting gold for XP and highlights
val PurpleTertiary = Color(0xFFFBBF24)
// A soft, light purple for backgrounds in light mode
val PurpleSoft = Color(0xFFF5F3FF)

// --- Light Theme Colors ---
val LightBackground = Color(0xFFFCFCFF) // Clean white
val LightSurface = Color(0xFFFFFFFF)    // Pure white for cards
val TextOnLight = Color(0xFF1B1921)     // Very dark purple-black text
val TextOnLightMuted = Color(0xFF6B7280) // Readable grey for subtext

// --- Dark Theme Colors ---
val DarkBackground = Color(0xFF1A1721) // Very dark purple
val DarkSurface = Color(0xFF2C2A3A)   // Dark purple-grey for cards
val TextOnDark = Color(0xFFF5F3FF)      // Off-white text
val TextOnDarkMuted = Color(0xFF9E9BAC)   // Light grey-purple for subtext

// --- 2. DEFINE YOUR CUSTOM NAVBAR COLORS (matches the theme) ---

// For Dark Mode
val DarkNavContainer = Color(0xFF2C2A3A).copy(alpha = 0.99f) // Dark purple glass
val DarkNavIndicator = Color(0xFF9333EA).copy(alpha = 0.4f) // Translucent (brighter) primary glass
val DarkNavSelected = Color.White
val DarkNavUnselected = TextOnDarkMuted // Light grey-purple

// For Light Mode
val LightNavContainer = Color.White.copy(alpha = 0.95f) // Light frosted glass
val LightNavIndicator = Color(0xFF652497).copy(alpha = 0.15f) // Very subtle purple pill
val LightNavSelected = PurplePrimary // Solid dark purple
val LightNavUnselected = TextOnLightMuted // Solid readable grey

// --- 3. CREATE THE APP'S COLOR SCHEMES ---

private val DarkColorScheme = darkColorScheme(
    primary = PurpleSecondary, // <-- CHANGED: Use the BRIGHTER purple as primary
    secondary = PurplePrimary, // <-- CHANGED: Use the DARKER purple as secondary
    tertiary = PurpleTertiary,
    background = DarkBackground,
    surface = DarkSurface, // Card backgrounds
    surfaceVariant = DarkSurface, // Inner card backgrounds
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextOnDark, // Main text
    onSurface = TextOnDark,    // Text on cards
    onSurfaceVariant = TextOnDarkMuted // Muted text
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary, // Keep dark purple as primary in light mode
    secondary = PurpleSecondary,
    tertiary = PurpleTertiary,
    background = LightBackground,
    surface = LightSurface, // Card backgrounds (white)
    surfaceVariant = PurpleSoft,  // Inner card backgrounds (soft purple)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = TextOnLight,
    onBackground = TextOnLight, // Main text (dark)
    onSurface = TextOnLight,    // Text on cards (dark)
    onSurfaceVariant = TextOnLightMuted // Muted text (grey)
)

// --- 4. CREATE YOUR THEME COMPOSABLE ---

@Composable
fun StudySageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assumes you have Typography.kt
        content = content
    )
}