package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.ThemeMode

/**
 * Shared reusable press & haptic feedback modifier providing iOS-grade tactile response:
 * - Immediate scale-down (~0.96f) and subtle opacity shift (~0.88f) on touch down with spring release
 * - Hardware-accelerated `graphicsLayer` transforms (zero layout/measure pass during animation)
 * - Light haptic feedback (`HapticFeedbackType.TextHandleMove` or `LongPress`)
 * - Built-in debounce window (`debounceMs`) to prevent accidental double-taps on async actions
 */
fun Modifier.pressableFeedback(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    pressedAlpha: Float = 0.88f,
    enableHaptics: Boolean = true,
    isPrimaryHaptic: Boolean = false,
    debounceMs: Long = 280L,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressableScale"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.48f
            isPressed -> pressedAlpha
            else -> 1f
        },
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pressableAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            role = role,
            onClick = {
                val now = SystemClock.elapsedRealtime()
                if (now - lastClickTimestamp >= debounceMs) {
                    lastClickTimestamp = now
                    if (enableHaptics) {
                        haptic.performHapticFeedback(
                            if (isPrimaryHaptic) {
                                HapticFeedbackType.LongPress
                            } else {
                                HapticFeedbackType.TextHandleMove
                            }
                        )
                    }
                    onClick()
                }
            }
        )
}

@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .clip(CircleShape)
            .pressableFeedback(
                enabled = enabled,
                pressedScale = 0.92f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AnimatedLiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val bgColor = MaterialTheme.colorScheme.background
    val infiniteTransition = rememberInfiniteTransition(label = "liquidAurora")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPhase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPhase2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = size.minDimension.coerceAtLeast(1f)

            val center1 = Offset(
                x = w * (0.18f + 0.22f * phase1),
                y = h * (0.12f + 0.14f * (1f - phase1))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientOrbCyan, Color.Transparent),
                    center = center1,
                    radius = minDim * 0.72f
                ),
                radius = minDim * 0.72f,
                center = center1
            )

            val center2 = Offset(
                x = w * (0.82f - 0.20f * phase2),
                y = h * (0.42f + 0.18f * phase1)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientOrbIndigo, Color.Transparent),
                    center = center2,
                    radius = minDim * 0.78f
                ),
                radius = minDim * 0.78f,
                center = center2
            )

            val center3 = Offset(
                x = w * (0.35f + 0.25f * phase2),
                y = h * (0.85f - 0.12f * phase1)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientOrbAmber, Color.Transparent),
                    center = center3,
                    radius = minDim * 0.65f
                ),
                radius = minDim * 0.65f,
                center = center3
            )
        }
        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    accentGlowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val surfaceTop = if (elevated) glass.glassElevatedTop else glass.glassSurfaceTop
    val surfaceBottom = if (elevated) glass.glassElevatedBottom else glass.glassSurfaceBottom

    val borderBrush = remember(accentGlowColor, glass) {
        Brush.linearGradient(
            colors = listOf(
                accentGlowColor?.copy(alpha = 0.75f) ?: glass.glassBorderHighlight,
                glass.glassBorderMid,
                glass.glassBorderSubtle
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 900f)
        )
    }

    val backgroundBrush = remember(surfaceTop, surfaceBottom) {
        Brush.verticalGradient(
            colors = listOf(surfaceTop, surfaceBottom)
        )
    }

    val pressModifier = if (onClick != null) {
        Modifier.pressableFeedback(
            pressedScale = 0.975f,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(pressModifier)
            .background(brush = backgroundBrush, shape = shape)
            .border(width = 1.2.dp, brush = borderBrush, shape = shape)
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(glass.glassInnerGlow, Color.Transparent),
                        startY = 0f,
                        endY = 28.dp.toPx()
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, 28.dp.toPx().coerceAtMost(size.height)),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassPillButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val glass = LocalLiquidGlassColors.current
    val shape = RoundedCornerShape(16.dp)
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val bgBrush = remember(isPrimary, accentColor, secondaryColor, glass) {
        if (isPrimary) {
            Brush.horizontalGradient(
                colors = listOf(accentColor, secondaryColor)
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(glass.glassElevatedTop, glass.glassSurfaceBottom)
            )
        }
    }

    val borderBrush = remember(isPrimary, accentColor, glass) {
        if (isPrimary) {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), accentColor.copy(alpha = 0.4f))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(glass.glassBorderHighlight, glass.glassBorderMid)
            )
        }
    }

    val textColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .clip(shape)
            .pressableFeedback(
                enabled = enabled && !isLoading,
                pressedScale = 0.96f,
                isPrimaryHaptic = isPrimary,
                onClick = onClick
            )
            .background(brush = bgBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (isPrimary) Color.White else accentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription ?: text,
                tint = if (isPrimary) Color.White else accentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ThemeModeSegmentedToggle(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val modes = listOf(
        Triple(ThemeMode.LIGHT, Icons.Outlined.LightMode, "Light"),
        Triple(ThemeMode.DARK, Icons.Outlined.DarkMode, "Dark"),
        Triple(ThemeMode.SYSTEM, Icons.Outlined.BrightnessAuto, "Auto")
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(glass.glassSurfaceTop, glass.glassSurfaceBottom)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(glass.glassBorderHighlight, glass.glassBorderSubtle)
                ),
                shape = CircleShape
            )
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, icon, label) ->
            val isSelected = currentMode == mode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "themeSegmentBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "themeSegmentFg"
            )

            Row(
                modifier = Modifier
                    .testTag("theme_mode_${mode.name.lowercase()}")
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 40.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .pressableFeedback(
                        pressedScale = 0.95f,
                        debounceMs = 150L,
                        onClick = { onModeSelected(mode) }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$label theme mode",
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FileErrorStateBanner(
    errorMessage: String,
    onSelectAnotherFile: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_error_banner"),
        cornerRadius = 20.dp,
        elevated = true,
        accentGlowColor = MaterialTheme.colorScheme.error,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "File error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unable to Open Notebook",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassPillButton(
                    text = "Dismiss",
                    onClick = onDismiss,
                    isPrimary = false,
                    modifier = Modifier.testTag("dismiss_error_button")
                )
                Spacer(modifier = Modifier.width(10.dp))
                LiquidGlassPillButton(
                    text = "Select Another File",
                    icon = Icons.Outlined.FolderOpen,
                    onClick = onSelectAnotherFile,
                    isPrimary = true,
                    modifier = Modifier.testTag("retry_select_file_button")
                )
            }
        }
    }
}

@Composable
fun LiquidGlassDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String = "Cancel",
    onDismissButton: () -> Unit = onDismiss,
    isDestructive: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    val glass = LocalLiquidGlassColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent,
            border = BorderStroke(
                1.4.dp,
                Brush.linearGradient(
                    listOf(glass.glassBorderHighlight, glass.glassBorderMid, glass.glassBorderSubtle)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(glass.glassElevatedTop, glass.glassElevatedBottom)
                        )
                    )
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPillButton(
                        text = dismissButtonText,
                        onClick = onDismissButton,
                        isPrimary = false,
                        modifier = Modifier.testTag("dialog_cancel_button")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    LiquidGlassPillButton(
                        text = confirmButtonText,
                        onClick = onConfirm,
                        isPrimary = true,
                        accentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("dialog_confirm_button")
                    )
                }
            }
        }
    }
}
