package com.example.launcher.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WallpaperGlassStyle
import com.example.launcher.model.WallpaperParticleOverlay
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.system.WallpaperStorageManager
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroSapphire
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-featured modern Wallpaper Engine supporting:
 * - 6 Live 60fps Canvas animated worlds
 * - 2 Liquid/Architectural Glass procedural worlds
 * - High-res bundled artwork & zero-permission Gallery Photos
 * - Real device sensor 3D parallax tilt
 * - 6 Glass lens overlays (Frosted, Fluted/Reeded, Prism Crystal, Rain Droplets, Liquid Shimmer)
 * - 4 Live particle overlays (Stardust, Bokeh, Fireflies, Snow)
 * - Dynamic Blur, Color Tone grading, Ambient Glow, and Scrim Dimming
 */
@Composable
fun VitroAdvancedWallpaperBackdrop(
    wallpaperConfig: WallpaperConfig,
    glowIntensity: Float = 0.80f,
    enableSensorParallax: Boolean = true,
    isMiniPreview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    var isPowerSave by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isPowerSave = pm?.isPowerSaveMode == true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    isPowerSave = pm?.isPowerSaveMode == true
                }
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val speed = wallpaperConfig.liveSpeed.coerceIn(0.3f, 2.0f)
    val cycleMs = (16000f / speed).toInt().coerceIn(5000, 32000)

    val needsPhase = (wallpaperConfig.preset.isLive ||
        wallpaperConfig.glassStyle == WallpaperGlassStyle.LIQUID_SHIMMER ||
        wallpaperConfig.particleOverlay != WallpaperParticleOverlay.NONE) &&
        isResumed &&
        !isPowerSave

    val livePhase: Float = if (needsPhase) {
        val infinite = rememberInfiniteTransition(label = "vitro_wp_engine")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wp_phase"
        )
        p
    } else {
        0.18f
    }

    val (tiltX, tiltY) = rememberGyroParallaxOffset(
        enabled = isResumed && !isPowerSave && enableSensorParallax && wallpaperConfig.parallaxEnabled &&
            !isMiniPreview && wallpaperConfig.preset != WallpaperPreset.SYSTEM_LIVE
    )

    var customBitmap by remember(wallpaperConfig.customImagePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(wallpaperConfig.preset, wallpaperConfig.customImagePath) {
        if (wallpaperConfig.preset == WallpaperPreset.CUSTOM_PHOTO && wallpaperConfig.customImagePath.isNotBlank()) {
            customBitmap = WallpaperStorageManager.loadCustomWallpaperBitmap(wallpaperConfig.customImagePath)
        }
    }

    val parallaxFactor = if (enableSensorParallax && wallpaperConfig.parallaxEnabled && !isMiniPreview) {
        wallpaperConfig.parallaxStrength.coerceIn(0f, 1f)
    } else 0f

    val blurDp = wallpaperConfig.blurRadiusDp.coerceIn(0f, 24f)
    val baseLayerModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            if (parallaxFactor > 0.01f) {
                val scale = 1f + 0.09f * parallaxFactor
                scaleX = scale
                scaleY = scale
                translationX = tiltX * 42f * parallaxFactor
                translationY = tiltY * 42f * parallaxFactor
            }
        }
        .let { mod ->
            if (blurDp > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mod.blur(blurDp.dp)
            } else mod
        }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Base Wallpaper World (Live, Glass, HD, Custom Photo, or Native System Live)
        Box(modifier = baseLayerModifier) {
            when (wallpaperConfig.preset) {
                WallpaperPreset.SYSTEM_LIVE -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                WallpaperPreset.CUSTOM_PHOTO -> {
                    val bmp = customBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = stringResource(R.string.cd_wallpaper),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795471682249),
                            contentDescription = stringResource(R.string.cd_wallpaper),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                WallpaperPreset.LIVE_AURORA_WAVE -> LiveAuroraWaveCanvas(phase = livePhase)
                WallpaperPreset.LIVE_NEBULA_ORBS -> LiveNebulaOrbsCanvas(phase = livePhase)
                WallpaperPreset.LIVE_LIQUID_LAVA -> LiveLiquidLavaCanvas(phase = livePhase)
                WallpaperPreset.LIVE_CYBER_GRID -> LiveCyberGridCanvas(phase = livePhase)
                WallpaperPreset.LIVE_RAIN_GLASS -> LiveRainGlassCanvas(phase = livePhase)
                WallpaperPreset.LIVE_SOLAR_DYNAMIC -> LiveSolarDynamicCanvas(phase = livePhase)
                WallpaperPreset.GLASS_REFRACTION -> GlassRefractionWorldCanvas(phase = livePhase)
                WallpaperPreset.GLASS_PRISM_BLOOM -> GlassPrismBloomCanvas(phase = livePhase)
            }
        }

        // Fallback soft veil for API < 31 when blur slider is used
        if (blurDp > 0.5f && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A1428).copy(alpha = (blurDp / 24f) * 0.42f))
            )
        }

        // Layer 2: Color Tone Atmosphere Wash
        if (wallpaperConfig.colorTone.tint != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                wallpaperConfig.colorTone.tint.copy(alpha = 0.24f),
                                wallpaperConfig.colorTone.tint.copy(alpha = 0.12f),
                                wallpaperConfig.colorTone.tint.copy(alpha = 0.28f)
                            )
                        )
                    )
            )
        }

        // Layer 3: Architectural & Liquid Glass Lens Overlay (Frosted / Fluted / Prism / Droplets / Shimmer)
        if (wallpaperConfig.glassStyle != WallpaperGlassStyle.NONE && wallpaperConfig.glassIntensity > 0.02f) {
            WallpaperGlassOverlayLayer(
                style = wallpaperConfig.glassStyle,
                intensity = wallpaperConfig.glassIntensity.coerceIn(0f, 1f),
                phase = livePhase,
                isMiniPreview = isMiniPreview
            )
        }

        // Layer 4: Live Floating Particle / Bokeh Layer
        if (wallpaperConfig.particleOverlay != WallpaperParticleOverlay.NONE) {
            WallpaperParticleLayer(
                overlay = wallpaperConfig.particleOverlay,
                phase = livePhase,
                isMiniPreview = isMiniPreview
            )
        }

        // Layer 5: Subtle Ambient Aura Glow (skip for mini preview to save GPU)
        if (glowIntensity > 0.02f && !isMiniPreview && wallpaperConfig.preset != WallpaperPreset.SYSTEM_LIVE) {
            VitroAmbientGlowLayer(intensity = glowIntensity)
        }

        // Layer 6: Atmospheric Contrast Scrim so icons & frosted typography stay crystal sharp
        val effectiveDim = wallpaperConfig.dimAlpha.coerceIn(0f, 0.70f)
        if (effectiveDim > 0.01f || wallpaperConfig.preset != WallpaperPreset.SYSTEM_LIVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (effectiveDim + 0.08f).coerceAtMost(0.78f)),
                                Color.Black.copy(alpha = (effectiveDim * 0.42f).coerceAtMost(0.55f)),
                                Color.Black.copy(alpha = (effectiveDim + 0.12f).coerceAtMost(0.80f))
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun rememberGyroParallaxOffset(enabled: Boolean): Pair<Float, Float> {
    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(enabled, context) {
        if (!enabled) {
            tiltX = 0f
            tiltY = 0f
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accel == null) {
            return@DisposableEffect onDispose { }
        }

        var smoothX = 0f
        var smoothY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX = (event.values.getOrNull(0) ?: 0f).coerceIn(-6f, 6f) / 6f
                val rawY = ((event.values.getOrNull(1) ?: 0f) - 4.5f).coerceIn(-6f, 6f) / 6f
                smoothX = smoothX * 0.88f + rawX * 0.12f
                smoothY = smoothY * 0.88f + rawY * 0.12f
                tiltX = smoothX
                tiltY = smoothY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    return tiltX to tiltY
}

// =====================================================================
// LIVE 60FPS CANVAS WALLPAPER WORLDS
// =====================================================================

@Composable
private fun LiveAuroraWaveCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (2f * PI.toFloat())

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF030714), Color(0xFF081733), Color(0xFF060E24), Color(0xFF020611))
            )
        )

        val ribbons = listOf(
            Triple(0.26f, listOf(Color(0xAA22D3EE), Color(0x6638BDF8), Color.Transparent), 0f),
            Triple(0.46f, listOf(Color(0x99818CF8), Color(0x5534D399), Color.Transparent), 1.9f),
            Triple(0.68f, listOf(Color(0x882DD4BF), Color(0x446366F1), Color.Transparent), 3.7f)
        )

        ribbons.forEachIndexed { idx, (baseYFrac, colors, offsetPhase) ->
            val path = Path()
            val startY = h * baseYFrac
            path.moveTo(0f, startY)
            val steps = 16
            for (s in 0..steps) {
                val x = (w / steps) * s
                val wave1 = sin(t + offsetPhase + s * 0.45f) * h * 0.075f
                val wave2 = cos(t * 1.3f - s * 0.3f + idx) * h * 0.045f
                path.lineTo(x, startY + wave1 + wave2)
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = colors,
                    startY = startY - h * 0.15f,
                    endY = startY + h * 0.42f
                )
            )
        }
    }
}

@Composable
private fun LiveNebulaOrbsCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (2f * PI.toFloat())

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF040312), Color(0xFF130B2E), Color(0xFF081026), Color(0xFF03050F))
            )
        )

        // Drifting cosmic plasma spheres
        drawGlowingOrb(
            center = Offset(w * (0.28f + 0.22f * sin(t)), h * (0.26f + 0.10f * cos(t))),
            radius = w * 0.62f,
            inner = Color(0xAA6366F1),
            mid = Color(0x4422D3EE)
        )
        drawGlowingOrb(
            center = Offset(w * (0.74f + 0.20f * cos(t + 1.2f)), h * (0.55f + 0.14f * sin(t + 0.8f))),
            radius = w * 0.66f,
            inner = Color(0xAAEC4899),
            mid = Color(0x447C3AED)
        )
        drawGlowingOrb(
            center = Offset(w * (0.45f + 0.25f * sin(t * 1.5f + 2.4f)), h * (0.82f + 0.08f * cos(t))),
            radius = w * 0.54f,
            inner = Color(0x9914B8A6),
            mid = Color(0x3338BDF8)
        )

        // Subtle twinkling stars
        for (i in 0 until 36) {
            val sx = ((i * 73 + 19) % 100) / 100f * w
            val sy = ((i * 41 + 11) % 100) / 100f * h
            val twinkle = (0.35f + 0.65f * ((sin(t * 2f + i * 0.9f) + 1f) / 2f))
            drawCircle(
                color = Color.White.copy(alpha = twinkle * 0.72f),
                radius = if (i % 5 == 0) 2.6f else 1.4f,
                center = Offset(sx, sy)
            )
        }
    }
}

@Composable
private fun LiveLiquidLavaCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (2f * PI.toFloat())

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF140614), Color(0xFF2B0A22), Color(0xFF120820), Color(0xFF070512))
            )
        )

        drawGlowingOrb(
            center = Offset(w * (0.30f + 0.28f * sin(t)), h * (0.24f + 0.18f * cos(t))),
            radius = w * 0.68f,
            inner = Color(0xCCF97316),
            mid = Color(0x55FB7185)
        )
        drawGlowingOrb(
            center = Offset(w * (0.75f + 0.22f * cos(t * 1.2f)), h * (0.48f + 0.20f * sin(t))),
            radius = w * 0.64f,
            inner = Color(0xCCF43F5E),
            mid = Color(0x55A855F7)
        )
        drawGlowingOrb(
            center = Offset(w * (0.42f + 0.26f * sin(t + 2.1f)), h * (0.78f + 0.14f * cos(t * 1.4f))),
            radius = w * 0.66f,
            inner = Color(0xBBF59E0B),
            mid = Color(0x446366F1)
        )
    }
}

@Composable
private fun LiveCyberGridCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.56f

        // Upper synth sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF050214), Color(0xFF1E0B42), Color(0xFF4C1D95), Color(0xFF0E7490)),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(w, horizonY)
        )

        // Glowing neon sun on the horizon
        val sunRadius = w * 0.27f
        val sunCenter = Offset(w * 0.5f, horizonY - sunRadius * 0.22f)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFFB7185), Color(0xFFA855F7)),
                startY = sunCenter.y - sunRadius,
                endY = sunCenter.y + sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )

        // Lower cyber floor
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0622), Color(0xFF020617)),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // Glowing horizon line
        drawLine(
            color = VitroCyanAccent.copy(alpha = 0.9f),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 3f
        )

        // Perspective vertical grid lines
        val vanishX = w * 0.5f
        for (i in -6..6) {
            val bottomX = vanishX + i * (w * 0.22f)
            drawLine(
                color = VitroCyanAccent.copy(alpha = 0.34f),
                start = Offset(vanishX + i * (w * 0.045f), horizonY),
                end = Offset(bottomX, h),
                strokeWidth = 1.6f
            )
        }

        // Animated scrolling horizontal perspective lines
        val rows = 12
        for (r in 0 until rows) {
            val frac = ((r + phase * 3f) % rows) / rows.toFloat()
            val eased = frac * frac
            val y = horizonY + eased * (h - horizonY)
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = (0.14f + eased * 0.45f)),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f + eased * 1.4f
            )
        }
    }
}

@Composable
private fun LiveRainGlassCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Moody rainy city night window bokeh backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF06101E), Color(0xFF0E243B), Color(0xFF142E49), Color(0xFF050A14))
            )
        )
        drawGlowingOrb(Offset(w * 0.22f, h * 0.30f), w * 0.36f, Color(0x6638BDF8), Color(0x1838BDF8))
        drawGlowingOrb(Offset(w * 0.78f, h * 0.42f), w * 0.32f, Color(0x55F59E0B), Color(0x15F59E0B))
        drawGlowingOrb(Offset(w * 0.54f, h * 0.72f), w * 0.40f, Color(0x55FB7185), Color(0x15818CF8))

        // Animated sliding raindrops & refraction trails
        for (i in 0 until 26) {
            val x = ((i * 53 + 17) % 94 + 3) / 100f * w
            val speedMult = 1f + (i % 4) * 0.35f
            val yFrac = ((i * 0.17f) + phase * speedMult) % 1.15f - 0.08f
            val y = yFrac * h
            val dropRadius = 3.2f + (i % 4) * 1.9f
            val trailLen = 36f + (i % 5) * 24f

            // Water trail behind drop
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.24f)),
                    startY = y - trailLen,
                    endY = y
                ),
                start = Offset(x, y - trailLen),
                end = Offset(x, y),
                strokeWidth = dropRadius * 0.65f
            )
            // Glassy water droplet
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = dropRadius,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = dropRadius * 0.32f,
                center = Offset(x - dropRadius * 0.28f, y - dropRadius * 0.28f)
            )
        }
    }
}

@Composable
private fun LiveSolarDynamicCanvas(phase: Float) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val t = phase * (2f * PI.toFloat())

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val skyColors = when (hour) {
            in 5..8 -> listOf(Color(0xFF1E1B4B), Color(0xFF9D174D), Color(0xFFF59E0B), Color(0xFF1E293B))
            in 9..15 -> listOf(Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0F172A))
            in 16..18 -> listOf(Color(0xFF1E1B4B), Color(0xFFBE185D), Color(0xFFF97316), Color(0xFF0F172A))
            else -> listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF040914))
        }
        drawRect(brush = Brush.verticalGradient(skyColors))

        // Celestial sun or moon
        val isNight = hour !in 6..18
        val celestialCenter = Offset(
            x = w * (0.25f + 0.50f * ((hour % 12) / 12f)),
            y = h * (if (isNight) 0.24f else 0.30f) + sin(t) * 14f
        )
        drawGlowingOrb(
            center = celestialCenter,
            radius = w * 0.44f,
            inner = if (isNight) Color(0x88E2E8F0) else Color(0xCCFDE047),
            mid = if (isNight) Color(0x2838BDF8) else Color(0x44FB923C)
        )
        drawCircle(
            color = if (isNight) Color(0xFFF8FAFC) else Color(0xFFFEF08A),
            radius = w * 0.085f,
            center = celestialCenter
        )

        // Stars at night or soft drifting clouds by day
        if (isNight) {
            for (i in 0 until 32) {
                val sx = ((i * 67 + 13) % 100) / 100f * w
                val sy = ((i * 29 + 7) % 62) / 100f * h
                val alpha = 0.3f + 0.65f * ((sin(t * 2f + i) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = alpha), radius = 1.9f, center = Offset(sx, sy))
            }
        } else {
            for (i in 0 until 4) {
                val cx = (((i * 0.29f + phase * 0.4f) % 1.3f) - 0.15f) * w
                val cy = h * (0.22f + i * 0.11f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(cx, cy),
                    size = Size(w * 0.42f, 44f),
                    cornerRadius = CornerRadius(44f, 44f)
                )
            }
        }
    }
}

@Composable
private fun GlassRefractionWorldCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (2f * PI.toFloat())

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF050A18), Color(0xFF0F2147), Color(0xFF191645), Color(0xFF060B1A))
            )
        )

        // Flowing iridescent color beams behind fluted glass
        drawGlowingOrb(
            Offset(w * (0.25f + 0.25f * sin(t)), h * 0.32f),
            w * 0.72f,
            VitroCyanAccent.copy(alpha = 0.75f),
            VitroIndigo.copy(alpha = 0.25f)
        )
        drawGlowingOrb(
            Offset(w * (0.75f - 0.25f * cos(t)), h * 0.66f),
            w * 0.72f,
            VitroCoral.copy(alpha = 0.60f),
            VitroMint.copy(alpha = 0.25f)
        )

        // Crisp architectural vertical fluted glass ribs
        val ribs = 18
        val ribW = w / ribs
        for (i in 0 until ribs) {
            val x = i * ribW
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent,
                        Color(0xFF020617).copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.08f)
                    ),
                    startX = x,
                    endX = x + ribW
                ),
                topLeft = Offset(x, 0f),
                size = Size(ribW, h)
            )
        }
    }
}

@Composable
private fun GlassPrismBloomCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.46f
        val t = phase * (2f * PI.toFloat())

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF172554), Color(0xFF0A1026), Color(0xFF030612)),
                center = Offset(cx, cy),
                radius = h * 0.75f
            )
        )

        val petals = 6
        val baseRadius = w * 0.44f
        for (i in 0 until petals) {
            val angle = t * 0.5f + i * (2f * PI.toFloat() / petals)
            val px = cx + cos(angle) * (w * 0.18f)
            val py = cy + sin(angle) * (w * 0.24f)
            val petalColor = when (i % 3) {
                0 -> VitroCyanAccent
                1 -> VitroIndigo
                else -> VitroMint
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        petalColor.copy(alpha = 0.36f),
                        petalColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(px, py),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = baseRadius * 0.56f,
                center = Offset(px, py),
                style = Stroke(width = 1.6f)
            )
        }
    }
}

// =====================================================================
// GLASS LENS OVERLAYS & LIVE PARTICLE OVERLAYS (WORK ON ANY WALLPAPER!)
// =====================================================================

@Composable
private fun WallpaperGlassOverlayLayer(
    style: WallpaperGlassStyle,
    intensity: Float,
    phase: Float,
    isMiniPreview: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (style) {
            WallpaperGlassStyle.NONE -> Unit
            WallpaperGlassStyle.FROSTED_GLASS -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f * intensity),
                            Color(0xFFE0F2FE).copy(alpha = 0.10f * intensity),
                            Color.White.copy(alpha = 0.16f * intensity)
                        )
                    )
                )
                // Specular diagonal glass reflection band
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f * intensity), Color.Transparent)
                    ),
                    start = Offset(0f, h * 0.18f),
                    end = Offset(w, h * 0.42f),
                    strokeWidth = h * 0.18f
                )
            }
            WallpaperGlassStyle.FLUTED_REEDED -> {
                val ribs = if (isMiniPreview) 10 else 22
                val ribW = w / ribs
                for (i in 0 until ribs) {
                    val x = i * ribW
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f * intensity),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.30f * intensity),
                                Color.White.copy(alpha = 0.08f * intensity)
                            ),
                            startX = x,
                            endX = x + ribW
                        ),
                        topLeft = Offset(x, 0f),
                        size = Size(ribW, h)
                    )
                }
            }
            WallpaperGlassStyle.PRISM_CRYSTAL -> {
                val bands = if (isMiniPreview) 5 else 9
                for (i in 0 until bands) {
                    val startX = (i.toFloat() / bands) * w * 1.4f - w * 0.2f
                    val prismColor = when (i % 3) {
                        0 -> VitroCyanAccent
                        1 -> Color.White
                        else -> Color(0xFFC084FC)
                    }
                    drawLine(
                        color = prismColor.copy(alpha = 0.15f * intensity),
                        start = Offset(startX, 0f),
                        end = Offset(startX - w * 0.45f, h),
                        strokeWidth = w * 0.09f
                    )
                }
            }
            WallpaperGlassStyle.RAIN_DROPLETS -> {
                val count = if (isMiniPreview) 12 else 34
                for (i in 0 until count) {
                    val x = ((i * 61 + 13) % 94 + 3) / 100f * w
                    val y = ((i * 37 + 19) % 94 + 3) / 100f * h
                    val r = (if (isMiniPreview) 2.5f else 4.5f) + (i % 5) * (if (isMiniPreview) 1f else 2.2f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f * intensity),
                        radius = r,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f * intensity),
                        radius = r,
                        center = Offset(x, y),
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * intensity),
                        radius = r * 0.28f,
                        center = Offset(x - r * 0.3f, y - r * 0.3f)
                    )
                }
            }
            WallpaperGlassStyle.LIQUID_SHIMMER -> {
                val sweepX = (-0.4f + phase * 1.8f) * w
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.28f * intensity),
                            VitroCyanAccent.copy(alpha = 0.16f * intensity),
                            Color.Transparent
                        ),
                        startX = sweepX - w * 0.35f,
                        endX = sweepX + w * 0.35f
                    ),
                    start = Offset(sweepX, -h * 0.1f),
                    end = Offset(sweepX - w * 0.35f, h * 1.1f),
                    strokeWidth = w * 0.45f
                )
            }
        }
    }
}

@Composable
private fun WallpaperParticleLayer(
    overlay: WallpaperParticleOverlay,
    phase: Float,
    isMiniPreview: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * (2f * PI.toFloat())
        val count = if (isMiniPreview) 10 else 26

        when (overlay) {
            WallpaperParticleOverlay.NONE -> Unit
            WallpaperParticleOverlay.STARDUST -> {
                for (i in 0 until count) {
                    val x = ((i * 71 + 17) % 100) / 100f * w
                    val y = (((i * 43) / 100f - phase * 0.25f) % 1f + 1f) % 1f * h
                    val alpha = 0.25f + 0.65f * ((sin(t * 2f + i * 1.1f) + 1f) / 2f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = if (i % 4 == 0) 2.8f else 1.5f,
                        center = Offset(x, y)
                    )
                }
            }
            WallpaperParticleOverlay.BOKEH_ORBS -> {
                val orbCount = if (isMiniPreview) 5 else 12
                for (i in 0 until orbCount) {
                    val x = ((i * 59 + 11) % 90 + 5) / 100f * w + sin(t + i) * 18f
                    val y = (((i * 0.13f) - phase * 0.3f) % 1.2f + 1.2f) % 1.2f * h - h * 0.1f
                    val r = (if (isMiniPreview) 10f else 22f) + (i % 4) * (if (isMiniPreview) 4f else 10f)
                    val col = when (i % 3) {
                        0 -> VitroCyanAccent
                        1 -> Color.White
                        else -> VitroAmber
                    }
                    drawCircle(
                        color = col.copy(alpha = 0.13f),
                        radius = r,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = col.copy(alpha = 0.24f),
                        radius = r,
                        center = Offset(x, y),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
            WallpaperParticleOverlay.FIREFLIES -> {
                for (i in 0 until count) {
                    val x = ((i * 47 + 9) % 96 + 2) / 100f * w + sin(t * 1.5f + i) * 26f
                    val y = ((i * 31 + 15) % 92 + 4) / 100f * h + cos(t * 1.2f + i * 0.7f) * 22f
                    val pulse = 0.25f + 0.75f * ((sin(t * 3f + i * 1.3f) + 1f) / 2f)
                    val col = if (i % 2 == 0) VitroMint else VitroCyanAccent
                    drawCircle(col.copy(alpha = pulse * 0.22f), radius = 11f, center = Offset(x, y))
                    drawCircle(col.copy(alpha = pulse * 0.90f), radius = 2.6f, center = Offset(x, y))
                }
            }
            WallpaperParticleOverlay.SNOW_CRYSTAL -> {
                for (i in 0 until count) {
                    val drift = sin(t + i * 0.8f) * 18f
                    val x = (((i * 63 + 7) % 100) / 100f * w + drift).coerceIn(0f, w)
                    val y = (((i * 0.11f) + phase * (0.6f + (i % 3) * 0.2f)) % 1f) * h
                    val r = 1.8f + (i % 4) * 1.1f
                    drawCircle(Color.White.copy(alpha = 0.65f), radius = r, center = Offset(x, y))
                }
            }
        }
    }
}

private fun DrawScope.drawGlowingOrb(
    center: Offset,
    radius: Float,
    inner: Color,
    mid: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(inner, mid, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
