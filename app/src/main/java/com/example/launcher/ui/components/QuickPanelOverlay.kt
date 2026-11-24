package com.example.launcher.ui.components

import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Vibration
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt

/**
 * Pure iPhone 18 Pro Max Glass Control Center (Pusat Kontrol).
 * - Direct instant module toggling without redirecting out to Android Settings (long-press for detail settings).
 * - Live in-panel Horizontal/Vertical switch and live DIY corner drag resize & reorder right when opened.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanelOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    nowPlaying: NowPlayingInfo,
    feedbackToast: String? = null,
    onDismiss: () -> Unit,
    onModuleClick: (QuickModuleType) -> Unit,
    onModuleLongPress: (QuickModuleType) -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onUpdateModuleSize: (QuickModuleType, Float, Int) -> Unit = { _, _, _ -> },
    onMoveModule: (QuickModuleType, Boolean) -> Unit = { _, _ -> },
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit
) {
    var dismissDragSum by remember { mutableFloatStateOf(0f) }
    var isEditLayoutMode by remember(visible) { mutableStateOf(false) }

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
        BackHandler {
            if (isEditLayoutMode) isEditLayoutMode = false else onDismiss()
        }

        val safeTopModifier = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 8.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDC040814))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isEditLayoutMode) isEditLayoutMode = false else onDismiss()
                    }
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
                    .pointerInput(isEditLayoutMode) {
                        if (!isEditLayoutMode) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dismissDragSum < -90f) onDismiss()
                                    dismissDragSum = 0f
                                },
                                onDragCancel = { dismissDragSum = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    if (dragAmount < 0) dismissDragSum += dragAmount
                                }
                            )
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("quick_panel_overlay"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Live Telemetry + Direct Horizontal/Vertical Toggle + Live DIY Resize Toggle + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditLayoutMode) "Atur Modul Langsung" else telemetry.timeText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEditLayoutMode) VitroCyanAccent else VitroTextPrimary
                        )
                        Text(
                            text = if (isEditLayoutMode) {
                                "Seret sudut kanan bawah modul untuk ubah ukuran"
                            } else {
                                "${telemetry.dateText} • ${telemetry.batteryPercent}% • ${telemetry.networkLabel}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Direct Orientation Switcher (Horizontal <-> Vertical right inside panel)
                        val isHoriz = config.quickPanelOrientation == QuickPanelOrientation.HORIZONTAL_STRIP
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHoriz) VitroCyanAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    0.8.dp,
                                    if (isHoriz) VitroCyanAccent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.18f),
                                    CircleShape
                                )
                                .clickable(onClick = onToggleOrientation)
                                .padding(horizontal = 11.dp)
                                .testTag("quick_panel_orientation_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHoriz) Icons.Rounded.SwapHoriz else Icons.Rounded.SwapVert,
                                    contentDescription = "Ubah Orientasi",
                                    tint = if (isHoriz) VitroCyanAccent else VitroTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHoriz) "Horiz" else "Vert",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHoriz) VitroCyanAccent else VitroTextPrimary
                                )
                            }
                        }

                        // Direct DIY Module Resize Toggle
                        IconButton(
                            onClick = { isEditLayoutMode = !isEditLayoutMode },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isEditLayoutMode) VitroCyanAccent else Color.White.copy(alpha = 0.12f)
                                )
                                .testTag("quick_panel_edit_toggle")
                        ) {
                            Icon(
                                imageVector = if (isEditLayoutMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                                contentDescription = if (isEditLayoutMode) "Selesai Atur" else "Atur Ukuran Modul",
                                tint = if (isEditLayoutMode) Color(0xFF040814) else VitroTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("close_quick_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup Pusat Kontrol",
                                tint = VitroTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Instant Direct Action Feedback Pill
                AnimatedVisibility(
                    visible = !feedbackToast.isNullOrBlank(),
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VitroCyanAccent.copy(alpha = 0.18f))
                            .border(0.9.dp, VitroCyanAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = feedbackToast.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val activeModules = remember(config.quickModules) {
                    config.quickModules.filter { it.enabled }
                }

                if (config.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS) {
                    if (config.showBrightnessSlider || config.showVolumeSlider) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (config.showBrightnessSlider) {
                                VerticalGlassSlider(
                                    label = "Cahaya",
                                    value = telemetry.brightnessFraction,
                                    onValueChange = onBrightnessChange,
                                    icon = Icons.Rounded.WbSunny,
                                    accentColor = VitroAmber,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    heightDp = config.sliderHeightDp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (config.showVolumeSlider) {
                                VerticalGlassSlider(
                                    label = "Volume",
                                    value = telemetry.volumeFraction,
                                    onValueChange = onVolumeChange,
                                    icon = Icons.Rounded.VolumeUp,
                                    accentColor = VitroCyanAccent,
                                    glassOpacity = config.quickPanelGlassOpacity,
                                    heightDp = config.sliderHeightDp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    if (config.showBrightnessSlider) {
                        HorizontalGlassSlider(
                            label = "Kecerahan Layar",
                            value = telemetry.brightnessFraction,
                            onValueChange = onBrightnessChange,
                            icon = Icons.Rounded.WbSunny,
                            accentColor = VitroAmber,
                            glassOpacity = config.quickPanelGlassOpacity
                        )
                    }
                    if (config.showVolumeSlider) {
                        HorizontalGlassSlider(
                            label = "Volume Media",
                            value = telemetry.volumeFraction,
                            onValueChange = onVolumeChange,
                            icon = Icons.Rounded.VolumeUp,
                            accentColor = VitroCyanAccent,
                            glassOpacity = config.quickPanelGlassOpacity
                        )
                    }
                }

                // Live Media Control Card
                if (config.showMediaControl) {
                    VitroGlassContainer(
                        cornerRadius = 24.dp,
                        glassOpacity = config.quickPanelGlassOpacity,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo, Color(0xFFEC4899)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nowPlaying.title.ifBlank { "Pemutar Media" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VitroTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = nowPlaying.artist.ifBlank {
                                        nowPlaying.appName.ifBlank { "Ketuk putar untuk sinkron musik HP" }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VitroTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onMediaPrev, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = VitroTextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable(onClick = onMediaPlayPause),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (nowPlaying.isPlaying || overlays.isMediaPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = "Putar atau jeda",
                                    tint = VitroTextPrimary
                                )
                            }
                            IconButton(onClick = onMediaNext, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = VitroTextPrimary)
                            }
                        }
                    }
                }

                // Free-Sized Interactive Modules Grid (directly resizable in-place when Edit Mode is ON!)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeModules.forEachIndexed { idx, mod ->
                        FreeSizedQuickModuleTile(
                            module = mod,
                            telemetry = telemetry,
                            glassOpacity = config.quickPanelGlassOpacity,
                            isEditMode = isEditLayoutMode,
                            canMoveLeft = idx > 0,
                            canMoveRight = idx < activeModules.lastIndex,
                            onClick = { onModuleClick(mod.type) },
                            onLongPress = { onModuleLongPress(mod.type) },
                            onLiveResize = { newWidth, newHeight ->
                                onUpdateModuleSize(mod.type, newWidth, newHeight)
                            },
                            onMoveLeft = { onMoveModule(mod.type, true) },
                            onMoveRight = { onMoveModule(mod.type, false) }
                        )
                    }
                }

                // Bottom pill indicator for swipe-up dismiss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                            .clickable(onClick = onDismiss)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FreeSizedQuickModuleTile(
    module: QuickModuleConfig,
    telemetry: DeviceTelemetry,
    glassOpacity: Float,
    isEditMode: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onLiveResize: (Float, Int) -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    val density = LocalDensity.current
    val (icon, active, accent, subtitle) = resolveQuickModuleVisuals(module.type, telemetry)
    val clampedWidth = module.widthPercent.coerceIn(0.23f, 1.0f)
    val clampedHeight = module.heightDp.coerceIn(56, 220)

    var dragWidth by remember(module.widthPercent) { mutableFloatStateOf(clampedWidth) }
    var dragHeight by remember(module.heightDp) { mutableFloatStateOf(clampedHeight.toFloat()) }

    val activeWidth = if (isEditMode) dragWidth else clampedWidth
    val activeHeight = if (isEditMode) dragHeight.roundToInt() else clampedHeight
    val isCompactSquare = activeWidth < 0.36f && activeHeight < 110
    val isTallCard = activeHeight >= 120

    Box(
        modifier = Modifier
            .fillMaxWidth(activeWidth.coerceIn(0.23f, 0.99f))
            .height(activeHeight.dp)
    ) {
        VitroGlassContainer(
            cornerRadius = 22.dp,
            glassOpacity = if (active) (glassOpacity + 0.14f).coerceAtMost(0.55f) else glassOpacity,
            tintColor = if (active) accent else Color.White,
            borderColor = when {
                isEditMode -> VitroCyanAccent.copy(alpha = 0.9f)
                active -> accent.copy(alpha = 0.72f)
                else -> Color.White.copy(alpha = 0.24f)
            },
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { if (!isEditMode) onClick() },
                    onLongClick = { if (!isEditMode) onLongPress() }
                )
                .testTag("quick_module_${module.type.name.lowercase()}")
        ) {
            if (isCompactSquare) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = module.type.label,
                        tint = if (active) Color.White else VitroTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    if (activeHeight >= 70) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = module.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (isTallCard) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (active) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = module.type.label,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = module.type.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White.copy(alpha = 0.9f) else VitroTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (active) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = module.type.label,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.type.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White.copy(alpha = 0.9f) else VitroTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live In-Panel DIY Controls (reorder arrows + bottom-right live drag resize handle)
        if (isEditMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (canMoveLeft) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xDD0B132B))
                            .clickable(onClick = onMoveLeft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geser Kiri",
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                if (canMoveRight) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xDD0B132B))
                            .clickable(onClick = onMoveRight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Geser Kanan",
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Live Corner Drag Resize Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(VitroCyanAccent)
                    .pointerInput(module.type) {
                        detectDragGestures(
                            onDragEnd = {
                                onLiveResize(dragWidth, dragHeight.roundToInt())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dxDp = with(density) { dragAmount.x.toDp().value }
                                val dyDp = with(density) { dragAmount.y.toDp().value }
                                dragWidth = (dragWidth + dxDp / 340f).coerceIn(0.23f, 1.0f)
                                dragHeight = (dragHeight + dyDp).coerceIn(56f, 220f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenWith,
                    contentDescription = "Seret Ubah Ukuran",
                    tint = Color(0xFF040814),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private data class QuickModuleVisual(
    val icon: ImageVector,
    val active: Boolean,
    val accent: Color,
    val subtitle: String
)

private fun resolveQuickModuleVisuals(
    type: QuickModuleType,
    telemetry: DeviceTelemetry
): QuickModuleVisual = when (type) {
    QuickModuleType.WIFI -> QuickModuleVisual(
        Icons.Rounded.Wifi,
        telemetry.isWifiConnected,
        VitroCyanAccent,
        if (telemetry.isWifiConnected) "Terhubung" else "Nonaktif"
    )
    QuickModuleType.CELLULAR -> QuickModuleVisual(
        Icons.Rounded.SignalCellularAlt,
        telemetry.isCellularConnected,
        VitroMint,
        if (telemetry.isCellularConnected) "Aktif" else "Mati"
    )
    QuickModuleType.BLUETOOTH -> QuickModuleVisual(
        Icons.Rounded.Bluetooth,
        telemetry.isBluetoothOn,
        VitroIndigo,
        if (telemetry.isBluetoothOn) "Aktif" else "Nonaktif"
    )
    QuickModuleType.FLASHLIGHT -> QuickModuleVisual(
        Icons.Rounded.FlashlightOn,
        telemetry.isFlashlightOn,
        VitroAmber,
        if (telemetry.isFlashlightOn) "Menyala" else "Mati"
    )
    QuickModuleType.DND -> QuickModuleVisual(
        Icons.Rounded.DoNotDisturbOn,
        telemetry.isDndEnabled,
        Color(0xFF8B5CF6),
        if (telemetry.isDndEnabled) "Aktif" else "Mati"
    )
    QuickModuleType.RINGER_MODE -> when (telemetry.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> QuickModuleVisual(
            Icons.Rounded.NotificationsOff,
            true,
            VitroCoral,
            "Hening"
        )
        AudioManager.RINGER_MODE_VIBRATE -> QuickModuleVisual(
            Icons.Rounded.Vibration,
            true,
            VitroIndigo,
            "Getar"
        )
        else -> QuickModuleVisual(
            Icons.Rounded.VolumeUp,
            true,
            VitroCyanAccent,
            "Dering"
        )
    }
    QuickModuleType.ROTATION -> QuickModuleVisual(
        Icons.Rounded.ScreenRotation,
        telemetry.isAutoRotateEnabled,
        VitroCyanAccent,
        if (telemetry.isAutoRotateEnabled) "Otomatis" else "Terkunci"
    )
    QuickModuleType.AIRPLANE -> QuickModuleVisual(
        Icons.Rounded.AirplanemodeActive,
        telemetry.isAirplaneModeOn,
        VitroAmber,
        if (telemetry.isAirplaneModeOn) "Aktif" else "Mati"
    )
    QuickModuleType.HOTSPOT -> QuickModuleVisual(
        Icons.Rounded.WifiTethering,
        false,
        VitroMint,
        "Ketuk cepat"
    )
    QuickModuleType.LOCATION -> QuickModuleVisual(
        Icons.Rounded.LocationOn,
        telemetry.isLocationEnabled,
        VitroCyanAccent,
        if (telemetry.isLocationEnabled) "GPS Aktif" else "Mati"
    )
    QuickModuleType.BATTERY_SAVER -> QuickModuleVisual(
        Icons.Rounded.BatterySaver,
        telemetry.isPowerSaveMode,
        VitroAmber,
        if (telemetry.isPowerSaveMode) "Aktif" else "${telemetry.batteryPercent}%"
    )
    QuickModuleType.SCREENSHOT -> QuickModuleVisual(
        Icons.Rounded.Screenshot,
        false,
        VitroIndigo,
        "Tangkap layar"
    )
    QuickModuleType.CAMERA -> QuickModuleVisual(
        Icons.Rounded.PhotoCamera,
        false,
        VitroCoral,
        "Buka cepat"
    )
    QuickModuleType.LOCK_SCREEN -> QuickModuleVisual(
        Icons.Rounded.Lock,
        false,
        VitroCoral,
        "Kunci layar"
    )
}

@Composable
private fun VerticalGlassSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    accentColor: Color,
    glassOpacity: Float,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = glassOpacity,
        modifier = modifier
            .height(heightDp.coerceIn(100, 220).dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = -dragAmount / size.height.coerceAtLeast(1)
                    onValueChange((value + delta).coerceIn(0.05f, 1f))
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0.05f, 1f))
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.80f),
                                accentColor.copy(alpha = 0.48f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextPrimary
                )
            }
        }
    }
}

@Composable
private fun HorizontalGlassSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    accentColor: Color,
    glassOpacity: Float
) {
    VitroGlassContainer(
        cornerRadius = 22.dp,
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
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = VitroTextPrimary)
                    Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = accentColor)
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
