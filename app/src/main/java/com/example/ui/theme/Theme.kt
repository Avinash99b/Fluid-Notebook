package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFDC2626)
)

@Composable
private fun animateColor(targetValue: Color): Color {
    val animated by animateColorAsState(
        targetValue = targetValue,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "themeColorTransition"
    )
    return animated
}

@Composable
fun LuminaNotebookTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val targetScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val liquidColors = if (useDarkTheme) DarkLiquidGlassColors else LightLiquidGlassColors

    val animatedScheme = ColorScheme(
        primary = animateColor(targetScheme.primary),
        onPrimary = animateColor(targetScheme.onPrimary),
        primaryContainer = animateColor(targetScheme.primaryContainer),
        onPrimaryContainer = animateColor(targetScheme.onPrimaryContainer),
        inversePrimary = animateColor(targetScheme.inversePrimary),
        secondary = animateColor(targetScheme.secondary),
        onSecondary = animateColor(targetScheme.onSecondary),
        secondaryContainer = animateColor(targetScheme.secondaryContainer),
        onSecondaryContainer = animateColor(targetScheme.onSecondaryContainer),
        tertiary = animateColor(targetScheme.tertiary),
        onTertiary = animateColor(targetScheme.onTertiary),
        tertiaryContainer = animateColor(targetScheme.tertiaryContainer),
        onTertiaryContainer = animateColor(targetScheme.onTertiaryContainer),
        background = animateColor(targetScheme.background),
        onBackground = animateColor(targetScheme.onBackground),
        surface = animateColor(targetScheme.surface),
        onSurface = animateColor(targetScheme.onSurface),
        surfaceVariant = animateColor(targetScheme.surfaceVariant),
        onSurfaceVariant = animateColor(targetScheme.onSurfaceVariant),
        surfaceTint = animateColor(targetScheme.surfaceTint),
        inverseSurface = animateColor(targetScheme.inverseSurface),
        inverseOnSurface = animateColor(targetScheme.inverseOnSurface),
        error = animateColor(targetScheme.error),
        onError = animateColor(targetScheme.onError),
        errorContainer = animateColor(targetScheme.errorContainer),
        onErrorContainer = animateColor(targetScheme.onErrorContainer),
        outline = animateColor(targetScheme.outline),
        outlineVariant = animateColor(targetScheme.outlineVariant),
        scrim = targetScheme.scrim,
        surfaceBright = animateColor(targetScheme.surfaceBright),
        surfaceDim = animateColor(targetScheme.surfaceDim),
        surfaceContainer = animateColor(targetScheme.surfaceContainer),
        surfaceContainerHigh = animateColor(targetScheme.surfaceContainerHigh),
        surfaceContainerHighest = animateColor(targetScheme.surfaceContainerHighest),
        surfaceContainerLow = animateColor(targetScheme.surfaceContainerLow),
        surfaceContainerLowest = animateColor(targetScheme.surfaceContainerLowest)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalLiquidGlassColors provides liquidColors) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}
