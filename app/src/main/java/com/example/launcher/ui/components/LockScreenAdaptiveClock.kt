package com.example.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LockClockColor
import com.example.launcher.model.LockClockStyle
import com.example.launcher.model.WidgetType
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Lock Screen Clock supporting Adaptive Clock + 5 aesthetic styles,
 * direct long-press DIY entry, vertical drag positioning, and corner drag resizing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveLockClockHero(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    notificationCount: Int,
    isDiyMode: Boolean,
    onEnterDiy: () -> Unit,
    onUpdateClockLayout: (scale: Float, verticalOffsetDp: Float) -> Unit
) {
    val density = LocalDensity.current
    var liveScale by remember(config.lockClockScale) { mutableFloatStateOf(config.lockClockScale) }
    var liveVOffset by remember(config.lockClockVerticalOffsetDp) { mutableFloatStateOf(config.lockClockVerticalOffsetDp) }

    val effectiveScale = if (isDiyMode) liveScale else config.lockClockScale
    val effectiveVOffset = if (isDiyMode) liveVOffset else config.lockClockVerticalOffsetDp

    // Adaptive Clock intelligence: expands when screen is calm, gracefully adapts when notifications arrive
    val adaptiveFactorTarget = when {
        config.lockClockStyle != LockClockStyle.ADAPTIVE_LIQUID -> 1.0f
        notificationCount == 0 -> 1.16f
        notificationCount == 1 -> 1.02f
        notificationCount == 2 -> 0.92f
        else -> 0.84f
    }
    val adaptiveFactor by animateFloatAsState(
        targetValue = adaptiveFactorTarget,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 280f),
        label = "adaptive_clock_factor"
    )

    val finalScale = (effectiveScale * adaptiveFactor).coerceIn(0.50f, 2.0f)
    val clockColor = config.lockClockColor
    val dynamicWeight = when {
        config.lockClockStyle == LockClockStyle.ADAPTIVE_LIQUID -> {
            if (notificationCount == 0) FontWeight(config.lockClockWeight.coerceIn(300, 900))
            else FontWeight((config.lockClockWeight - 100).coerceIn(300, 800))
        }
        config.lockClockStyle == LockClockStyle.EDITORIAL_THIN -> FontWeight.Thin
        config.lockClockStyle == LockClockStyle.BOLD_KINETIC -> FontWeight.Black
        else -> FontWeight(config.lockClockWeight.coerceIn(300, 900))
    }

    val parts = telemetry.timeText.split(":")
    val hourStr = parts.getOrElse(0) { "09" }
    val minStr = parts.getOrElse(1) { "41" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, with(density) { effectiveVOffset.dp.roundToPx() }) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .then(
                    if (isDiyMode) {
                        Modifier
                            .background(Color(0x55081124))
                            .border(1.5.dp, VitroCyanAccent, RoundedCornerShape(28.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = { onUpdateClockLayout(liveScale, liveVOffset) },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val dyDp = with(density) { drag.y.toDp().value }
                                        liveVOffset = (liveVOffset + dyDp).coerceIn(-60f, 160f)
                                    }
                                )
                            }
                    } else {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onEnterDiy
                        )
                    }
                )
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .testTag("lock_adaptive_clock")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = telemetry.dateText,
                    style = MaterialTheme.typography.titleSmall,
                    color = clockColor.primary.copy(alpha = 0.88f),
                    fontWeight = FontWeight.Medium
                )

                when (config.lockClockStyle) {
                    LockClockStyle.SPLIT_STACK -> {
                        val sizeSp = (76f * finalScale).coerceIn(42f, 134f).sp
                        Text(
                            text = hourStr,
                            style = TextStyle(
                                fontSize = sizeSp,
                                lineHeight = sizeSp * 0.90f,
                                fontWeight = dynamicWeight,
                                brush = Brush.verticalGradient(listOf(clockColor.primary, clockColor.accent))
                            )
                        )
                        Text(
                            text = minStr,
                            style = TextStyle(
                                fontSize = sizeSp,
                                lineHeight = sizeSp * 0.90f,
                                fontWeight = dynamicWeight,
                                brush = Brush.verticalGradient(listOf(clockColor.accent, clockColor.primary.copy(alpha = 0.85f)))
                            )
                        )
                    }

                    LockClockStyle.ANALOG_HYBRID -> {
                        val dialDp = (104f * finalScale).coerceIn(68f, 168f).dp
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LockAnalogDial(dialSizeDp = dialDp, accent = clockColor.accent, primary = clockColor.primary)
                            Column {
                                Text(
                                    text = telemetry.timeText,
                                    style = TextStyle(
                                        fontSize = (40f * finalScale).coerceIn(24f, 68f).sp,
                                        fontWeight = dynamicWeight,
                                        color = clockColor.primary
                                    )
                                )
                                Text(
                                    text = "Adaptive Hybrid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = clockColor.accent
                                )
                            }
                        }
                    }

                    LockClockStyle.IOS_DEPTH, LockClockStyle.ADAPTIVE_LIQUID -> {
                        val sizeSp = (78f * finalScale).coerceIn(42f, 146f).sp
                        Text(
                            text = telemetry.timeText,
                            style = TextStyle(
                                fontSize = sizeSp,
                                fontWeight = dynamicWeight,
                                letterSpacing = (-2f * finalScale).sp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        clockColor.primary,
                                        clockColor.primary.copy(alpha = 0.92f),
                                        clockColor.accent.copy(alpha = 0.85f)
                                    )
                                )
                            )
                        )
                        if (config.lockClockStyle == LockClockStyle.ADAPTIVE_LIQUID && isDiyMode) {
                            Text(
                                text = "Adaptive Clock • ${(effectiveScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = VitroCyanAccent
                            )
                        }
                    }

                    LockClockStyle.EDITORIAL_THIN, LockClockStyle.BOLD_KINETIC -> {
                        val sizeSp = (74f * finalScale).coerceIn(40f, 140f).sp
                        Text(
                            text = telemetry.timeText,
                            style = TextStyle(
                                fontSize = sizeSp,
                                fontWeight = dynamicWeight,
                                letterSpacing = if (config.lockClockStyle == LockClockStyle.EDITORIAL_THIN) 2.sp else (-1.5).sp,
                                color = clockColor.primary
                            )
                        )
                    }
                }
            }

            if (isDiyMode) {
                // Reset Clock Size & Position button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE0B132B))
                        .clickable {
                            liveScale = 1.0f
                            liveVOffset = 0f
                            onUpdateClockLayout(1.0f, 0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset Jam", tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                }

                // Live Drag Corner Handle right on the Lock Screen Clock!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(VitroCyanAccent)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onUpdateClockLayout(liveScale, liveVOffset) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val delta = (with(density) { (drag.x + drag.y).toDp().value }) / 180f
                                    liveScale = (liveScale + delta).coerceIn(0.55f, 1.85f)
                                }
                            )
                        }
                        .testTag("lock_clock_resize_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenWith,
                        contentDescription = "Seret Ubah Ukuran Jam",
                        tint = Color(0xFF040814),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockAnalogDial(
    dialSizeDp: androidx.compose.ui.unit.Dp,
    accent: Color,
    primary: Color
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val h = cal.get(Calendar.HOUR)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)

    Canvas(modifier = Modifier.size(dialSizeDp)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color.White.copy(alpha = 0.10f), radius = r, center = c)
        drawCircle(color = primary.copy(alpha = 0.45f), radius = r - 1.5f, center = c, style = Stroke(2.2f))
        for (i in 0 until 12) {
            val ang = Math.toRadians(i * 30.0 - 90.0)
            drawLine(
                color = if (i % 3 == 0) accent else primary.copy(alpha = 0.45f),
                start = Offset(c.x + (r * 0.80f * cos(ang)).toFloat(), c.y + (r * 0.80f * sin(ang)).toFloat()),
                end = Offset(c.x + (r * 0.92f * cos(ang)).toFloat(), c.y + (r * 0.92f * sin(ang)).toFloat()),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f,
                cap = StrokeCap.Round
            )
        }
        val hAng = ((h + m / 60f) * 30f - 90f) * (PI / 180.0)
        drawLine(primary, c, Offset(c.x + (r * 0.48f * cos(hAng)).toFloat(), c.y + (r * 0.48f * sin(hAng)).toFloat()), 4.5f, StrokeCap.Round)
        val mAng = ((m + s / 60f) * 6f - 90f) * (PI / 180.0)
        drawLine(primary.copy(alpha = 0.88f), c, Offset(c.x + (r * 0.70f * cos(mAng)).toFloat(), c.y + (r * 0.70f * sin(mAng)).toFloat()), 3f, StrokeCap.Round)
        val sAng = (s * 6f - 90f) * (PI / 180.0)
        drawLine(accent, c, Offset(c.x + (r * 0.78f * cos(sAng)).toFloat(), c.y + (r * 0.78f * sin(sAng)).toFloat()), 1.8f, StrokeCap.Round)
        drawCircle(accent, 4f, c)
    }
}

/**
 * Compact frosted glass DIY Editor Bar shown at the bottom of the Lock Screen during Lock DIY Mode.
 */
@Composable
fun LockScreenDiyControlSheet(
    config: LauncherConfig,
    onUpdateClockStyle: (LockClockStyle) -> Unit,
    onUpdateClockColor: (LockClockColor) -> Unit,
    onUpdateClockScale: (Float) -> Unit,
    onUpdateClockWeight: (Int) -> Unit,
    onAddLockWidget: (WidgetType) -> Unit,
    onDone: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 26.dp,
        glassOpacity = 0.86f,
        tintColor = Color(0xFF071124),
        borderColor = VitroCyanAccent.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("lock_diy_control_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIY Layar Kunci & Adaptive Clock",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VitroCyanAccent
                    )
                    Text(
                        text = "Seret sudut jam/widget untuk ubah ukuran langsung",
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(VitroCyanAccent)
                        .clickable(onClick = onDone)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF040814), modifier = Modifier.size(15.dp))
                        Text("Selesai", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF040814))
                    }
                }
            }

            // Clock Style horizontal picker (Adaptive Clock first!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockClockStyle.entries.forEach { style ->
                    val selected = config.lockClockStyle == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) VitroCyanAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.09f))
                            .border(
                                1.dp,
                                if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onUpdateClockStyle(style) }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) VitroCyanAccent else VitroTextPrimary
                        )
                    }
                }
            }

            // Clock Color swatches + Direct Scale Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockClockColor.entries.forEach { c ->
                    val sel = config.lockClockColor == c
                    Box(
                        modifier = Modifier
                            .size(if (sel) 28.dp else 24.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.primary, c.accent)))
                            .border(if (sel) 2.dp else 0.8.dp, if (sel) Color.White else Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { onUpdateClockColor(c) }
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Ukuran ${(config.lockClockScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroCyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ukuran", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary, modifier = Modifier.width(44.dp))
                Slider(
                    value = config.lockClockScale,
                    onValueChange = onUpdateClockScale,
                    valueRange = 0.55f..1.85f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroCyanAccent),
                    modifier = Modifier.weight(1f).height(26.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tebal", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary, modifier = Modifier.width(44.dp))
                Slider(
                    value = config.lockClockWeight.toFloat(),
                    onValueChange = { onUpdateClockWeight((it / 100f).roundToInt() * 100) },
                    valueRange = 300f..900f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroCyanAccent),
                    modifier = Modifier.weight(1f).height(26.dp)
                )
            }

            // Quick Add Lock Screen Glass Widget Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickTypes = listOf(
                    WidgetType.WEATHER_PRO,
                    WidgetType.BATTERY_SYSTEM,
                    WidgetType.MEDIA_WAVEFORM,
                    WidgetType.WORLD_CLOCK,
                    WidgetType.CALENDAR_MINI,
                    WidgetType.DEVICE_RADAR,
                    WidgetType.DAILY_QUOTE
                )
                quickTypes.forEach { wt ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onAddLockWidget(wt) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(13.dp))
                        Text(wt.label, style = MaterialTheme.typography.labelSmall, color = VitroTextPrimary)
                    }
                }
            }
        }
    }
}
