package com.example.launcher.ui.components

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.launcher.model.BlackBerryLedConfig
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DynamicIslandConfig
import com.example.launcher.model.LedBlinkPattern
import com.example.launcher.model.LedColorPreset
import com.example.launcher.model.LedIndicatorTrigger
import com.example.launcher.model.LedPlacement
import com.example.launcher.model.LiveNotification
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

data class ResolvedLedState(
    val isActive: Boolean,
    val colorHex: Long,
    val pattern: LedBlinkPattern,
    val blinkIntervalMs: Int,
    val sizeDp: Float,
    val glowIntensity: Float,
    val triggerNotification: LiveNotification? = null,
    val statusLabel: String = ""
)

/**
 * Pure BlackBerry Dual-LED evaluator shared by Compose UI (Home/Lock/Studio)
 * and native WindowManager overlay (system-wide across all apps).
 */
object BlackBerryLedEvaluator {

    fun evaluateAppLed(
        config: BlackBerryLedConfig,
        notifications: List<LiveNotification>,
        nowMillis: Long,
        manualTestStartMillis: Long = 0L
    ): ResolvedLedState {
        val lamp = config.appLed
        if (!config.masterEnabled || !lamp.enabled) {
            return ResolvedLedState(
                isActive = false,
                colorHex = lamp.colorHex,
                pattern = lamp.pattern,
                blinkIntervalMs = lamp.blinkIntervalMs,
                sizeDp = lamp.sizeDp,
                glowIntensity = lamp.glowIntensity
            )
        }

        // Check manual test window (from Studio "Uji Nyala LED 1")
        if (manualTestStartMillis > 0L) {
            val testDurationMs = if (lamp.durationSeconds > 0) {
                lamp.durationSeconds.coerceIn(5, 30) * 1000L
            } else {
                10_000L
            }
            if (nowMillis - manualTestStartMillis in 0..testDurationMs) {
                val previewHex = config.appRules.firstOrNull()?.colorHex ?: lamp.colorHex
                return ResolvedLedState(
                    isActive = true,
                    colorHex = previewHex,
                    pattern = lamp.pattern,
                    blinkIntervalMs = lamp.blinkIntervalMs,
                    sizeDp = lamp.sizeDp,
                    glowIntensity = lamp.glowIntensity,
                    statusLabel = "Uji LED 1"
                )
            }
        }

        val candidates = notifications.filterNot { it.isOngoing || it.isMedia }
        val rulesMap = config.appRules.associateBy { it.packageName }

        val matchedNotif: LiveNotification? = when {
            rulesMap.isNotEmpty() -> {
                candidates.firstOrNull { it.packageName in rulesMap }
                    ?: if (config.appLedAllAppsFallback) candidates.firstOrNull() else null
            }
            config.appLedAllAppsFallback -> candidates.firstOrNull()
            else -> null
        }

        if (matchedNotif == null) {
            return ResolvedLedState(
                isActive = false,
                colorHex = lamp.colorHex,
                pattern = lamp.pattern,
                blinkIntervalMs = lamp.blinkIntervalMs,
                sizeDp = lamp.sizeDp,
                glowIntensity = lamp.glowIntensity
            )
        }

        // Enforce configurable duration (0 = keep blinking until notification is opened/cleared)
        if (lamp.durationSeconds > 0) {
            val elapsedMs = (nowMillis - matchedNotif.timestampMillis).coerceAtLeast(0L)
            if (elapsedMs > lamp.durationSeconds * 1000L) {
                return ResolvedLedState(
                    isActive = false,
                    colorHex = lamp.colorHex,
                    pattern = lamp.pattern,
                    blinkIntervalMs = lamp.blinkIntervalMs,
                    sizeDp = lamp.sizeDp,
                    glowIntensity = lamp.glowIntensity
                )
            }
        }

        val resolvedHex = rulesMap[matchedNotif.packageName]?.colorHex ?: lamp.colorHex
        return ResolvedLedState(
            isActive = true,
            colorHex = resolvedHex,
            pattern = lamp.pattern,
            blinkIntervalMs = lamp.blinkIntervalMs,
            sizeDp = lamp.sizeDp,
            glowIntensity = lamp.glowIntensity,
            triggerNotification = matchedNotif,
            statusLabel = matchedNotif.appName
        )
    }

    fun evaluateIndicatorLed(
        config: BlackBerryLedConfig,
        telemetry: DeviceTelemetry,
        nowMillis: Long,
        conditionStartMillis: Long = 0L,
        manualTestStartMillis: Long = 0L
    ): ResolvedLedState {
        val lamp = config.indicatorLed
        if (!config.masterEnabled || !lamp.enabled) {
            return ResolvedLedState(
                isActive = false,
                colorHex = lamp.colorHex,
                pattern = lamp.pattern,
                blinkIntervalMs = lamp.blinkIntervalMs,
                sizeDp = lamp.sizeDp,
                glowIntensity = lamp.glowIntensity
            )
        }

        if (manualTestStartMillis > 0L) {
            val testDurationMs = if (lamp.durationSeconds > 0) {
                lamp.durationSeconds.coerceIn(5, 30) * 1000L
            } else {
                10_000L
            }
            if (nowMillis - manualTestStartMillis in 0..testDurationMs) {
                return ResolvedLedState(
                    isActive = true,
                    colorHex = lamp.colorHex,
                    pattern = lamp.pattern,
                    blinkIntervalMs = lamp.blinkIntervalMs,
                    sizeDp = lamp.sizeDp,
                    glowIntensity = lamp.glowIntensity,
                    statusLabel = "Uji LED 2"
                )
            }
        }

        val isDndOrSilent = telemetry.isDndEnabled ||
            telemetry.ringerMode != AudioManager.RINGER_MODE_NORMAL
        val hasNetwork = telemetry.isWifiConnected || telemetry.isCellularConnected

        var active = false
        var autoHex = lamp.colorHex
        var label = ""

        when (config.indicatorTrigger) {
            LedIndicatorTrigger.SMART_AUTO -> {
                when {
                    telemetry.isCharging && telemetry.batteryPercent >= 98 -> {
                        active = true
                        autoHex = LedColorPreset.BBM_GREEN.hex
                        label = "Baterai Penuh"
                    }
                    telemetry.isCharging -> {
                        active = true
                        autoHex = LedColorPreset.AMBER_GOLD.hex
                        label = "Mengisi Daya"
                    }
                    telemetry.batteryPercent <= 20 -> {
                        active = true
                        autoHex = LedColorPreset.BB_RED.hex
                        label = "Baterai Lemah"
                    }
                    isDndOrSilent -> {
                        active = true
                        autoHex = LedColorPreset.VIOLET_NEON.hex
                        label = "Mode Hening/DND"
                    }
                    hasNetwork -> {
                        active = true
                        autoHex = lamp.colorHex
                        label = "Jaringan Aktif"
                    }
                }
            }
            LedIndicatorTrigger.CHARGING -> {
                if (telemetry.isCharging) {
                    active = true
                    autoHex = if (telemetry.batteryPercent >= 98) LedColorPreset.BBM_GREEN.hex else lamp.colorHex
                    label = if (telemetry.batteryPercent >= 98) "Penuh" else "Mengisi Daya"
                }
            }
            LedIndicatorTrigger.LOW_BATTERY -> {
                if (telemetry.batteryPercent <= 20 && !telemetry.isCharging) {
                    active = true
                    autoHex = lamp.colorHex
                    label = "Baterai Lemah"
                }
            }
            LedIndicatorTrigger.NETWORK_SIGNAL -> {
                if (hasNetwork) {
                    active = true
                    autoHex = lamp.colorHex
                    label = telemetry.networkLabel
                }
            }
            LedIndicatorTrigger.DND_SILENT -> {
                if (isDndOrSilent) {
                    active = true
                    autoHex = lamp.colorHex
                    label = "Mode Hening"
                }
            }
            LedIndicatorTrigger.ALWAYS_ON -> {
                active = true
                autoHex = lamp.colorHex
                label = "Aktif"
            }
        }

        if (active && lamp.durationSeconds > 0 && conditionStartMillis > 0L) {
            if (nowMillis - conditionStartMillis > lamp.durationSeconds * 1000L) {
                active = false
            }
        }

        val finalColor = if (
            config.indicatorTrigger == LedIndicatorTrigger.SMART_AUTO &&
            config.indicatorUseAutoStatusColor
        ) autoHex else lamp.colorHex

        return ResolvedLedState(
            isActive = active,
            colorHex = finalColor,
            pattern = lamp.pattern,
            blinkIntervalMs = lamp.blinkIntervalMs,
            sizeDp = lamp.sizeDp,
            glowIntensity = lamp.glowIntensity,
            statusLabel = label
        )
    }

    /**
     * Computes instantaneous diode brightness [0f..1f] for authentic BlackBerry LED rhythms.
     */
    fun computeIntensity(
        pattern: LedBlinkPattern,
        intervalMs: Int,
        elapsedMs: Long
    ): Float {
        val period = intervalMs.coerceIn(400, 4500).toFloat()
        val phase = ((elapsedMs % period.toLong()).toFloat() / period).coerceIn(0f, 1f)

        return when (pattern) {
            LedBlinkPattern.SOLID -> 1.0f
            LedBlinkPattern.CLASSIC_BB -> {
                // Crisp hardware-style BlackBerry pulse: quick flash (~15% duty cycle) then dark rest
                when {
                    phase < 0.03f -> phase / 0.03f
                    phase < 0.11f -> 1.0f
                    phase < 0.18f -> 1f - ((phase - 0.11f) / 0.07f)
                    else -> 0f
                }
            }
            LedBlinkPattern.DOUBLE_PULSE -> {
                // Two quick BBM-style flashes followed by calm dark rest
                when {
                    phase < 0.03f -> phase / 0.03f
                    phase < 0.10f -> 1.0f
                    phase < 0.14f -> 1f - ((phase - 0.10f) / 0.04f)
                    phase < 0.21f -> 0f
                    phase < 0.24f -> (phase - 0.21f) / 0.03f
                    phase < 0.31f -> 1.0f
                    phase < 0.36f -> 1f - ((phase - 0.31f) / 0.05f)
                    else -> 0f
                }
            }
            LedBlinkPattern.HEARTBEAT -> {
                // Smooth double Gaussian "lub-dub" pulse
                val b1 = exp(-((phase - 0.14f) / 0.055f).pow(2)).toFloat()
                val b2 = 0.85f * exp(-((phase - 0.30f) / 0.065f).pow(2)).toFloat()
                (b1 + b2).coerceIn(0f, 1f)
            }
            LedBlinkPattern.BREATHING -> {
                val wave = (1f - cos(2f * PI.toFloat() * phase)) * 0.5f
                0.08f + 0.92f * wave
            }
            LedBlinkPattern.STROBE_FAST -> {
                val fastPeriod = (period * 0.45f).coerceAtLeast(260f)
                val fastPhase = ((elapsedMs % fastPeriod.toLong()).toFloat() / fastPeriod)
                if (fastPhase < 0.44f) 1.0f else 0f
            }
        }
    }
}

/**
 * Dual BlackBerry-style glowing hardware LED diodes displayed over Home Screen & Lock Screen.
 * Zero-drain lifecycle gating: stops ticking immediately when both LEDs are inactive or paused.
 */
@Composable
fun BlackBerryDualLedOverlay(
    ledConfig: BlackBerryLedConfig,
    islandConfig: DynamicIslandConfig,
    telemetry: DeviceTelemetry,
    notifications: List<LiveNotification>,
    isLockScreen: Boolean = false,
    testLed1Timestamp: Long = 0L,
    testLed2Timestamp: Long = 0L,
    onTapAppLedNotification: (LiveNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!ledConfig.masterEnabled) return
    if (isLockScreen && !ledConfig.showOnLockScreen) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isResumed = true
            else if (event == Lifecycle.Event.ON_PAUSE) isResumed = false
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Track when LED 2's system condition changed (for indicator duration countdown)
    val indicatorSignature = remember(
        telemetry.isCharging,
        telemetry.batteryPercent <= 20,
        telemetry.batteryPercent >= 98,
        telemetry.isDndEnabled,
        telemetry.ringerMode,
        telemetry.isWifiConnected,
        telemetry.isCellularConnected,
        ledConfig.indicatorTrigger
    ) {
        "${telemetry.isCharging}|${telemetry.batteryPercent <= 20}|${telemetry.batteryPercent >= 98}|" +
            "${telemetry.isDndEnabled}|${telemetry.ringerMode}|${telemetry.isWifiConnected}|" +
            "${telemetry.isCellularConnected}|${ledConfig.indicatorTrigger}"
    }
    var indicatorConditionStart by remember(indicatorSignature) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val led1 = BlackBerryLedEvaluator.evaluateAppLed(
        config = ledConfig,
        notifications = notifications,
        nowMillis = nowMillis,
        manualTestStartMillis = testLed1Timestamp
    )
    val led2 = BlackBerryLedEvaluator.evaluateIndicatorLed(
        config = ledConfig,
        telemetry = telemetry,
        nowMillis = nowMillis,
        conditionStartMillis = indicatorConditionStart,
        manualTestStartMillis = testLed2Timestamp
    )

    val anyActive = led1.isActive || led2.isActive
    val needsSmoothAnimation = anyActive && (
        (led1.isActive && led1.pattern != LedBlinkPattern.SOLID) ||
            (led2.isActive && led2.pattern != LedBlinkPattern.SOLID)
        )
    val needsDurationCheck = anyActive && (
        (led1.isActive && (ledConfig.appLed.durationSeconds > 0 || testLed1Timestamp > 0L)) ||
            (led2.isActive && (ledConfig.indicatorLed.durationSeconds > 0 || testLed2Timestamp > 0L))
        )

    // Battery-safe animation loop: runs ONLY when active AND foregrounded
    LaunchedEffect(isResumed, needsSmoothAnimation, needsDurationCheck) {
        if (!isResumed) return@LaunchedEffect
        if (needsSmoothAnimation) {
            val frameDelay = if (telemetry.isPowerSaveMode) 90L else 36L
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(frameDelay)
            }
        } else if (needsDurationCheck) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    if (!anyActive) return

    val intensity1 = if (led1.isActive) {
        BlackBerryLedEvaluator.computeIntensity(led1.pattern, led1.blinkIntervalMs, nowMillis)
    } else 0f

    val intensity2 = if (led2.isActive) {
        BlackBerryLedEvaluator.computeIntensity(led2.pattern, led2.blinkIntervalMs, nowMillis)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .testTag("blackberry_dual_led_overlay")
    ) {
        when (ledConfig.placement) {
            LedPlacement.BESIDE_DYNAMIC_ISLAND -> {
                val halfIsland = (islandConfig.widthDp / 2f) + ledConfig.spacingDp + 12f
                val topY = (islandConfig.topOffsetDp + (islandConfig.heightDp / 2f) - 12f).coerceAtLeast(4f)
                // LED 1 on Left of Dynamic Island
                if (led1.isActive) {
                    BlackBerryLedDiode(
                        color = Color(led1.colorHex),
                        intensity = intensity1,
                        sizeDp = led1.sizeDp.dp,
                        glowIntensity = led1.glowIntensity,
                        contentDescription = "Lampu LED Notifikasi Aplikasi (${led1.statusLabel})",
                        onClick = led1.triggerNotification?.let { notif -> { onTapAppLedNotification(notif) } },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = (islandConfig.horizontalOffsetDp - halfIsland).dp,
                                y = topY.dp
                            )
                            .testTag("blackberry_led_1")
                    )
                }
                // LED 2 on Right of Dynamic Island
                if (led2.isActive) {
                    BlackBerryLedDiode(
                        color = Color(led2.colorHex),
                        intensity = intensity2,
                        sizeDp = led2.sizeDp.dp,
                        glowIntensity = led2.glowIntensity,
                        contentDescription = "Lampu LED Indikator Sistem (${led2.statusLabel})",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = (islandConfig.horizontalOffsetDp + halfIsland).dp,
                                y = topY.dp
                            )
                            .testTag("blackberry_led_2")
                    )
                }
            }

            else -> {
                val alignment = when (ledConfig.placement) {
                    LedPlacement.TOP_LEFT_PAIR -> Alignment.TopStart
                    LedPlacement.TOP_CENTER_PAIR -> Alignment.TopCenter
                    else -> Alignment.TopEnd
                }
                val horizontalPad = if (ledConfig.placement == LedPlacement.TOP_CENTER_PAIR) {
                    0.dp
                } else {
                    ledConfig.horizontalOffsetDp.dp
                }

                Row(
                    modifier = Modifier
                        .align(alignment)
                        .padding(
                            top = ledConfig.topOffsetDp.dp,
                            start = if (ledConfig.placement == LedPlacement.TOP_LEFT_PAIR) horizontalPad else 0.dp,
                            end = if (ledConfig.placement == LedPlacement.TOP_RIGHT_PAIR) horizontalPad else 0.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(ledConfig.spacingDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (led1.isActive) {
                        BlackBerryLedDiode(
                            color = Color(led1.colorHex),
                            intensity = intensity1,
                            sizeDp = led1.sizeDp.dp,
                            glowIntensity = led1.glowIntensity,
                            contentDescription = "Lampu LED Notifikasi Aplikasi (${led1.statusLabel})",
                            onClick = led1.triggerNotification?.let { notif -> { onTapAppLedNotification(notif) } },
                            modifier = Modifier.testTag("blackberry_led_1")
                        )
                    }
                    if (led2.isActive) {
                        BlackBerryLedDiode(
                            color = Color(led2.colorHex),
                            intensity = intensity2,
                            sizeDp = led2.sizeDp.dp,
                            glowIntensity = led2.glowIntensity,
                            contentDescription = "Lampu LED Indikator Sistem (${led2.statusLabel})",
                            modifier = Modifier.testTag("blackberry_led_2")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hardware-grade recessed BlackBerry LED diode with layered radial bloom & specular glass lens.
 */
@Composable
fun BlackBerryLedDiode(
    color: Color,
    intensity: Float,
    sizeDp: Dp,
    glowIntensity: Float,
    showDormantLens: Boolean = false,
    contentDescription: String = "Lampu LED BlackBerry",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clamped = intensity.coerceIn(0f, 1f)
    val glow = glowIntensity.coerceIn(0.2f, 1f)
    // Extra outer canvas box for the radiant halo bloom
    val canvasSize = (sizeDp * 2.55f).coerceAtLeast(22.dp)

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Canvas(
        modifier = modifier
            .size(canvasSize)
            .semantics { this.contentDescription = contentDescription }
            .then(clickMod)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val coreRadius = (sizeDp.toPx() / 2f).coerceAtLeast(3f)

        // Subtle obsidian hardware micro-housing ring when visible
        if (showDormantLens || clamped > 0.02f) {
            drawCircle(
                color = Color(0xFF0A0F1A).copy(alpha = if (showDormantLens) 0.92f else 0.65f * clamped),
                radius = coreRadius * 1.24f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = if (showDormantLens) 0.22f else 0.18f * clamped),
                radius = coreRadius * 1.24f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
        }

        if (clamped > 0.01f) {
            // Layer 1: Wide atmospheric outer LED bloom
            val outerRadius = coreRadius * (2.05f + 0.55f * glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = (0.58f * glow * clamped).coerceIn(0f, 0.9f)),
                        color.copy(alpha = (0.22f * glow * clamped).coerceIn(0f, 0.6f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Layer 2: Vivid luminous diode core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (0.85f * clamped).coerceIn(0f, 1f)),
                        color.copy(alpha = (0.98f * clamped).coerceIn(0f, 1f)),
                        color.copy(alpha = (0.72f * clamped).coerceIn(0f, 1f))
                    ),
                    center = center - Offset(coreRadius * 0.22f, coreRadius * 0.22f),
                    radius = coreRadius * 1.15f
                ),
                radius = coreRadius,
                center = center
            )

            // Layer 3: Crisp glass specular highlight pinpoint
            drawCircle(
                color = Color.White.copy(alpha = (0.92f * clamped).coerceIn(0f, 1f)),
                radius = (coreRadius * 0.28f).coerceAtLeast(1.2f),
                center = center - Offset(coreRadius * 0.30f, coreRadius * 0.30f)
            )
        } else if (showDormantLens) {
            // Unlit glass lens bead when dormant in Studio preview
            drawCircle(
                color = color.copy(alpha = 0.16f),
                radius = coreRadius * 0.88f,
                center = center
            )
        }
    }
}
