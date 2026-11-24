package com.example.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.WallpaperPreset
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroSapphire

@Composable
fun VitroWallpaperBackdrop(
    preset: WallpaperPreset,
    dimAlpha: Float = 0.16f,
    glowIntensity: Float = 0.80f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (preset) {
            WallpaperPreset.SYSTEM_LIVE -> {
                // Transparent backdrop letting the real Android system / live wallpaper show through
                Box(modifier = Modifier.fillMaxSize())
            }
            WallpaperPreset.AURORA -> {
                Image(
                    painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795471682249),
                    contentDescription = stringResource(R.string.cd_wallpaper),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WallpaperPreset.PRISM -> {
                Image(
                    painter = painterResource(id = R.drawable.img_wallpaper_prism_1795471682679),
                    contentDescription = stringResource(R.string.cd_wallpaper),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WallpaperPreset.DUSK -> {
                Image(
                    painter = painterResource(id = R.drawable.img_wallpaper_dusk_1795471681919),
                    contentDescription = stringResource(R.string.cd_wallpaper),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WallpaperPreset.OBSIDIAN_MESH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    VitroObsidian,
                                    Color(0xFF101D3A),
                                    VitroIndigo.copy(alpha = 0.35f),
                                    VitroCyanAccent.copy(alpha = 0.18f),
                                    VitroObsidian
                                )
                            )
                        )
                )
            }
        }

        // Subtle atmospheric ambient aura orbs strictly on the wallpaper layer
        if (glowIntensity > 0.02f) {
            VitroAmbientGlowLayer(intensity = glowIntensity)
        }

        // Soft atmospheric scrim so frosted surfaces & white typography stay crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = dimAlpha + 0.10f),
                            Color.Black.copy(alpha = dimAlpha * 0.45f),
                            Color.Black.copy(alpha = dimAlpha + 0.16f)
                        )
                    )
                )
        )
    }
}

@Composable
fun VitroAmbientGlowLayer(
    intensity: Float = 0.8f,
    modifier: Modifier = Modifier
) {
    val clamped = intensity.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow_drift")
    val driftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_orb_drift"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_orb_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseAlpha = 0.18f * clamped

        val orb1Center = Offset(
            x = w * (0.16f + driftProgress * 0.08f),
            y = h * (0.18f + driftProgress * 0.05f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VitroCyanAccent.copy(alpha = baseAlpha * pulse),
                    VitroSapphire.copy(alpha = baseAlpha * 0.35f),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = orb1Center
        )

        val orb2Center = Offset(
            x = w * (0.84f - driftProgress * 0.07f),
            y = h * (0.48f - driftProgress * 0.05f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VitroIndigo.copy(alpha = baseAlpha * 0.85f),
                    VitroCoral.copy(alpha = baseAlpha * 0.18f),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = w * 0.58f
            ),
            radius = w * 0.58f,
            center = orb2Center
        )

        val orb3Center = Offset(
            x = w * (0.50f + (driftProgress - 0.5f) * 0.12f),
            y = h * 0.90f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VitroCyanAccent.copy(alpha = baseAlpha * 0.78f),
                    VitroMint.copy(alpha = baseAlpha * 0.25f),
                    Color.Transparent
                ),
                center = orb3Center,
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = orb3Center
        )
    }
}

@Composable
fun VitroGlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tintColor: Color = Color.White,
    glassOpacity: Float = 0.22f,
    borderColor: Color = Color.White.copy(alpha = 0.38f),
    glowColor: Color = Color.Transparent,
    glowIntensity: Float = 0f,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    val clampedOpacity = glassOpacity.coerceIn(0.08f, 0.78f)
    val fillBrush = remember(tintColor, clampedOpacity, glowColor, glowIntensity) {
        val sheenColor = if (glowColor != Color.Transparent && glowIntensity > 0.02f) {
            glowColor.copy(alpha = (0.16f * glowIntensity).coerceAtMost(0.24f))
        } else {
            tintColor.copy(alpha = (clampedOpacity * 1.15f).coerceAtMost(0.84f))
        }
        Brush.linearGradient(
            colors = listOf(
                tintColor.copy(alpha = (clampedOpacity * 1.18f).coerceAtMost(0.86f)),
                sheenColor,
                Color(0xFF0A1024).copy(alpha = (clampedOpacity * 0.85f).coerceIn(0.20f, 0.65f))
            )
        )
    }
    val borderBrush = remember(borderColor, glowColor, glowIntensity) {
        val rimHighlight = if (glowColor != Color.Transparent && glowIntensity > 0.05f) {
            glowColor.copy(alpha = (0.52f * glowIntensity).coerceIn(0.25f, 0.65f))
        } else {
            borderColor.copy(alpha = 0.38f)
        }
        Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = 0.78f),
                borderColor.copy(alpha = 0.18f),
                rimHighlight
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content
    )
}

fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    glowColor: Color = VitroCyanAccent,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetScale = when {
        isPressed -> scaleDown
        isHovered -> 1.03f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 520f),
        label = "smooth_hover_scale"
    )

    this
        .scale(scale)
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(color = glowColor),
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun rememberSmoothHoverGlowColor(
    isHovered: Boolean,
    isActive: Boolean,
    activeColor: Color = VitroCyanAccent
): Color {
    val target = when {
        isActive -> activeColor
        isHovered -> activeColor.copy(alpha = 0.65f)
        else -> Color.White.copy(alpha = 0.25f)
    }
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(220),
        label = "hover_border_glow"
    )
    return animated
}
