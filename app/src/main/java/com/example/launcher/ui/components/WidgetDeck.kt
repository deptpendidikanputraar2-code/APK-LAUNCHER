package com.example.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import com.example.launcher.system.VitroAppWidgetManager
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun VitroWidgetSection(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWidgets = config.enabledWidgets.ifEmpty { listOf(WidgetType.CLOCK_WEATHER) }
    val currentIndex = overlays.activeSmartStackIndex.coerceIn(0, activeWidgets.lastIndex)

    when (config.widgetStackMode) {
        WidgetStackMode.SMART_STACK -> {
            SmartStackWidgetView(
                widgets = activeWidgets,
                currentIndex = currentIndex,
                glassOpacity = config.widgetGlassOpacity,
                quickNote = config.quickNoteText,
                telemetry = telemetry,
                overlays = overlays,
                notifications = notifications,
                nowPlaying = nowPlaying,
                installedApps = installedApps,
                isNotificationAccessEnabled = isNotificationAccessEnabled,
                onCycleStack = onCycleStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
                onOpenNotification = onOpenNotification,
                onDismissNotification = onDismissNotification,
                onEnableNotificationAccess = onEnableNotificationAccess,
                onLaunchApp = onLaunchApp,
                onOpenStudio = onOpenStudio,
                modifier = modifier
            )
        }

        WidgetStackMode.CARD_DECK -> {
            CardDeckWidgetView(
                widgets = activeWidgets,
                currentIndex = currentIndex,
                glassOpacity = config.widgetGlassOpacity,
                quickNote = config.quickNoteText,
                telemetry = telemetry,
                overlays = overlays,
                notifications = notifications,
                nowPlaying = nowPlaying,
                installedApps = installedApps,
                isNotificationAccessEnabled = isNotificationAccessEnabled,
                onCycleStack = onCycleStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
                onOpenNotification = onOpenNotification,
                onDismissNotification = onDismissNotification,
                onEnableNotificationAccess = onEnableNotificationAccess,
                onLaunchApp = onLaunchApp,
                onOpenStudio = onOpenStudio,
                modifier = modifier
            )
        }

        WidgetStackMode.GRID_SEPARATE -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activeWidgets.forEach { type ->
                    VitroGlassContainer(
                        cornerRadius = 26.dp,
                        glassOpacity = config.widgetGlassOpacity,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WidgetContentBody(
                            type = type,
                            telemetry = telemetry,
                            overlays = overlays,
                            notifications = notifications,
                            nowPlaying = nowPlaying,
                            installedApps = installedApps,
                            isNotificationAccessEnabled = isNotificationAccessEnabled,
                            quickNote = config.quickNoteText,
                            onToggleMedia = onToggleMedia,
                            onToggleFocusTimer = onToggleFocusTimer,
                            onResetFocusTimer = onResetFocusTimer,
                            onOpenNotification = onOpenNotification,
                            onDismissNotification = onDismissNotification,
                            onEnableNotificationAccess = onEnableNotificationAccess,
                            onLaunchApp = onLaunchApp,
                            onOpenStudio = onOpenStudio
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleBuiltInGlassWidgetCard(
    type: WidgetType,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = config.widgetGlassOpacity,
        modifier = modifier.fillMaxWidth()
    ) {
        WidgetContentBody(
            type = type,
            telemetry = telemetry,
            overlays = overlays,
            notifications = notifications,
            nowPlaying = nowPlaying,
            installedApps = installedApps,
            isNotificationAccessEnabled = isNotificationAccessEnabled,
            quickNote = config.quickNoteText,
            onToggleMedia = onToggleMedia,
            onToggleFocusTimer = onToggleFocusTimer,
            onResetFocusTimer = onResetFocusTimer,
            onOpenNotification = onOpenNotification,
            onDismissNotification = onDismissNotification,
            onEnableNotificationAccess = onEnableNotificationAccess,
            onLaunchApp = onLaunchApp,
            onOpenStudio = onOpenStudio
        )
    }
}

@Composable
fun AndroidSystemWidgetCard(
    appWidgetId: Int,
    label: String,
    height: Dp,
    glassOpacity: Float,
    appWidgetManager: VitroAppWidgetManager,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = glassOpacity * 0.72f,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("android_system_widget_$appWidgetId")
    ) {
        AndroidView(
            factory = { ctx ->
                appWidgetManager.createHostView(ctx, appWidgetId)
                    ?: android.widget.TextView(ctx).apply {
                        text = "Widget Android: $label"
                        setTextColor(android.graphics.Color.WHITE)
                        setPadding(32, 32, 32, 32)
                    }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        )
    }
}

@Composable
private fun SmartStackWidgetView(
    widgets: List<WidgetType>,
    currentIndex: Int,
    glassOpacity: Float,
    quickNote: String,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }

    VitroGlassContainer(
        cornerRadius = 28.dp,
        glassOpacity = glassOpacity,
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_stack_widget")
            .pointerInput(widgets.size) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -36f) onCycleStack(1)
                        else if (dragAccum > 36f) onCycleStack(-1)
                        dragAccum = 0f
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = widgets[currentIndex],
                    transitionSpec = {
                        (slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f)) { it / 2 } +
                            fadeIn(tween(180)) + scaleIn(initialScale = 0.94f)) togetherWith
                            (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(140)))
                    },
                    label = "smart_stack_flip"
                ) { targetWidget ->
                    WidgetContentBody(
                        type = targetWidget,
                        telemetry = telemetry,
                        overlays = overlays,
                        notifications = notifications,
                        nowPlaying = nowPlaying,
                        installedApps = installedApps,
                        isNotificationAccessEnabled = isNotificationAccessEnabled,
                        quickNote = quickNote,
                        onToggleMedia = onToggleMedia,
                        onToggleFocusTimer = onToggleFocusTimer,
                        onResetFocusTimer = onResetFocusTimer,
                        onOpenNotification = onOpenNotification,
                        onDismissNotification = onDismissNotification,
                        onEnableNotificationAccess = onEnableNotificationAccess,
                        onLaunchApp = onLaunchApp,
                        onOpenStudio = onOpenStudio
                    )
                }
            }

            if (widgets.size > 1) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .testTag("smart_stack_cycle_button")
                        .bouncyClickable { onCycleStack(1) }
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Ganti widget tumpuk",
                        tint = VitroTextPrimary.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                    widgets.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(if (idx == currentIndex) 16.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == currentIndex) VitroCyanAccent
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDeckWidgetView(
    widgets: List<WidgetType>,
    currentIndex: Int,
    glassOpacity: Float,
    quickNote: String,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("card_deck_widget")
    ) {
        if (widgets.size >= 3) {
            VitroGlassContainer(
                cornerRadius = 26.dp,
                glassOpacity = glassOpacity * 0.7f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .offset(y = 16.dp)
                    .scale(0.90f)
                    .alpha(0.55f)
            ) {}
        }
        if (widgets.size >= 2) {
            VitroGlassContainer(
                cornerRadius = 26.dp,
                glassOpacity = glassOpacity * 0.85f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .offset(y = 8.dp)
                    .scale(0.95f)
                    .alpha(0.8f)
            ) {}
        }
        VitroGlassContainer(
            cornerRadius = 26.dp,
            glassOpacity = glassOpacity,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(scaleDown = 0.98f) { onCycleStack(1) }
        ) {
            Column {
                WidgetContentBody(
                    type = widgets[currentIndex],
                    telemetry = telemetry,
                    overlays = overlays,
                    notifications = notifications,
                    nowPlaying = nowPlaying,
                    installedApps = installedApps,
                    isNotificationAccessEnabled = isNotificationAccessEnabled,
                    quickNote = quickNote,
                    onToggleMedia = onToggleMedia,
                    onToggleFocusTimer = onToggleFocusTimer,
                    onResetFocusTimer = onResetFocusTimer,
                    onOpenNotification = onOpenNotification,
                    onDismissNotification = onDismissNotification,
                    onEnableNotificationAccess = onEnableNotificationAccess,
                    onLaunchApp = onLaunchApp,
                    onOpenStudio = onOpenStudio
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tumpuk Kartu • ${currentIndex + 1}/${widgets.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroTextSecondary
                    )
                    Text(
                        text = "Ketuk untuk gilir",
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroCyanAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetContentBody(
    type: WidgetType,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    quickNote: String,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit
) {
    when (type) {
        WidgetType.CLOCK_WEATHER -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = telemetry.timeText,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                        color = VitroTextPrimary
                    )
                    Text(
                        text = telemetry.dateText,
                        style = MaterialTheme.typography.titleSmall,
                        color = VitroTextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WbTwilight,
                            contentDescription = "Status",
                            tint = VitroAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = VitroTextPrimary
                        )
                    }
                    Text(
                        text = "${telemetry.networkLabel} • ${"%.1f".format(telemetry.batteryTempC)}°C",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroCyanAccent
                    )
                }
            }
        }

        WidgetType.BATTERY_SYSTEM -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull
                            else Icons.Rounded.BatteryStd,
                            contentDescription = "Baterai perangkat",
                            tint = VitroMint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Daya Perangkat",
                                style = MaterialTheme.typography.titleSmall,
                                color = VitroTextPrimary
                            )
                            Text(
                                text = if (telemetry.isCharging) "Sedang mengisi • ${"%.1f".format(telemetry.batteryTempC)}°C"
                                else "Suhu ${"%.1f".format(telemetry.batteryTempC)}°C • ${if (telemetry.isPowerSaveMode) "Hemat Daya" else "Optimal"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VitroTextSecondary
                            )
                        }
                    }
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VitroMint
                    )
                }
                LinearProgressIndicator(
                    progress = { (telemetry.batteryPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = VitroMint,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )
            }
        }

        WidgetType.FOCUS_MEDIA -> {
            val mins = overlays.focusSecondsRemaining / 60
            val secs = overlays.focusSecondsRemaining % 60
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VitroIndigo.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Fokus & Media",
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = nowPlaying.title.ifBlank { "Sesi Fokus & Media" },
                            style = MaterialTheme.typography.titleSmall,
                            color = VitroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (nowPlaying.hasActiveSession) {
                                nowPlaying.artist.ifBlank { nowPlaying.appName }
                            } else {
                                "%02d:%02d • %s".format(mins, secs, if (overlays.isFocusRunning) "Berjalan" else "Siap")
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = VitroCyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (nowPlaying.hasActiveSession) onToggleMedia()
                            else {
                                onToggleFocusTimer()
                                onToggleMedia()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = if (overlays.isFocusRunning || nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Mulai atau jeda",
                            tint = VitroTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onResetFocusTimer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Atur ulang timer",
                            tint = VitroTextSecondary
                        )
                    }
                }
            }
        }

        WidgetType.CALENDAR_MINI -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(VitroCyanAccent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = telemetry.dayNumber,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VitroTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Kalender",
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = telemetry.monthText,
                            style = MaterialTheme.typography.labelLarge,
                            color = VitroCyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = telemetry.dateText,
                        style = MaterialTheme.typography.titleMedium,
                        color = VitroTextPrimary
                    )
                    Text(
                        text = "Tersinkron dengan jam sistem HP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VitroTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WidgetType.QUICK_NOTE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(onClick = onOpenStudio)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = "Catatan cepat",
                    tint = VitroAmber,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Catatan Cepat",
                        style = MaterialTheme.typography.labelLarge,
                        color = VitroAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = quickNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VitroTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WidgetType.LIVE_NOTIFICATIONS -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Notifikasi HP (${notifications.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = VitroCyanAccent
                        )
                    }
                    if (!isNotificationAccessEnabled) {
                        Text(
                            text = "Aktifkan →",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroAmber,
                            modifier = Modifier.clickable(onClick = onEnableNotificationAccess)
                        )
                    }
                }

                if (!isNotificationAccessEnabled) {
                    Text(
                        text = "Ketuk untuk mengaktifkan Akses Notifikasi agar pesan WhatsApp, SMS & aplikasi tampil real-time di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VitroTextSecondary,
                        modifier = Modifier.clickable(onClick = onEnableNotificationAccess)
                    )
                } else if (notifications.isEmpty()) {
                    Text(
                        text = "Tidak ada notifikasi baru saat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VitroTextSecondary
                    )
                } else {
                    notifications.take(2).forEach { notif ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onOpenNotification(notif) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (notif.appIcon != null) {
                                Image(
                                    bitmap = notif.appIcon,
                                    contentDescription = notif.appName,
                                    modifier = Modifier.size(26.dp).clip(CircleShape)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${notif.title} • ${notif.appName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = VitroTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = notif.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VitroTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDismissNotification(notif.key) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Hapus",
                                    tint = VitroTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        WidgetType.STORAGE_RAM -> {
            val ramPct = (telemetry.ramUsedGb / telemetry.ramTotalGb).coerceIn(0.05f, 1f)
            val storagePct = (telemetry.storageUsedGb / telemetry.storageTotalGb).coerceIn(0.05f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(16.dp))
                        Text("RAM Perangkat", style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                    }
                    Text(
                        text = "${"%.1f".format(telemetry.ramUsedGb)} / ${"%.1f".format(telemetry.ramTotalGb)} GB",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroCyanAccent
                    )
                }
                LinearProgressIndicator(
                    progress = { ramPct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = VitroCyanAccent,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Storage, contentDescription = null, tint = VitroMint, modifier = Modifier.size(16.dp))
                        Text("Penyimpanan Internal", style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                    }
                    Text(
                        text = "${"%.0f".format(telemetry.storageUsedGb)} / ${"%.0f".format(telemetry.storageTotalGb)} GB",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroMint
                    )
                }
                LinearProgressIndicator(
                    progress = { storagePct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = VitroMint,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )
            }
        }

        WidgetType.APP_SHORTCUTS -> {
            val shortcutApps = installedApps.take(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pintasan Cepat",
                    style = MaterialTheme.typography.labelLarge,
                    color = VitroCyanAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    shortcutApps.forEach { app ->
                        Column(
                            modifier = Modifier
                                .width(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onLaunchApp(app) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.label.take(1).uppercase(), color = VitroTextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.label,
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
    }
}
