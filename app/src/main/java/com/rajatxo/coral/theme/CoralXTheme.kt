package com.rajatxo.coral.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ★ Coral X Theme System
 *
 * Supports:
 *   - Light mode
 *   - Dark mode
 *   - AMOLED black mode (pure black backgrounds for OLED battery savings)
 *   - Material You dynamic color (Android 12+) — uses the user's wallpaper colors
 *   - Manual accent color override
 *
 * Usage in MainActivity:
 *   setContent {
 *       CoralXTheme {
 *           // app content
 *       }
 *   }
 *
 * Theme mode is controlled by ThemeManager (a SharedPreferences-backed StateFlow).
 * When UI is built, settings will let the user pick: System / Light / Dark / AMOLED.
 */

// ─── Color Definitions ───────────────────────────────────────────

// Coral X brand colors
val CoralRed = Color(0xFFFF6B6B)
val CoralPink = Color(0xFFFF8FAB)
val CoralGold = Color(0xFFFFD166)

// Dark theme palette
private val DarkColorScheme = darkColorScheme(
    primary = CoralRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A1A),
    onPrimaryContainer = CoralPink,
    secondary = CoralGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2E0A),
    onSecondaryContainer = CoralGold,
    tertiary = CoralPink,
    onTertiary = Color.Black,
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBBBBBB),
    error = Color(0xFFFF5252),
    onError = Color.White,
)

// AMOLED theme palette (pure black backgrounds)
private val AmoledColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF0A0A0A),
)

// Light theme palette
private val LightColorScheme = lightColorScheme(
    primary = CoralRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = Color(0xFF3D0A0A),
    secondary = CoralGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF3CC),
    onSecondaryContainer = Color(0xFF3D2E0A),
    tertiary = CoralPink,
    onTertiary = Color.Black,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF555555),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

/**
 * Theme mode enum.
 * Will be stored in SharedPreferences via ThemeManager (when built).
 */
enum class ThemeMode {
    SYSTEM,     // Follow system dark/light setting
    LIGHT,      // Force light
    DARK,       // Force dark
    AMOLED      // Force dark with pure black backgrounds
}

/**
 * The main theme composable.
 *
 * @param mode Theme mode (defaults to SYSTEM)
 * @param dynamicColor Whether to use Material You dynamic color (Android 12+)
 */
@Composable
fun CoralXTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    // Pick the color scheme
    val colorScheme = when {
        // Material You dynamic color (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // AMOLED black mode
        mode == ThemeMode.AMOLED -> AmoledColorScheme
        // Standard dark/light
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set system bar icon appearance based on theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
