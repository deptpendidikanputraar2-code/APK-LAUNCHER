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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
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
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
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
                onCycleStack = onCycleStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
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
                onCycleStack = onCycleStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
                onOpenStudio = onOpenStudio,
                modifier = modifier
            )
        }

        WidgetStackMode.GRID_SEPARATE -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val first = activeWidgets.first()
                VitroGlassContainer(
                    cornerRadius = 26.dp,
                    glassOpacity = config.widgetGlassOpacity,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WidgetContentBody(
                        type = first,
                        telemetry = telemetry,
                        overlays = overlays,
                        quickNote = config.quickNoteText,
                        onToggleMedia = onToggleMedia,
                        onToggleFocusTimer = onToggleFocusTimer,
                        onResetFocusTimer = onResetFocusTimer,
                        onOpenStudio = onOpenStudio
                    )
                }
                if (activeWidgets.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        activeWidgets.drop(1).take(2).forEach { wType ->
                            VitroGlassContainer(
                                cornerRadius = 22.dp,
                                glassOpacity = config.widgetGlassOpacity,
                                modifier = Modifier.weight(1f)
                            ) {
                                CompactWidgetCard(
                                    type = wType,
                                    telemetry = telemetry,
                                    overlays = overlays,
                                    onToggleFocusTimer = onToggleFocusTimer
                                )
                            }
                        }
                    }
                }
            }
        }
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
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
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
                        quickNote = quickNote,
                        onToggleMedia = onToggleMedia,
                        onToggleFocusTimer = onToggleFocusTimer,
                        onResetFocusTimer = onResetFocusTimer,
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
    onCycleStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
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
                    quickNote = quickNote,
                    onToggleMedia = onToggleMedia,
                    onToggleFocusTimer = onToggleFocusTimer,
                    onResetFocusTimer = onResetFocusTimer,
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
    quickNote: String,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
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
                            contentDescription = "Atmosfer",
                            tint = VitroAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "27°C",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VitroTextPrimary
                        )
                    }
                    Text(
                        text = "Cerah Berawan",
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
                                else "Suhu ${"%.1f".format(telemetry.batteryTempC)}°C • Optimal",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = "Sesi Fokus & Ambien",
                            style = MaterialTheme.typography.titleSmall,
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "%02d:%02d • %s".format(
                                mins,
                                secs,
                                if (overlays.isFocusRunning) "Berjalan" else "Siap"
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = VitroCyanAccent
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            onToggleFocusTimer()
                            onToggleMedia()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = if (overlays.isFocusRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Mulai atau jeda fokus",
                            tint = VitroTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onResetFocusTimer,
                        modifier = Modifier.size(48.dp)
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
                        text = "Semua jadwal tertata rapi hari ini",
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
    }
}

@Composable
private fun CompactWidgetCard(
    type: WidgetType,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    onToggleFocusTimer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelMedium,
            color = VitroCyanAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        when (type) {
            WidgetType.BATTERY_SYSTEM -> {
                Text(
                    text = "${telemetry.batteryPercent}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VitroMint
                )
                Text(
                    text = if (telemetry.isCharging) "Mengisi daya" else "${"%.1f".format(telemetry.batteryTempC)}°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary
                )
            }
            WidgetType.FOCUS_MEDIA -> {
                val mins = overlays.focusSecondsRemaining / 60
                val secs = overlays.focusSecondsRemaining % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = VitroTextPrimary,
                    modifier = Modifier.bouncyClickable(onClick = onToggleFocusTimer)
                )
                Text(
                    text = if (overlays.isFocusRunning) "Ketuk untuk jeda" else "Ketuk mulai",
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary
                )
            }
            WidgetType.CALENDAR_MINI -> {
                Text(
                    text = telemetry.dayNumber,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VitroTextPrimary
                )
                Text(
                    text = telemetry.monthText,
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> {
                Text(
                    text = telemetry.timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VitroTextPrimary
                )
                Text(
                    text = telemetry.dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
