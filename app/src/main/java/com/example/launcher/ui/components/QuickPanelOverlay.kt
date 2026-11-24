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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.ModuleSize
import com.example.launcher.model.QuickModuleConfig
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanelOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    onDismiss: () -> Unit,
    onModuleClick: (QuickModuleType) -> Unit,
    onCycleModuleSize: (QuickModuleType) -> Unit,
    onToggleOrientation: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMedia: () -> Unit,
    onOpenStudio: () -> Unit
) {
    var editLayoutMode by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC040814))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
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
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("quick_panel_overlay"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.quick_panel_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "${config.quickPanelOrientation.label} • ${telemetry.timeText}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroCyanAccent
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VitroGlassContainer(
                            cornerRadius = 50.dp,
                            glassOpacity = if (editLayoutMode) 0.45f else 0.22f,
                            tintColor = if (editLayoutMode) VitroCyanAccent else Color.White,
                            modifier = Modifier
                                .testTag("quick_panel_edit_size_toggle")
                                .bouncyClickable { editLayoutMode = !editLayoutMode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Ubah ukuran modul",
                                    tint = VitroTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (editLayoutMode) "Selesai" else "Ukuran",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VitroTextPrimary
                                )
                            }
                        }

                        VitroGlassContainer(
                            cornerRadius = 50.dp,
                            glassOpacity = 0.24f,
                            modifier = Modifier
                                .size(48.dp)
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
                }

                // Quick orientation pill switcher + edit hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VitroGlassContainer(
                        cornerRadius = 50.dp,
                        glassOpacity = 0.20f,
                        modifier = Modifier
                            .testTag("quick_panel_orientation_toggle")
                            .bouncyClickable(onClick = onToggleOrientation)
                    ) {
                        Text(
                            text = if (config.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS)
                                "Mode: Vertikal Glass iPhone (Ketuk ubah)"
                            else
                                "Mode: Horizontal Strip (Ketuk ubah)",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (editLayoutMode) {
                    VitroGlassContainer(
                        cornerRadius = 18.dp,
                        tintColor = VitroCyanAccent,
                        glassOpacity = 0.22f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mode Ukuran Aktif: Ketuk modul di bawah untuk mengganti ukuran (1×1, 2×1 Horizontal, atau 1×2 Vertikal).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VitroTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                val enabledModules = config.quickModules.filter { it.enabled }

                if (config.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS) {
                    // iPhone Glass Control Center Layout: Modules Flow + Vertical Capsule Sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left & Center Module Grid
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 2
                        ) {
                            enabledModules.forEach { mod ->
                                val widthFraction = when (mod.size) {
                                    ModuleSize.WIDE_2X1 -> 1f
                                    else -> 0.47f
                                }
                                val tileHeight = when (mod.size) {
                                    ModuleSize.TALL_1X2 -> 148.dp
                                    else -> 72.dp
                                }
                                QuickPanelModuleTile(
                                    module = mod,
                                    telemetry = telemetry,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    editMode = editLayoutMode,
                                    onClick = {
                                        if (editLayoutMode) onCycleModuleSize(mod.type)
                                        else onModuleClick(mod.type)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(widthFraction)
                                        .height(tileHeight)
                                )
                            }
                        }

                        // Right Column: iOS-style vertical frosted glass Brightness & Volume Capsules
                        if (config.showBrightnessSlider || config.showVolumeSlider) {
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
                                        testTag = "qp_volume_slider"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // HORIZONTAL STRIP MODE: Wide glass sliders + horizontal flowing modules
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3
                    ) {
                        enabledModules.forEach { mod ->
                            val tileWidth = when (mod.size) {
                                ModuleSize.WIDE_2X1 -> 0.64f
                                else -> 0.305f
                            }
                            val tileHeight = when (mod.size) {
                                ModuleSize.TALL_1X2 -> 124.dp
                                else -> 76.dp
                            }
                            QuickPanelModuleTile(
                                module = mod,
                                telemetry = telemetry,
                                glassOpacity = config.quickPanelGlassOpacity,
                                editMode = editLayoutMode,
                                onClick = {
                                    if (editLayoutMode) onCycleModuleSize(mod.type)
                                    else onModuleClick(mod.type)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(tileWidth)
                                    .height(tileHeight)
                            )
                        }
                    }
                }

                // Media Glass Pill
                if (config.showMediaControl) {
                    VitroGlassContainer(
                        cornerRadius = 26.dp,
                        glassOpacity = config.quickPanelGlassOpacity,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                Column {
                                    Text(
                                        text = if (overlays.isMediaPlaying) "Sedang Diputar" else "Pemutar Media",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VitroTextPrimary
                                    )
                                    Text(
                                        text = if (overlays.isMediaPlaying) "Ambien Kaca • Vitro Audio" else "Ketuk tombol putar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VitroTextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onToggleMedia,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = if (overlays.isMediaPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Putar atau jeda",
                                    tint = VitroTextPrimary
                                )
                            }
                        }
                    }
                }

                // Direct button to Studio Quick Panel editor
                VitroGlassContainer(
                    cornerRadius = 22.dp,
                    tintColor = VitroCyanAccent,
                    glassOpacity = 0.22f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(onClick = onOpenStudio)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Atur Modul & Kustomisasi Lengkap di Studio",
                            style = MaterialTheme.typography.labelLarge,
                            color = VitroTextPrimary
                        )
                    }
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
    editMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, isActive, activeColor) = when (module.type) {
        QuickModuleType.WIFI -> Triple(Icons.Rounded.Wifi, telemetry.isWifiConnected, VitroCyanAccent)
        QuickModuleType.CELLULAR -> Triple(Icons.Rounded.SignalCellularAlt, telemetry.isCellularConnected, VitroMint)
        QuickModuleType.BLUETOOTH -> Triple(Icons.Rounded.Bluetooth, telemetry.isBluetoothVisualOn, Color(0xFF3B82F6))
        QuickModuleType.FLASHLIGHT -> Triple(Icons.Rounded.FlashlightOn, telemetry.isFlashlightOn, VitroAmber)
        QuickModuleType.DND -> Triple(Icons.Rounded.DoNotDisturbOn, telemetry.isDndEnabled, VitroIndigo)
        QuickModuleType.ROTATION -> Triple(Icons.Rounded.ScreenRotation, telemetry.isAutoRotateEnabled, VitroCyanAccent)
        QuickModuleType.LOCK_SCREEN -> Triple(Icons.Rounded.Lock, false, Color(0xFF94A3B8))
        QuickModuleType.STUDIO -> Triple(Icons.Rounded.Tune, true, VitroCyanAccent)
    }

    VitroGlassContainer(
        cornerRadius = 24.dp,
        tintColor = if (isActive) activeColor else Color.White,
        glassOpacity = if (isActive) (glassOpacity + 0.16f).coerceAtMost(0.65f) else glassOpacity,
        borderColor = if (editMode) VitroAmber else if (isActive) activeColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.32f),
        modifier = modifier
            .testTag("qp_module_${module.type.name.lowercase()}")
            .bouncyClickable(onClick = onClick)
    ) {
        when (module.size) {
            ModuleSize.WIDE_2X1 -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            text = if (editMode) "Ukuran: ${module.size.label}" else if (isActive) "Aktif" else "Ketuk",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            }

            ModuleSize.TALL_1X2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isActive) activeColor else Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = module.type.label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
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
                            text = if (editMode) module.size.label else if (isActive) "Aktif" else "Mati",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            }

            ModuleSize.SMALL_1X1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
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
                        text = if (editMode) module.size.label else module.type.label,
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
    testTag: String
) {
    val clamped = value.coerceIn(0.05f, 1f)
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = glassOpacity,
        modifier = Modifier
            .width(74.dp)
            .height(152.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = -dragAmount / size.height.toFloat()
                    onValueChange((clamped + delta).coerceIn(0.05f, 1f))
                }
            }
    ) {
        // Vertical fill level
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
