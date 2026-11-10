package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Mode Palette - Crisp Frosted Quartz & Prismatic Cyan/Indigo
val LightBackground = Color(0xFFF3F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE7EEF8)
val LightPrimary = Color(0xFF0284C7)       // Refractive Sky Cyan
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0F2FE)
val LightOnPrimaryContainer = Color(0xFF075985)
val LightSecondary = Color(0xFF4F46E5)     // Deep Prismatic Indigo
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE0E7FF)
val LightOnSecondaryContainer = Color(0xFF312E81)
val LightTertiary = Color(0xFF0D9488)      // Emerald Glass Accent
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

// Dark Mode Palette - Deep Cosmic Obsidian & Liquid Cyan/Violet
val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF0F172A)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkPrimary = Color(0xFF38BDF8)        // Luminous Liquid Cyan
val DarkOnPrimary = Color(0xFF082F49)
val DarkPrimaryContainer = Color(0xFF0C4A6E)
val DarkOnPrimaryContainer = Color(0xFFE0F2FE)
val DarkSecondary = Color(0xFF818CF8)      // Electric Indigo
val DarkOnSecondary = Color(0xFF1E1B4B)
val DarkSecondaryContainer = Color(0xFF312E81)
val DarkOnSecondaryContainer = Color(0xFFE0E7FF)
val DarkTertiary = Color(0xFF2DD4BF)       // Aqua Glow
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassElevatedTop: Color,
    val glassElevatedBottom: Color,
    val glassBorderHighlight: Color,
    val glassBorderMid: Color,
    val glassBorderSubtle: Color,
    val glassInnerGlow: Color,
    val ambientOrbCyan: Color,
    val ambientOrbIndigo: Color,
    val ambientOrbAmber: Color,
    val codeBackground: Color,
    val codeOutputBackground: Color,
    val syntaxKeyword: Color,
    val syntaxString: Color,
    val syntaxComment: Color,
    val syntaxNumber: Color,
    val syntaxFunction: Color,
    val syntaxDecorator: Color,
    val badgeCode: Color,
    val badgeMarkdown: Color,
    val badgeDirty: Color,
    val badgeSaved: Color
)

val LightLiquidGlassColors = LiquidGlassColors(
    isDark = false,
    glassSurfaceTop = Color(0xD9FFFFFF),
    glassSurfaceBottom = Color(0xB3F1F5F9),
    glassElevatedTop = Color(0xF2FFFFFF),
    glassElevatedBottom = Color(0xD9F8FAFC),
    glassBorderHighlight = Color(0xFFFFFFFF),
    glassBorderMid = Color(0x99BAE6FD),
    glassBorderSubtle = Color(0x4D94A3B8),
    glassInnerGlow = Color(0x66FFFFFF),
    ambientOrbCyan = Color(0x3338BDF8),
    ambientOrbIndigo = Color(0x266366F1),
    ambientOrbAmber = Color(0x22F59E0B),
    codeBackground = Color(0xCCEEF2F9),
    codeOutputBackground = Color(0xB3F8FAFC),
    syntaxKeyword = Color(0xFF7C3AED),
    syntaxString = Color(0xFF059669),
    syntaxComment = Color(0xFF64748B),
    syntaxNumber = Color(0xFFD97706),
    syntaxFunction = Color(0xFF0284C7),
    syntaxDecorator = Color(0xFFDB2777),
    badgeCode = Color(0xFF0284C7),
    badgeMarkdown = Color(0xFF7C3AED),
    badgeDirty = Color(0xFFD97706),
    badgeSaved = Color(0xFF059669)
)

val DarkLiquidGlassColors = LiquidGlassColors(
    isDark = true,
    glassSurfaceTop = Color(0xB8162238),
    glassSurfaceBottom = Color(0x940D1526),
    glassElevatedTop = Color(0xD91E2D4A),
    glassElevatedBottom = Color(0xB8111B2E),
    glassBorderHighlight = Color(0x80E0F2FE),
    glassBorderMid = Color(0x4D38BDF8),
    glassBorderSubtle = Color(0x26818CF8),
    glassInnerGlow = Color(0x1FFFFFFF),
    ambientOrbCyan = Color(0x2E0EA5E9),
    ambientOrbIndigo = Color(0x296366F1),
    ambientOrbAmber = Color(0x1CF59E0B),
    codeBackground = Color(0xD9090E1A),
    codeOutputBackground = Color(0xBF0B1324),
    syntaxKeyword = Color(0xFFC084FC),
    syntaxString = Color(0xFF34D399),
    syntaxComment = Color(0xFF64748B),
    syntaxNumber = Color(0xFFFBBF24),
    syntaxFunction = Color(0xFF38BDF8),
    syntaxDecorator = Color(0xFFF472B6),
    badgeCode = Color(0xFF38BDF8),
    badgeMarkdown = Color(0xFFA78BFA),
    badgeDirty = Color(0xFFFBBF24),
    badgeSaved = Color(0xFF34D399)
)

val LocalLiquidGlassColors = staticCompositionLocalOf { DarkLiquidGlassColors }
