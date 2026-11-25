package com.example.launcher.ui.components

import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.delay

private enum class IslandVisualState {
    COMPACT,
    AUTO_NOTIF,
    AUTO_SYSTEM_STATUS,
    EXPANDED_FULL
}

private data class SystemStatusBanner(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val accent: Color
)

/**
 * iPhone 18 Pro Max Liquid Glass Dynamic Island.
 * Automatically expands & collapses with spring physics when notifications arrive or system status
 * changes (charging, flashlight, ringer mode), displays an animated aurora music visualizer,
 * and supports a detached dual-activity split bubble.
 */
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

    var visualState by remember { mutableStateOf(IslandVisualState.COMPACT) }
    var activeStatusBanner by remember { mutableStateOf<SystemStatusBanner?>(null) }
    var lastHandledNotifKey by remember { mutableStateOf<String?>(null) }
    var previousCharging by remember { mutableStateOf(telemetry.isCharging) }
    var previousFlashlight by remember { mutableStateOf(telemetry.isFlashlightOn) }
    var previousRinger by remember { mutableStateOf(telemetry.ringerMode) }

    val latestNotif = notifications.firstOrNull()
    val hasMedia = islandConfig.showMedia && (nowPlaying.hasActiveSession || overlays.isMediaPlaying)
    val hasFocus = islandConfig.showFocusTimer && overlays.isFocusRunning
    val hasSecondaryBubble = islandConfig.showDualSplitBubble &&
        visualState == IslandVisualState.COMPACT &&
        ((hasMedia && latestNotif != null) ||
            (hasFocus && (hasMedia || latestNotif != null)) ||
            (telemetry.isFlashlightOn && (hasMedia || latestNotif != null)))

    // Auto-expand when a brand-new real notification arrives
    LaunchedEffect(latestNotif?.key, latestNotif?.timestampMillis, islandConfig.autoExpandOnNotification) {
        if (islandConfig.autoExpandOnNotification &&
            islandConfig.showNotifications &&
            latestNotif != null &&
            latestNotif.key != lastHandledNotifKey
        ) {
            lastHandledNotifKey = latestNotif.key
            if (visualState != IslandVisualState.EXPANDED_FULL) {
                visualState = IslandVisualState.AUTO_NOTIF
                delay(5_200L)
                if (visualState == IslandVisualState.AUTO_NOTIF) {
                    visualState = IslandVisualState.COMPACT
                }
            }
        }
    }

    // Auto-expand on live hardware/system status transitions (charging, torch, ringer)
    LaunchedEffect(telemetry.isCharging, telemetry.isFlashlightOn, telemetry.ringerMode, islandConfig.autoExpandOnSystemStatus) {
        if (!islandConfig.autoExpandOnSystemStatus) {
            previousCharging = telemetry.isCharging
            previousFlashlight = telemetry.isFlashlightOn
            previousRinger = telemetry.ringerMode
            return@LaunchedEffect
        }
        val banner = when {
            telemetry.isCharging != previousCharging -> SystemStatusBanner(
                title = if (telemetry.isCharging) "Mengisi Daya" else "Pengisian Dilepas",
                detail = "${telemetry.batteryPercent}% • ${String.format("%.1f", telemetry.batteryTempC)}°C",
                icon = Icons.Rounded.Bolt,
                accent = if (telemetry.isCharging) VitroMint else VitroAmber
            )
            telemetry.isFlashlightOn != previousFlashlight -> SystemStatusBanner(
                title = if (telemetry.isFlashlightOn) "Senter Aktif" else "Senter Dimatikan",
                detail = "Ketuk pulau untuk kontrol cepat",
                icon = Icons.Rounded.FlashlightOn,
                accent = VitroAmber
            )
            telemetry.ringerMode != previousRinger -> when (telemetry.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> SystemStatusBanner(
                    title = "Mode Hening",
                    detail = "Notifikasi disenyapkan",
                    icon = Icons.Rounded.NotificationsOff,
                    accent = VitroCoral
                )
                AudioManager.RINGER_MODE_VIBRATE -> SystemStatusBanner(
                    title = "Mode Getar",
                    detail = "Getaran aktif",
                    icon = Icons.Rounded.Vibration,
                    accent = VitroIndigo
                )
                else -> SystemStatusBanner(
                    title = "Mode Dering",
                    detail = "Suara aktif",
                    icon = Icons.Rounded.VolumeUp,
                    accent = VitroCyanAccent
                )
            }
            else -> null
        }
        previousCharging = telemetry.isCharging
        previousFlashlight = telemetry.isFlashlightOn
        previousRinger = telemetry.ringerMode

        if (banner != null && visualState != IslandVisualState.EXPANDED_FULL) {
            activeStatusBanner = banner
            visualState = IslandVisualState.AUTO_SYSTEM_STATUS
            delay(2_800L)
            if (visualState == IslandVisualState.AUTO_SYSTEM_STATUS) {
                visualState = IslandVisualState.COMPACT
            }
        }
    }

    val compactBaseW = islandConfig.widthDp.coerceIn(116f, 280f)
    val compactDynamicW = when {
        hasMedia -> (compactBaseW + 46f).coerceAtMost(268f)
        latestNotif != null && islandConfig.showNotifications -> (compactBaseW + 28f).coerceAtMost(254f)
        telemetry.isCharging -> (compactBaseW + 18f).coerceAtMost(246f)
        else -> compactBaseW
    }

    val targetWidth = when (visualState) {
        IslandVisualState.COMPACT -> compactDynamicW.dp
        IslandVisualState.AUTO_SYSTEM_STATUS -> 286.dp
        IslandVisualState.AUTO_NOTIF -> 346.dp
        IslandVisualState.EXPANDED_FULL -> 356.dp
    }

    val targetHeight = when (visualState) {
        IslandVisualState.COMPACT -> islandConfig.heightDp.coerceIn(28f, 54f).dp
        IslandVisualState.AUTO_SYSTEM_STATUS -> 66.dp
        IslandVisualState.AUTO_NOTIF -> 108.dp
        IslandVisualState.EXPANDED_FULL -> {
            var h = 154
            if (hasMedia) h += 58
            if (latestNotif != null && islandConfig.showNotifications) h += 64
            h.coerceAtMost(276).dp
        }
    }

    val targetRadius = when (visualState) {
        IslandVisualState.COMPACT -> islandConfig.cornerRadiusDp.coerceIn(14f, 32f).dp
        IslandVisualState.AUTO_SYSTEM_STATUS -> 32.dp
        IslandVisualState.AUTO_NOTIF -> 32.dp
        IslandVisualState.EXPANDED_FULL -> 36.dp
    }

    val morphSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 380f
    )
    val animatedW by animateDpAsState(targetWidth, morphSpring, label = "di_w")
    val animatedH by animateDpAsState(targetHeight, morphSpring, label = "di_h")
    val animatedR by animateDpAsState(targetRadius, morphSpring, label = "di_r")

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldAnimateAura = isResumed &&
        !telemetry.isPowerSaveMode &&
        (visualState != IslandVisualState.COMPACT || hasMedia || telemetry.isCharging)

    val shimmerPhase: Float
    val glowPulse: Float
    if (shouldAnimateAura) {
        val infinite = rememberInfiniteTransition(label = "di_aura")
        val s by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
            label = "di_shimmer"
        )
        val g by infinite.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "di_glow"
        )
        shimmerPhase = s
        glowPulse = g
    } else {
        shimmerPhase = 0.45f
        glowPulse = 0.84f
    }

    val currentOnOpenQuickPanel by rememberUpdatedState(onOpenQuickPanel)

    val style = islandConfig.style
    val rimAlpha = (islandConfig.borderGlow * glowPulse).coerceIn(0.15f, 0.96f)
    val shape = RoundedCornerShape(animatedR)

    val liquidRimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = rimAlpha * 0.65f),
            style.accentColor.copy(alpha = rimAlpha),
            VitroIndigo.copy(alpha = rimAlpha * (0.5f + 0.5f * shimmerPhase)),
            Color(0xFFEC4899).copy(alpha = rimAlpha * (0.4f + 0.5f * (1f - shimmerPhase)))
        )
    )

    val containerBgBrush = Brush.verticalGradient(
        colors = listOf(
            style.containerColor.copy(alpha = 0.98f),
            Color(0xFF040711).copy(alpha = 0.96f)
        )
    )

    Row(
        modifier = modifier
            .offset(
                x = islandConfig.horizontalOffsetDp.dp,
                y = islandConfig.topOffsetDp.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val outerGestureModifier = if (
            visualState == IslandVisualState.COMPACT ||
            visualState == IslandVisualState.AUTO_SYSTEM_STATUS
        ) {
            Modifier.pointerInput(visualState) {
                detectTapGestures(
                    onTap = { visualState = IslandVisualState.EXPANDED_FULL },
                    onLongPress = { currentOnOpenQuickPanel() }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .width(animatedW)
                .height(animatedH)
                .clip(shape)
                .background(containerBgBrush)
                .border(
                    width = if (visualState != IslandVisualState.COMPACT) 1.5.dp else 1.dp,
                    brush = liquidRimBrush,
                    shape = shape
                )
                .then(outerGestureModifier)
                .testTag("vitro_dynamic_island")
        ) {
            // Top liquid glass specular sheen line (iPhone 18 Pro Max feel)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.62f)
                    .height(1.2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.34f * rimAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            AnimatedContent(
                targetState = visualState,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.94f))
                },
                label = "di_content"
            ) { state ->
                when (state) {
                    IslandVisualState.COMPACT -> CompactProMaxContent(
                        islandConfig = islandConfig,
                        telemetry = telemetry,
                        overlays = overlays,
                        latestNotif = latestNotif,
                        nowPlaying = nowPlaying,
                        hasMedia = hasMedia,
                        hasFocus = hasFocus,
                        accentColor = style.accentColor
                    )

                    IslandVisualState.AUTO_SYSTEM_STATUS -> AutoSystemStatusContent(
                        banner = activeStatusBanner ?: SystemStatusBanner(
                            title = "Vitro Status",
                            detail = "${telemetry.batteryPercent}% • ${telemetry.timeText}",
                            icon = Icons.Rounded.Bolt,
                            accent = VitroMint
                        ),
                        telemetry = telemetry
                    )

                    IslandVisualState.AUTO_NOTIF -> AutoNotificationExpandContent(
                        notification = latestNotif,
                        accentColor = style.accentColor,
                        onOpen = { n ->
                            visualState = IslandVisualState.COMPACT
                            onOpenNotification(n)
                        },
                        onDismiss = { key ->
                            visualState = IslandVisualState.COMPACT
                            onDismissNotification(key)
                        }
                    )

                    IslandVisualState.EXPANDED_FULL -> ExpandedProMaxContent(
                        islandConfig = islandConfig,
                        telemetry = telemetry,
                        overlays = overlays,
                        latestNotif = latestNotif,
                        nowPlaying = nowPlaying,
                        hasMedia = hasMedia,
                        accentColor = style.accentColor,
                        onCollapse = { visualState = IslandVisualState.COMPACT },
                        onMediaPlayPause = onMediaPlayPause,
                        onMediaNext = onMediaNext,
                        onMediaPrev = onMediaPrev,
                        onToggleFlashlight = onToggleFlashlight,
                        onOpenQuickPanel = {
                            visualState = IslandVisualState.COMPACT
                            onOpenQuickPanel()
                        },
                        onOpenNotification = { n ->
                            visualState = IslandVisualState.COMPACT
                            onOpenNotification(n)
                        },
                        onDismissNotification = onDismissNotification
                    )
                }
            }
        }

        // Detached iPhone 18 Pro Max Dual-Activity Split Bubble
        AnimatedVisibility(
            visible = hasSecondaryBubble,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut() + scaleOut()
        ) {
            val bubbleSize = islandConfig.heightDp.coerceIn(30f, 44f).dp
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(Color(0xF5040711))
                    .border(1.1.dp, liquidRimBrush, CircleShape)
                    .clickable { visualState = IslandVisualState.EXPANDED_FULL },
                contentAlignment = Alignment.Center
            ) {
                when {
                    telemetry.isFlashlightOn -> Icon(
                        Icons.Rounded.FlashlightOn,
                        contentDescription = "Senter",
                        tint = VitroAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    latestNotif?.appIcon != null -> Image(
                        bitmap = latestNotif.appIcon,
                        contentDescription = latestNotif.appName,
                        modifier = Modifier.size(18.dp).clip(CircleShape)
                    )
                    latestNotif != null -> Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = "Notifikasi",
                        tint = style.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    hasFocus -> Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Fokus",
                        tint = VitroMint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactProMaxContent(
    islandConfig: DynamicIslandConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    latestNotif: LiveNotification?,
    nowPlaying: NowPlayingInfo,
    hasMedia: Boolean,
    hasFocus: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left dynamic capsule slot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            when {
                hasMedia -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo, Color(0xFFEC4899)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = "Musik",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = nowPlaying.title.ifBlank { "Musik Aktif" },
                        color = VitroTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                islandConfig.showNotifications && latestNotif != null -> {
                    if (latestNotif.appIcon != null) {
                        Image(
                            bitmap = latestNotif.appIcon,
                            contentDescription = latestNotif.appName,
                            modifier = Modifier.size(18.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = latestNotif.title,
                        color = VitroTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                hasFocus -> {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = VitroMint,
                        modifier = Modifier.size(14.dp)
                    )
                    val mins = overlays.focusSecondsRemaining / 60
                    val secs = overlays.focusSecondsRemaining % 60
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        color = VitroMint,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                islandConfig.showClock -> {
                    Text(
                        text = telemetry.timeText,
                        color = VitroTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right live animated indicator slot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (hasMedia) {
                AuroraSpectrumVisualizer(isPlaying = nowPlaying.isPlaying || overlays.isMediaPlaying)
            } else {
                if (telemetry.isFlashlightOn) {
                    Icon(
                        Icons.Rounded.FlashlightOn,
                        contentDescription = "Senter",
                        tint = VitroAmber,
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (islandConfig.showBattery) {
                    if (telemetry.isCharging) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = "Mengisi",
                            tint = VitroMint,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        color = when {
                            telemetry.isCharging -> VitroMint
                            telemetry.batteryPercent <= 20 -> VitroCoral
                            else -> accentColor
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoSystemStatusContent(
    banner: SystemStatusBanner,
    telemetry: DeviceTelemetry
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(banner.accent.copy(alpha = 0.22f))
                .border(1.dp, banner.accent.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = banner.icon,
                contentDescription = banner.title,
                tint = banner.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = banner.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = banner.detail,
                style = MaterialTheme.typography.labelSmall,
                color = VitroTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${telemetry.batteryPercent}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = banner.accent
        )
    }
}

@Composable
private fun AutoNotificationExpandContent(
    notification: LiveNotification?,
    accentColor: Color,
    onOpen: (LiveNotification) -> Unit,
    onDismiss: (String) -> Unit
) {
    if (notification == null) return
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpen(notification) }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("dynamic_island_auto_notif_click"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notification.appIcon != null) {
                Image(
                    bitmap = notification.appIcon,
                    contentDescription = notification.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${notification.appName} • Baru Saja",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = VitroTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = { onDismiss(notification.key) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Tutup Notifikasi",
                tint = VitroTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExpandedProMaxContent(
    islandConfig: DynamicIslandConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    latestNotif: LiveNotification?,
    nowPlaying: NowPlayingInfo,
    hasMedia: Boolean,
    accentColor: Color,
    onCollapse: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(accentColor, VitroIndigo)))
                )
                Text(
                    text = "${telemetry.timeText} • ${telemetry.dateText}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (telemetry.isCharging) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = VitroMint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "${telemetry.batteryPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (telemetry.isCharging) VitroMint else accentColor
                )
                IconButton(onClick = onCollapse, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Kecilkan",
                        tint = VitroTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // iPhone 18 Pro Max Music Deck with Animated Spectrum
        if (hasMedia) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.16f),
                                VitroIndigo.copy(alpha = 0.18f),
                                Color(0xFFEC4899).copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo, Color(0xFFEC4899)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nowPlaying.title.ifBlank { "Pemutar Musik HP" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = VitroTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AuroraSpectrumVisualizer(
                            isPlaying = nowPlaying.isPlaying || overlays.isMediaPlaying,
                            barCount = 4
                        )
                        Text(
                            text = nowPlaying.artist.ifBlank { nowPlaying.appName.ifBlank { "Kontrol Langsung" } },
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onMediaPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = VitroTextPrimary, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onMediaPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (nowPlaying.isPlaying || overlays.isMediaPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Putar/Jeda",
                        tint = VitroTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onMediaNext, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = VitroTextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Latest Live Notification Preview inside Expanded Island
        if (islandConfig.showNotifications && latestNotif != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onOpenNotification(latestNotif) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (latestNotif.appIcon != null) {
                    Image(
                        bitmap = latestNotif.appIcon,
                        contentDescription = latestNotif.appName,
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                    )
                } else {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${latestNotif.appName} • ${latestNotif.title}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
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
                        Icons.Rounded.Close,
                        contentDescription = "Hapus",
                        tint = VitroTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Quick action pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandMiniActionPill(
                label = if (telemetry.isFlashlightOn) "Senter ON" else "Senter",
                icon = Icons.Rounded.FlashlightOn,
                active = telemetry.isFlashlightOn,
                accent = VitroAmber,
                onClick = onToggleFlashlight,
                modifier = Modifier.weight(1f)
            )
            IslandMiniActionPill(
                label = "Pusat Kontrol",
                icon = Icons.Rounded.ControlCamera,
                active = false,
                accent = accentColor,
                onClick = onOpenQuickPanel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AuroraSpectrumVisualizer(
    isPlaying: Boolean,
    barCount: Int = 5,
    maxHeightDp: Float = 15f
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val barColors = listOf(
        Color(0xFF38BDF8),
        Color(0xFF818CF8),
        Color(0xFFC084FC),
        Color(0xFFF472B6),
        Color(0xFF34D399)
    )

    if (isPlaying && isResumed) {
        val infinite = rememberInfiniteTransition(label = "spectrum")
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until barCount) {
                val animHeight by infinite.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 360 + i * 110,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$i"
                )
                val h = (maxHeightDp * animHeight).coerceAtLeast(3.5f)
                Box(
                    modifier = Modifier
                        .width(2.8.dp)
                        .height(h.dp)
                        .clip(CircleShape)
                        .background(barColors[i % barColors.size])
                )
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until barCount) {
                Box(
                    modifier = Modifier
                        .width(2.8.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(barColors[i % barColors.size].copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun IslandMiniActionPill(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f))
            .border(
                width = 0.8.dp,
                color = if (active) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else VitroTextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (active) accent else VitroTextPrimary,
            maxLines = 1
        )
    }
}
