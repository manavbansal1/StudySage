package com.group_7.studysage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- 1. DEFINE YOUR NEW PURPLE PALETTE ---

// A strong, modern primary purple (Indigo-ish)
val PurplePrimary = Color(0xFF6366F1)
// A lighter, softer accent purple
val PurpleAccent = Color(0xFFA5B4FC)
// A very light purple, good for backgrounds or secondary elements
val PurpleSoft = Color(0xFFEEF2FF)

// Dark theme colors
val DarkBackground = Color(0xFF191820) // Very dark, slightly purple
val DarkSurface = Color(0xFF23212E)   // Dark grey-purple for cards
val TextOnDark = Color(0xFFF1F0F5)      // Off-white for text

// Light theme colors
val LightBackground = Color(0xFFFCFCFF) // Clean, almost white
val LightSurface = Color(0xFFFFFFFF)    // Pure white for cards
val TextOnLight = Color(0xFF1F1937)     // Dark purple-black for text

// --- 2. DEFINE YOUR CUSTOM NAVBAR COLORS (matches the theme) ---

// For Dark Mode
val DarkNavContainer = Color(0xFF2C2A3A).copy(alpha = 0.8f) // Dark purple glass
val DarkNavIndicator = Color(0xFF6366F1).copy(alpha = 0.4f) // Translucent primary glass
val DarkNavSelected = Color.White
val DarkNavUnselected = Color(0xFF9E9BAC)

// For Light Mode
val LightNavContainer = Color(0xFFFFFFFF).copy(alpha = 0.8f) // Light frosted glass
val LightNavIndicator = Color(0xFF6366F1).copy(alpha = 0.15f) // Very subtle purple pill
val LightNavSelected = Color(0xFF6366F1) // Solid primary purple
val LightNavUnselected = Color(0xFF706D83) // Grey-purple

// --- 3. CREATE THE APP'S COLOR SCHEMES ---

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleAccent,
    tertiary = PurpleAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleAccent,
    tertiary = PurpleAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = TextOnLight,
    onTertiary = TextOnLight,
    onBackground = TextOnLight,
    onSurface = TextOnLight,
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