package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.LockClockColor
import com.example.launcher.model.LockClockStyle
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.WidgetType
import com.example.launcher.ui.components.AdaptiveLockClockHero
import com.example.launcher.ui.components.LockScreenDiyControlSheet
import com.example.launcher.ui.components.SingleBuiltInGlassWidgetCard
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.VitroWallpaperBackdrop
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.ui.components.bouncyClickable
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun LockScreenOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays = LauncherUiOverlays(),
    nowPlaying: NowPlayingInfo = NowPlayingInfo(),
    installedApps: List<InstalledApp> = emptyList(),
    notifications: List<LiveNotification>,
    isNotificationAccessEnabled: Boolean,
    onUnlock: () -> Unit,
    onVerifyPasscode: (String) -> Boolean,
    onToggleFlashlight: () -> Unit,
    onToggleMedia: () -> Unit = {},
    onTriggerLockPopup: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onOpenStudioFromLock: () -> Unit,
    onUpdateLockClockLayout: (Float, Float) -> Unit = { _, _ -> },
    onUpdateLockClockStyle: (LockClockStyle) -> Unit = {},
    onUpdateLockClockColor: (LockClockColor) -> Unit = {},
    onUpdateLockClockWeight: (Int) -> Unit = {},
    onAddLockWidget: (WidgetType) -> Unit = {},
    onUpdateLockWidgetLayout: (String, Float, Int, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onRemoveLockWidget: (String) -> Unit = {}
) {
    val requiresPasscode = config.lockSecurityMode != LockSecurityMode.NONE &&
        config.lockPasscodeHash.isNotBlank()

    var showPasscodeChallenge by remember(visible) { mutableStateOf(false) }
    var pendingNotificationToOpen by remember(visible) { mutableStateOf<LiveNotification?>(null) }
    var isLockDiyMode by remember(visible) { mutableStateOf(false) }

    fun requestUnlockOrChallenge(afterUnlockNotif: LiveNotification? = null) {
        if (isLockDiyMode) return
        if (requiresPasscode) {
            pendingNotificationToOpen = afterUnlockNotif
            showPasscodeChallenge = true
        } else {
            if (afterUnlockNotif != null) {
                // Open notification FIRST before unlocking/finishing Lock Screen Activity
                onOpenNotification(afterUnlockNotif)
            } else {
                onUnlock()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 5 },
        exit = fadeOut() + slideOutVertically { -it / 3 }
    ) {
        BackHandler {
            when {
                showPasscodeChallenge -> {
                    showPasscodeChallenge = false
                    pendingNotificationToOpen = null
                }
                isLockDiyMode -> isLockDiyMode = false
                !requiresPasscode -> onUnlock()
            }
        }

        val lockPagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { config.lockPageCount.coerceIn(1, 4) }
        )
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val unlockThresholdPx = with(density) { 108.dp.toPx() }
        val unlockDragOffsetY = remember { Animatable(0f) }

        fun finishSwipeUpGesture(flingVelocityY: Float = 0f) {
            scope.launch {
                val shouldUnlock = unlockDragOffsetY.value < -unlockThresholdPx || flingVelocityY < -950f
                if (shouldUnlock) {
                    if (!requiresPasscode) {
                        unlockDragOffsetY.animateTo(-unlockThresholdPx * 3.5f, tween(160))
                        unlockDragOffsetY.snapTo(0f)
                        requestUnlockOrChallenge()
                    } else {
                        unlockDragOffsetY.animateTo(
                            0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 460f)
                        )
                        requestUnlockOrChallenge()
                    }
                } else {
                    unlockDragOffsetY.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 460f)
                    )
                }
            }
        }

        val swipeUpNestedScroll = remember(showPasscodeChallenge, isLockDiyMode, requiresPasscode) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (showPasscodeChallenge || isLockDiyMode) return Offset.Zero
                    // When user overscrolls upward at the bottom of the lock screen list, lift the lock screen like iPhone
                    if (available.y < 0f || unlockDragOffsetY.value < 0f) {
                        val next = (unlockDragOffsetY.value + available.y).coerceIn(-unlockThresholdPx * 2.4f, 0f)
                        scope.launch { unlockDragOffsetY.snapTo(next) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (unlockDragOffsetY.value < -12f) {
                        finishSwipeUpGesture(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        val topSafe = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 6.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("lock_screen_overlay")
        ) {
            // Interactive iPhone-style sliding Lock Screen surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val y = unlockDragOffsetY.value
                        translationY = y
                        alpha = (1f + y / (unlockThresholdPx * 5.2f)).coerceIn(0.32f, 1f)
                    }
            ) {
                VitroWallpaperBackdrop(
                    wallpaperConfig = config.effectiveLockWallpaper(),
                    glowIntensity = config.ambientGlowIntensity
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(topSafe)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (config.dynamicIsland.enabled && config.dynamicIsland.showOnLockScreen) {
                        Spacer(modifier = Modifier.height((config.dynamicIsland.heightDp + config.dynamicIsland.topOffsetDp + 4f).dp))
                    }

                    // Top status row + subtle Lock DIY trigger button (avoid duplicate pill right under Dynamic Island)
                    val showLockStatusArea = !(config.dynamicIsland.enabled &&
                        config.dynamicIsland.showOnLockScreen &&
                        config.statusPosition == StatusIndicatorPosition.DYNAMIC_PILL)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showLockStatusArea) {
                            TopStatusIndicatorArea(
                                config = config,
                                telemetry = telemetry,
                                onOpenQuickPanel = {},
                                onTriggerPopup = onTriggerLockPopup
                            )
                        } else {
                            Spacer(modifier = Modifier.height(34.dp))
                        }
                        IconButton(
                            onClick = { isLockDiyMode = !isLockDiyMode },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isLockDiyMode) VitroCyanAccent else Color.White.copy(alpha = 0.14f))
                                .testTag("lock_diy_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Mode DIY Layar Kunci",
                                tint = if (isLockDiyMode) Color(0xFF040814) else VitroTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Multi-page Lock Screen Pager with overscroll-up unlock support
                    HorizontalPager(
                        state = lockPagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(swipeUpNestedScroll)
                    ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(config.lockPageScale)
                            .applyPageTransition(
                                pagerState = lockPagerState,
                                page = page,
                                style = config.pageTransitionStyle
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (page == 0) {
                            // Adaptive Clock (long-press or drag handle to freely resize directly on Lock Screen!)
                            AdaptiveLockClockHero(
                                config = config,
                                telemetry = telemetry,
                                notificationCount = notifications.size,
                                isDiyMode = isLockDiyMode,
                                onEnterDiy = { isLockDiyMode = true },
                                onUpdateClockLayout = onUpdateLockClockLayout
                            )

                            // DIY Resizable & Draggable Lock Screen Glass Widgets
                            if (config.lockWidgets.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    config.lockWidgets.forEach { placement ->
                                        LockScreenDiyWidgetItem(
                                            placement = placement,
                                            isDiyMode = isLockDiyMode,
                                            config = config,
                                            telemetry = telemetry,
                                            overlays = overlays,
                                            nowPlaying = nowPlaying,
                                            installedApps = installedApps,
                                            notifications = notifications,
                                            isNotificationAccessEnabled = isNotificationAccessEnabled,
                                            onEnterDiy = { isLockDiyMode = true },
                                            onUpdateLayout = { w, h, x, y ->
                                                onUpdateLockWidgetLayout(placement.id, w, h, x, y)
                                            },
                                            onRemove = { onRemoveLockWidget(placement.id) },
                                            onToggleMedia = onToggleMedia,
                                            onOpenNotification = { requestUnlockOrChallenge(it) },
                                            onDismissNotification = onDismissNotification,
                                            onEnableNotificationAccess = onEnableNotificationAccess,
                                            onOpenStudio = onOpenStudioFromLock
                                        )
                                    }
                                }
                            }

                            // Live Phone Notifications Deck on Lock Screen
                            LockNotificationsDeck(
                                notifications = notifications,
                                isNotificationAccessEnabled = isNotificationAccessEnabled,
                                glassOpacity = config.popupGlassOpacity,
                                onOpenNotification = { notif -> requestUnlockOrChallenge(notif) },
                                onDismissNotification = onDismissNotification,
                                onClearAll = onClearAllNotifications,
                                onEnableNotificationAccess = onEnableNotificationAccess,
                                onTriggerPreview = onTriggerLockPopup
                            )
                        } else {
                            // Secondary Lock Screen pages: extra focus / widgets / quick preview
                            AdaptiveLockClockHero(
                                config = config,
                                telemetry = telemetry,
                                notificationCount = notifications.size,
                                isDiyMode = isLockDiyMode,
                                onEnterDiy = { isLockDiyMode = true },
                                onUpdateClockLayout = onUpdateLockClockLayout
                            )
                            VitroGlassContainer(
                                cornerRadius = 24.dp,
                                glassOpacity = config.widgetGlassOpacity,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Halaman Layar Kunci ${page + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VitroCyanAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tahan lama jam atau ketuk ikon pensil di kanan atas untuk mengatur Adaptive Clock dan menambah widget kaca langsung di layar kunci.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VitroTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                    // Bottom Area: either Lock DIY Editor Sheet OR iPhone-Style Swipe-Up Unlock Bar
                    if (isLockDiyMode) {
                        LockScreenDiyControlSheet(
                            config = config,
                            onUpdateClockStyle = onUpdateLockClockStyle,
                            onUpdateClockColor = onUpdateLockClockColor,
                            onUpdateClockScale = { s -> onUpdateLockClockLayout(s, config.lockClockVerticalOffsetDp) },
                            onUpdateClockWeight = onUpdateLockClockWeight,
                            onAddLockWidget = onAddLockWidget,
                            onDone = { isLockDiyMode = false }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(showPasscodeChallenge, isLockDiyMode, requiresPasscode) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { finishSwipeUpGesture() },
                                        onDragCancel = { finishSwipeUpGesture() },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            val next = (unlockDragOffsetY.value + dragAmount)
                                                .coerceIn(-unlockThresholdPx * 2.8f, 0f)
                                            scope.launch { unlockDragOffsetY.snapTo(next) }
                                        }
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (telemetry.isFlashlightOn) VitroAmber.copy(alpha = 0.85f)
                                            else Color.White.copy(alpha = 0.16f)
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable(onClick = onToggleFlashlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.FlashlightOn,
                                        contentDescription = "Senter",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable(onClick = onTriggerLockPopup),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.NotificationsActive,
                                        contentDescription = "Notifikasi",
                                        tint = VitroCyanAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Dedicated iPhone Home Indicator Swipe-Up Zone
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 10.dp)
                                    .testTag("iphone_swipe_up_unlock_zone"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (requiresPasscode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (requiresPasscode) {
                                            "Geser ke atas untuk memasukkan sandi"
                                        } else {
                                            "Geser ke atas untuk membuka"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.84f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Iconic iPhone glowing home pill indicator
                                Box(
                                    modifier = Modifier
                                        .width(138.dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.88f))
                                )
                            }
                        }
                    }
                }
            }

            // Frosted Glass Passcode / PIN Challenge Modal
            AnimatedVisibility(
                visible = showPasscodeChallenge,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LockPasscodeChallengeOverlay(
                    securityMode = config.lockSecurityMode,
                    expectedLength = config.lockPasscodeLength,
                    onVerify = { input ->
                        if (onVerifyPasscode(input)) {
                            val targetNotif = pendingNotificationToOpen
                            showPasscodeChallenge = false
                            pendingNotificationToOpen = null
                            if (targetNotif != null) {
                                onOpenNotification(targetNotif)
                            } else {
                                onUnlock()
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onCancel = {
                        showPasscodeChallenge = false
                        pendingNotificationToOpen = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockScreenDiyWidgetItem(
    placement: HomeWidgetPlacement,
    isDiyMode: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    notifications: List<LiveNotification>,
    isNotificationAccessEnabled: Boolean,
    onEnterDiy: () -> Unit,
    onUpdateLayout: (Float, Int, Float, Float) -> Unit,
    onRemove: () -> Unit,
    onToggleMedia: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onOpenStudio: () -> Unit
) {
    val density = LocalDensity.current
    var widthPct by remember(placement.id, placement.widthPercent) { mutableFloatStateOf(placement.widthPercent) }
    var heightDp by remember(placement.id, placement.heightDp) { mutableFloatStateOf(placement.heightDp.toFloat()) }
    var xOff by remember(placement.id, placement.xOffsetDp) { mutableFloatStateOf(placement.xOffsetDp) }
    var yOff by remember(placement.id, placement.yOffsetDp) { mutableFloatStateOf(placement.yOffsetDp) }

    val w = (if (isDiyMode) widthPct else placement.widthPercent).coerceIn(0.44f, 0.99f)
    val h = (if (isDiyMode) heightDp.roundToInt() else placement.heightDp).coerceIn(80, 320)

    Box(
        modifier = Modifier
            .fillMaxWidth(w)
            .offset {
                IntOffset(
                    with(density) { xOff.dp.roundToPx() },
                    with(density) { yOff.dp.roundToPx() }
                )
            }
            .then(
                if (isDiyMode) {
                    Modifier
                        .border(1.4.dp, VitroCyanAccent, RoundedCornerShape(24.dp))
                        .pointerInput(placement.id) {
                            detectDragGestures(
                                onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    xOff = (xOff + with(density) { drag.x.toDp().value }).coerceIn(-100f, 100f)
                                    yOff = (yOff + with(density) { drag.y.toDp().value }).coerceIn(-80f, 160f)
                                }
                            )
                        }
                } else {
                    Modifier.combinedClickable(onClick = {}, onLongClick = onEnterDiy)
                }
            )
    ) {
        placement.builtInType?.let { type ->
            SingleBuiltInGlassWidgetCard(
                type = type,
                config = config,
                telemetry = telemetry,
                overlays = overlays,
                notifications = notifications,
                nowPlaying = nowPlaying,
                installedApps = installedApps,
                isNotificationAccessEnabled = isNotificationAccessEnabled,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = {},
                onResetFocusTimer = {},
                onOpenNotification = onOpenNotification,
                onDismissNotification = onDismissNotification,
                onEnableNotificationAccess = onEnableNotificationAccess,
                onLaunchApp = {},
                onOpenStudio = onOpenStudio,
                modifier = Modifier.height(h.dp)
            )
        }

        if (isDiyMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(VitroCoral)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(VitroCyanAccent)
                    .pointerInput(placement.id) {
                        detectDragGestures(
                            onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                            onDrag = { change, drag ->
                                change.consume()
                                widthPct = (widthPct + with(density) { drag.x.toDp().value } / 320f).coerceIn(0.44f, 1.0f)
                                heightDp = (heightDp + with(density) { drag.y.toDp().value }).coerceIn(80f, 320f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.OpenWith, contentDescription = "Ubah Ukuran", tint = Color(0xFF040814), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun LockNotificationsDeck(
    notifications: List<LiveNotification>,
    isNotificationAccessEnabled: Boolean,
    glassOpacity: Float,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onTriggerPreview: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isNotificationAccessEnabled) {
            VitroGlassContainer(
                cornerRadius = 22.dp,
                glassOpacity = glassOpacity,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEnableNotificationAccess)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktifkan Notifikasi Asli HP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "Ketuk di sini agar pesan WhatsApp, SMS & aplikasi lain muncul langsung di Layar Kunci & Dynamic Island.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            }
        } else if (notifications.isEmpty()) {
            VitroGlassContainer(
                cornerRadius = 22.dp,
                glassOpacity = glassOpacity * 0.8f,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTriggerPreview)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tidak ada notifikasi baru",
                        style = MaterialTheme.typography.bodySmall,
                        color = VitroTextSecondary
                    )
                    Text(
                        text = "Pratinjau",
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroCyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifikasi (${notifications.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary
                )
                TextButton(onClick = onClearAll) {
                    Text("Bersihkan", style = MaterialTheme.typography.labelSmall, color = VitroCyanAccent)
                }
            }
            notifications.take(6).forEach { notif ->
                VitroGlassContainer(
                    cornerRadius = 22.dp,
                    glassOpacity = glassOpacity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNotification(notif) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (notif.appIcon != null) {
                            Image(
                                bitmap = notif.appIcon,
                                contentDescription = notif.appName,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${notif.appName} • ${notif.title}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = VitroTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = notif.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = VitroTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!notif.isOngoing) {
                            IconButton(
                                onClick = { onDismissNotification(notif.key) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = VitroTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockPasscodeChallengeOverlay(
    securityMode: LockSecurityMode,
    expectedLength: Int,
    onVerify: (String) -> Boolean,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerError() {
        errorMsg = "Sandi salah, coba lagi"
        input = ""
        scope.launch {
            listOf(-18f, 18f, -12f, 12f, 0f).forEach { target ->
                shakeOffset.animateTo(target, tween(45))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VitroObsidian.copy(alpha = 0.88f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(34.dp))
            Text(
                text = if (securityMode == LockSecurityMode.PIN) "Masukkan PIN Layar Kunci" else "Masukkan Kata Sandi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary
            )
            if (errorMsg != null) {
                Text(errorMsg!!, style = MaterialTheme.typography.labelMedium, color = VitroCoral)
            }

            if (securityMode == LockSecurityMode.PIN) {
                val pinLen = expectedLength.coerceIn(4, 6)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(pinLen) { i ->
                        val filled = i < input.length
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(if (filled) VitroCyanAccent else Color.Transparent)
                                .border(1.5.dp, if (filled) VitroCyanAccent else Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                    }
                }

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Batal", "0", "DEL")
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.13f))
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                        .clickable {
                                            errorMsg = null
                                            when (key) {
                                                "Batal" -> onCancel()
                                                "DEL" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                                else -> {
                                                    if (input.length < pinLen) {
                                                        val next = input + key
                                                        input = next
                                                        if (next.length == pinLen) {
                                                            if (!onVerify(next)) triggerError()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "DEL" -> Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Hapus", tint = VitroTextPrimary)
                                        "Batal" -> Text("Batal", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                                        else -> Text(key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = VitroTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorMsg = null
                    },
                    singleLine = true,
                    placeholder = { Text("Kata sandi Anda", color = VitroTextSecondary) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Tampilkan",
                                tint = VitroTextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VitroTextPrimary,
                        unfocusedTextColor = VitroTextPrimary,
                        focusedBorderColor = VitroCyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Batal", color = VitroTextSecondary)
                    }
                    Button(
                        onClick = { if (!onVerify(input)) triggerError() },
                        colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buka", color = Color(0xFF040814), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
