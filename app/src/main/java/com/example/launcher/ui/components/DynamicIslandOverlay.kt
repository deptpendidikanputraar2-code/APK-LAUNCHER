package com.example.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DynamicIslandConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun VitroDynamicIsland(
    islandConfig: DynamicIslandConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!islandConfig.enabled) return

    var expanded by remember { mutableStateOf(false) }
    val latestNotif = notifications.firstOrNull()
    val hasMedia = islandConfig.showMedia && (nowPlaying.hasActiveSession || overlays.isMediaPlaying)
    val hasFocus = islandConfig.showFocusTimer && overlays.isFocusRunning

    val targetWidth = if (expanded) 340.dp else islandConfig.widthDp.coerceIn(108f, 280f).dp
    val targetHeight = if (expanded) {
        when {
            latestNotif != null && hasMedia -> 218.dp
            latestNotif != null || hasMedia -> 166.dp
            else -> 136.dp
        }
    } else {
        islandConfig.heightDp.coerceIn(26f, 54f).dp
    }
    val targetRadius = if (expanded) 30.dp else islandConfig.cornerRadiusDp.coerceIn(12f, 32f).dp

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "di_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 340f),
        label = "di_height"
    )
    val animatedRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "di_radius"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "di_wave")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "di_wave_pulse"
    )

    val accent = islandConfig.style.accentColor
    val bgBase = islandConfig.style.containerColor
    val glowAlpha = (islandConfig.borderGlow * (if (expanded) 0.9f else 0.65f)).coerceIn(0.12f, 0.92f)
    val shape = RoundedCornerShape(animatedRadius)

    Box(modifier = modifier.fillMaxSize()) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { expanded = false }
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = if (expanded) 0.dp else islandConfig.horizontalOffsetDp.dp,
                    y = islandConfig.topOffsetDp.dp
                )
                .width(animatedWidth)
                .height(animatedHeight)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bgBase.copy(alpha = 0.96f),
                            bgBase.copy(alpha = 0.90f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = glowAlpha),
                            Color.White.copy(alpha = glowAlpha * 0.55f),
                            accent.copy(alpha = glowAlpha * 0.75f)
                        )
                    ),
                    shape = shape
                )
                .clickable { expanded = !expanded }
                .testTag("vitro_dynamic_island")
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "di_state_content"
            ) { isExpanded ->
                if (!isExpanded) {
                    // Compact Idle / Live Activity Pill
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left slot: Notification badge / Media wave / Clock
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            when {
                                islandConfig.showNotifications && latestNotif != null -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = wavePulse))
                                    )
                                    Text(
                                        text = latestNotif.appName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = VitroTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                hasMedia -> {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = "Media",
                                        tint = accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = nowPlaying.title.ifBlank { "Audio" },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = VitroTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                hasFocus -> {
                                    val mins = overlays.focusSecondsRemaining / 60
                                    val secs = overlays.focusSecondsRemaining % 60
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = "Fokus",
                                        tint = accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "%02d:%02d".format(mins, secs),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = accent
                                    )
                                }
                                islandConfig.showClock -> {
                                    Text(
                                        text = telemetry.timeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = VitroTextPrimary
                                    )
                                }
                            }
                        }

                        // Right slot: Charging / Battery / Unread dot
                        if (islandConfig.showBattery) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (telemetry.isCharging) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = "Mengisi",
                                        tint = VitroMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "${telemetry.batteryPercent}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (telemetry.isCharging) VitroMint else VitroTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Expanded Interactive Dynamic Island Card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Live Status + Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = telemetry.timeText,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VitroTextPrimary
                                )
                                Text(
                                    text = "• ${telemetry.batteryPercent}%${if (telemetry.isCharging) " ⚡" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (telemetry.isCharging) VitroMint else accent
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (telemetry.isFlashlightOn) VitroAmber.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f))
                                        .clickable(onClick = onToggleFlashlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FlashlightOn,
                                        contentDescription = "Senter",
                                        tint = if (telemetry.isFlashlightOn) VitroAmber else VitroTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable {
                                            expanded = false
                                            onOpenQuickPanel()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ControlCamera,
                                        contentDescription = "Pusat Kontrol",
                                        tint = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Middle: Live Notification Banner (if any)
                        if (latestNotif != null && islandConfig.showNotifications) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .clickable {
                                        expanded = false
                                        onOpenNotification(latestNotif)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (latestNotif.appIcon != null) {
                                    Image(
                                        bitmap = latestNotif.appIcon,
                                        contentDescription = latestNotif.appName,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${latestNotif.title} • ${latestNotif.appName}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = VitroTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = latestNotif.text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VitroTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDismissNotification(latestNotif.key) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus notifikasi",
                                        tint = VitroTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Bottom: Real Media Controls Bar
                        if (islandConfig.showMedia) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = nowPlaying.title.ifBlank { "Kontrol Media HP" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VitroTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = nowPlaying.artist.ifBlank { "Putar / Jeda audio aktif" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VitroTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onMediaPrev, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = VitroTextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = onMediaPlayPause, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            imageVector = if (nowPlaying.isPlaying || overlays.isMediaPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Putar atau jeda",
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = onMediaNext, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = VitroTextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
