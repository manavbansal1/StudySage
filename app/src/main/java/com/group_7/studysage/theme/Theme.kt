package com.group_7.studysage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Purple & White Color Palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// StudySage Custom Colors
val PurplePrimary = Color(0xFF8B5CF6)
val PurplePrimaryDark = Color(0xFF7C3AED)
val PurplePrimaryLight = Color(0xFFA78BFA)
val PurpleSecondary = Color(0xFFDDD6FE)
val PurpleAccent = Color(0xFFC4B5FD)

val BackgroundLight = Color(0xFFFEFBFF)
val BackgroundSurface = Color(0xFFF8F7FF)
val TextPrimary = Color(0xFF1F1937)
val TextSecondary = Color(0xFF6B7280)
val CardBackground = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = PurpleAccent,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    secondary = PurpleSecondary,
    onSecondary = TextPrimary,
    tertiary = PurpleAccent,
    onTertiary = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurface,
    onSurfaceVariant = TextSecondary
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