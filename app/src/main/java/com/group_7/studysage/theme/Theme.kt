/**
 * Defines the app's visual design system - colors, fonts, and overall look.
 * Uses Material Design 3 with custom branding to make everything look consistent.
 * 
 */
package com.group_7.studysage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.group_7.studysage.theme.Typography



val PurplePrimary = Color(0xFF652497)

val PurpleSecondary = Color(0xFF9333EA)

val PurpleTertiary = Color(0xFFFBBF24)

val PurpleSoft = Color(0xFFF5F3FF)

// --- Light Theme Colors ---
val LightBackground = Color(0xFFFCFCFF) 
val LightSurface = Color(0xFFFFFFFF)   
val TextOnLight = Color(0xFF1B1921)     
val TextOnLightMuted = Color(0xFF6B7280)

// --- Dark Theme Colors ---
val DarkBackground = Color(0xFF1A1721)
val DarkSurface = Color(0xFF2C2A3A)   
val TextOnDark = Color(0xFFF5F3FF)      
val TextOnDarkMuted = Color(0xFF9E9BAC)   



// For Dark Mode
val DarkNavContainer = Color(0xFF2C2A3A).copy(alpha = 0.8f) // Dark purple glass
val DarkNavIndicator = Color(0xFF9333EA).copy(alpha = 0.4f) // Translucent (brighter) primary glass
val DarkNavSelected = Color.White
val DarkNavUnselected = TextOnDarkMuted // Light grey-purple

// For Light Mode
val LightNavContainer = Color.White.copy(alpha = 0.8f) // Light frosted glass
val LightNavIndicator = Color(0xFF652497).copy(alpha = 0.15f) // Very subtle purple pill
val LightNavSelected = PurplePrimary // Solid dark purple
val LightNavUnselected = TextOnLightMuted // Solid readable grey


private val DarkColorScheme = darkColorScheme(
    primary = PurpleSecondary,
    secondary = PurplePrimary, 
    tertiary = PurpleTertiary,
    background = DarkBackground,
    surface = DarkSurface, // Card backgrounds
    surfaceVariant = DarkSurface, 
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextOnDark, // Main text
    onSurface = TextOnDark,    // Text on cards
    onSurfaceVariant = TextOnDarkMuted // Muted text
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary, 
    secondary = PurpleSecondary,
    tertiary = PurpleTertiary,
    background = LightBackground,
    surface = LightSurface, 
    surfaceVariant = PurpleSoft,  
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = TextOnLight,
    onBackground = TextOnLight,
    onSurface = TextOnLight,    // Text on cards (dark)
    onSurfaceVariant = TextOnLightMuted // Muted text (grey)
)


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
        typography = Typography, 
        content = content
    )
}