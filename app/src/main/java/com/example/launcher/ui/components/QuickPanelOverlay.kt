package com.example.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.QuickModuleConfig
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

/**
 * Pure iPhone-Glass Control Center (Pusat Kontrol).
 * Strictly contains ONLY device controls — ZERO settings or Studio buttons inside the panel.
 * Every module's width (23%..100%) and height (56..220dp) is freely configurable inside Studio Vitro.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanelOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    nowPlaying: NowPlayingInfo,
    onDismiss: () -> Unit,
    onModuleClick: (QuickModuleType) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit
) {
    var dismissDragSum by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            animationSpec = spring(
                dampingRatio = config.transitionSpeed.dampingRatio,
                stiffness = config.transitionSpeed.stiffness
            )
        ) { -it / 4 },
        exit = fadeOut() + slideOutVertically { -it / 4 }
    ) {
        BackHandler(onBack = onDismiss)

        val safeTopModifier = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 8.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD8040814))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .then(safeTopModifier)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("quick_panel_overlay"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Clean Control Center Header (Live Status + Close only, zero settings clutter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = telemetry.timeText,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "${telemetry.dateText} • ${telemetry.networkLabel} • ${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroCyanAccent
                        )
                    }
                    VitroGlassContainer(
                        cornerRadius = 50.dp,
                        glassOpacity = 0.24f,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("quick_panel_close_button")
                            .bouncyClickable(onClick = onDismiss)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = VitroTextPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                val enabledModules = config.quickModules.filter { it.enabled }

                if (config.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS) {
                    // iPhone Glass Layout: Freely-Sized Module Grid + Vertical Glass Capsule Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            enabledModules.forEach { mod ->
                                // Respect free user widthPercent & heightDp
                                val adjustedFraction = mod.widthPercent.coerceIn(0.28f, 1.0f).let { raw ->
                                    if (raw >= 0.88f) 1f else (raw - 0.03f).coerceAtLeast(0.28f)
                                }
                                QuickPanelModuleTile(
                                    module = mod,
                                    telemetry = telemetry,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    onClick = { onModuleClick(mod.type) },
                                    modifier = Modifier
                                        .fillMaxWidth(adjustedFraction)
                                        .height(mod.heightDp.coerceIn(56, 220).dp)
                                )
                            }
                        }

                        if (config.showBrightnessSlider || config.showVolumeSlider) {
                            val sliderH = config.sliderHeightDp.coerceIn(100, 220).dp
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (config.showBrightnessSlider) {
                                    VerticalGlassCapsuleSlider(
                                        value = telemetry.brightnessFraction,
                                        onValueChange = onBrightnessChange,
                                        icon = Icons.Rounded.WbSunny,
                                        label = "Cahaya",
                                        accent = VitroAmber,
                                        glassOpacity = config.quickPanelGlassOpacity,
                                        height = sliderH,
                                        testTag = "qp_brightness_slider"
                                    )
                                }
                                if (config.showVolumeSlider) {
                                    VerticalGlassCapsuleSlider(
                                        value = telemetry.volumeFraction,
                                        onValueChange = onVolumeChange,
                                        icon = Icons.Rounded.VolumeUp,
                                        label = "Suara",
                                        accent = VitroCyanAccent,
                                        glassOpacity = config.quickPanelGlassOpacity,
                                        height = sliderH,
                                        testTag = "qp_volume_slider"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // HORIZONTAL STRIP MODE: Wide glass sliders + freely sized flowing modules
                    if (config.showBrightnessSlider || config.showVolumeSlider) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (config.showBrightnessSlider) {
                                HorizontalGlassSliderCard(
                                    label = "Kecerahan Layar",
                                    value = telemetry.brightnessFraction,
                                    icon = Icons.Rounded.WbSunny,
                                    accent = VitroAmber,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    onValueChange = onBrightnessChange
                                )
                            }
                            if (config.showVolumeSlider) {
                                HorizontalGlassSliderCard(
                                    label = "Volume Media",
                                    value = telemetry.volumeFraction,
                                    icon = Icons.Rounded.VolumeUp,
                                    accent = VitroCyanAccent,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    onValueChange = onVolumeChange
                                )
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        enabledModules.forEach { mod ->
                            val adjustedFraction = mod.widthPercent.coerceIn(0.23f, 1.0f).let { raw ->
                                if (raw >= 0.90f) 1f else (raw - 0.025f).coerceAtLeast(0.22f)
                            }
                            QuickPanelModuleTile(
                                module = mod,
                                telemetry = telemetry,
                                glassOpacity = config.quickPanelGlassOpacity,
                                onClick = { onModuleClick(mod.type) },
                                modifier = Modifier
                                    .fillMaxWidth(adjustedFraction)
                                    .height(mod.heightDp.coerceIn(56, 220).dp)
                            )
                        }
                    }
                }

                // Real Synced Media Player Pill
                if (config.showMediaControl) {
                    VitroGlassContainer(
                        cornerRadius = 26.dp,
                        glassOpacity = config.quickPanelGlassOpacity,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = "Media",
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nowPlaying.title.ifBlank {
                                            if (overlays.isMediaPlaying) "Audio Sedang Diputar" else "Pemutar Media HP"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VitroTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = nowPlaying.artist.ifBlank {
                                            nowPlaying.appName.ifBlank { "Tersinkron dengan audio sistem" }
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VitroTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onMediaPrev, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = VitroTextPrimary)
                                }
                                IconButton(
                                    onClick = onMediaPlayPause,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = if (nowPlaying.isPlaying || overlays.isMediaPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Putar atau jeda",
                                        tint = VitroTextPrimary
                                    )
                                }
                                IconButton(onClick = onMediaNext, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = VitroTextPrimary)
                                }
                            }
                        }
                    }
                }

                // Bottom Swipe-Up Dismiss Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { dismissDragSum = 0f },
                                onVerticalDrag = { _, dragAmount -> dismissDragSum += dragAmount },
                                onDragEnd = {
                                    if (dismissDragSum < -40f) onDismiss()
                                    dismissDragSum = 0f
                                }
                            )
                        }
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.42f))
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPanelModuleTile(
    module: QuickModuleConfig,
    telemetry: DeviceTelemetry,
    glassOpacity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, isActive, activeColor) = when (module.type) {
        QuickModuleType.WIFI -> Triple(Icons.Rounded.Wifi, telemetry.isWifiConnected, VitroCyanAccent)
        QuickModuleType.CELLULAR -> Triple(Icons.Rounded.SignalCellularAlt, telemetry.isCellularConnected, VitroMint)
        QuickModuleType.BLUETOOTH -> Triple(Icons.Rounded.Bluetooth, telemetry.isBluetoothOn, Color(0xFF3B82F6))
        QuickModuleType.FLASHLIGHT -> Triple(Icons.Rounded.FlashlightOn, telemetry.isFlashlightOn, VitroAmber)
        QuickModuleType.DND -> Triple(Icons.Rounded.DoNotDisturbOn, telemetry.isDndEnabled, VitroIndigo)
        QuickModuleType.ROTATION -> Triple(Icons.Rounded.ScreenRotation, telemetry.isAutoRotateEnabled, VitroCyanAccent)
        QuickModuleType.AIRPLANE -> Triple(Icons.Rounded.AirplanemodeActive, telemetry.isAirplaneModeOn, VitroAmber)
        QuickModuleType.HOTSPOT -> Triple(Icons.Rounded.WifiTethering, false, VitroMint)
        QuickModuleType.LOCATION -> Triple(Icons.Rounded.LocationOn, telemetry.isLocationEnabled, VitroCyanAccent)
        QuickModuleType.BATTERY_SAVER -> Triple(Icons.Rounded.BatterySaver, telemetry.isPowerSaveMode, VitroCoral)
        QuickModuleType.SCREENSHOT -> Triple(Icons.Rounded.Screenshot, false, VitroCyanAccent)
        QuickModuleType.CAMERA -> Triple(Icons.Rounded.PhotoCamera, false, VitroIndigo)
        QuickModuleType.LOCK_SCREEN -> Triple(Icons.Rounded.Lock, false, Color(0xFF94A3B8))
    }

    val isWide = module.widthPercent >= 0.46f
    val isTall = module.heightDp >= 115

    VitroGlassContainer(
        cornerRadius = 24.dp,
        tintColor = if (isActive) activeColor else Color.White,
        glassOpacity = if (isActive) (glassOpacity + 0.16f).coerceAtMost(0.65f) else glassOpacity,
        borderColor = if (isActive) activeColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.32f),
        modifier = modifier
            .testTag("qp_module_${module.type.name.lowercase()}")
            .bouncyClickable(onClick = onClick)
    ) {
        when {
            isWide && !isTall -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isActive) activeColor else Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = module.type.label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.type.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isActive) "Aktif" else "Ketuk",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            }

            isTall -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isActive) activeColor else Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = module.type.label,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = module.type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isActive) "Aktif" else "Mati",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = module.type.label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalGlassCapsuleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    label: String,
    accent: Color,
    glassOpacity: Float,
    height: Dp,
    testTag: String
) {
    val clamped = value.coerceIn(0.05f, 1f)
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = glassOpacity,
        modifier = Modifier
            .width(72.dp)
            .height(height)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = -dragAmount / size.height.toFloat()
                    onValueChange((clamped + delta).coerceIn(0.05f, 1f))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(clamped)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.85f),
                            accent.copy(alpha = 0.52f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = VitroTextPrimary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun HorizontalGlassSliderCard(
    label: String,
    value: Float,
    icon: ImageVector,
    accent: Color,
    glassOpacity: Float,
    onValueChange: (Float) -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 24.dp,
        glassOpacity = glassOpacity,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroTextPrimary
                    )
                    Text(
                        text = "${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
