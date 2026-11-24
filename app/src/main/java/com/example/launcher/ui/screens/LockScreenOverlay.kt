package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.VitroWallpaperBackdrop
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreenOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    notifications: List<LiveNotification>,
    isNotificationAccessEnabled: Boolean,
    onUnlock: () -> Unit,
    onVerifyPasscode: (String) -> Boolean,
    onToggleFlashlight: () -> Unit,
    onTriggerLockPopup: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onOpenStudioFromLock: () -> Unit
) {
    val requiresPasscode = config.lockSecurityMode != LockSecurityMode.NONE &&
        config.lockPasscodeHash.isNotBlank()

    var showPasscodeChallenge by remember(visible) { mutableStateOf(false) }
    var pendingNotificationToOpen by remember(visible) { mutableStateOf<LiveNotification?>(null) }

    fun requestUnlockOrChallenge(afterUnlockNotif: LiveNotification? = null) {
        if (requiresPasscode) {
            pendingNotificationToOpen = afterUnlockNotif
            showPasscodeChallenge = true
        } else {
            onUnlock()
            if (afterUnlockNotif != null) onOpenNotification(afterUnlockNotif)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 5 },
        exit = fadeOut() + slideOutVertically { -it / 3 }
    ) {
        BackHandler {
            if (showPasscodeChallenge) {
                showPasscodeChallenge = false
                pendingNotificationToOpen = null
            } else if (!requiresPasscode) {
                onUnlock()
            }
        }

        val lockPagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { config.lockPageCount.coerceIn(1, 4) }
        )
        var swipeAccum by remember { mutableFloatStateOf(0f) }

        val topSafe = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 6.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("lock_screen_overlay")
                .pointerInput(requiresPasscode) {
                    detectVerticalDragGestures(
                        onDragStart = { swipeAccum = 0f },
                        onVerticalDrag = { _, dragAmount -> swipeAccum += dragAmount },
                        onDragEnd = {
                            if (swipeAccum < -110f) requestUnlockOrChallenge()
                            swipeAccum = 0f
                        }
                    )
                }
        ) {
            VitroWallpaperBackdrop(preset = config.wallpaperPreset, dimAlpha = 0.28f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(topSafe)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopStatusIndicatorArea(
                    config = config,
                    telemetry = telemetry,
                    onOpenQuickPanel = onTriggerLockPopup,
                    onTriggerPopup = onTriggerLockPopup
                )

                HorizontalPager(
                    state = lockPagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("lock_screen_pager")
                ) { page ->
                    val horizontalMargin = ((1.16f - config.lockPageScale) * 48f).coerceIn(12f, 30f).dp
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .applyPageTransition(
                                pagerState = lockPagerState,
                                page = page,
                                style = config.pageTransitionStyle
                            )
                            .padding(horizontal = horizontalMargin, vertical = 10.dp)
                            .scale(config.lockPageScale.coerceIn(0.88f, 1.06f))
                    ) {
                        when (page) {
                            0 -> LockMainClockPage(
                                config = config,
                                telemetry = telemetry,
                                notifications = notifications,
                                isNotificationAccessEnabled = isNotificationAccessEnabled,
                                onOpenNotification = { notif -> requestUnlockOrChallenge(notif) },
                                onDismissNotification = onDismissNotification,
                                onClearAllNotifications = onClearAllNotifications,
                                onEnableNotificationAccess = onEnableNotificationAccess
                            )
                            1 -> LockQuickWidgetsPage(
                                config = config,
                                telemetry = telemetry
                            )
                            2 -> LockQuickControlsPage(
                                telemetry = telemetry,
                                onToggleFlashlight = onToggleFlashlight,
                                onTriggerLockPopup = onTriggerLockPopup,
                                onOpenStudioFromLock = {
                                    if (!requiresPasscode) onOpenStudioFromLock()
                                    else showPasscodeChallenge = true
                                }
                            )
                            else -> LockFocusNotePage(config = config, telemetry = telemetry)
                        }
                    }
                }

                if (config.lockPageCount > 1) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(config.lockPageCount.coerceIn(1, 4)) { idx ->
                            val selected = lockPagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .width(if (selected) 20.dp else 7.dp)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) VitroCyanAccent
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VitroGlassContainer(
                        cornerRadius = 50.dp,
                        tintColor = if (telemetry.isFlashlightOn) VitroAmber else Color.White,
                        glassOpacity = if (telemetry.isFlashlightOn) 0.45f else 0.22f,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("lock_flashlight_button")
                            .bouncyClickable(onClick = onToggleFlashlight)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashlightOn,
                            contentDescription = "Senter",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    VitroGlassContainer(
                        cornerRadius = 50.dp,
                        tintColor = Color.White,
                        glassOpacity = 0.24f,
                        modifier = Modifier
                            .testTag("lock_unlock_button")
                            .bouncyClickable(onClick = { requestUnlockOrChallenge() })
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (requiresPasscode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = "Buka kunci layar",
                                tint = VitroCyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (requiresPasscode) "Masukkan Sandi" else "Buka Layar",
                                style = MaterialTheme.typography.labelLarge,
                                color = VitroTextPrimary
                            )
                        }
                    }

                    VitroGlassContainer(
                        cornerRadius = 50.dp,
                        tintColor = Color.White,
                        glassOpacity = 0.22f,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("lock_test_popup_button")
                            .bouncyClickable(onClick = onTriggerLockPopup)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Notifikasi layar kunci",
                            tint = VitroCyanAccent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Text(
                    text = if (requiresPasscode) "Geser ke atas untuk memasukkan sandi" else stringResource(R.string.lock_screen_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary.copy(alpha = 0.8f)
                )
            }

            // Frosted Glass PIN / Password Challenge Gate
            AnimatedVisibility(
                visible = showPasscodeChallenge,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 }
            ) {
                LockPasscodeChallengeOverlay(
                    mode = config.lockSecurityMode,
                    pinLength = config.lockPasscodeLength.coerceIn(4, 6),
                    onVerify = onVerifyPasscode,
                    onSuccess = {
                        showPasscodeChallenge = false
                        val targetNotif = pendingNotificationToOpen
                        pendingNotificationToOpen = null
                        onUnlock()
                        if (targetNotif != null) onOpenNotification(targetNotif)
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

@Composable
private fun LockPasscodeChallengeOverlay(
    mode: LockSecurityMode,
    pinLength: Int,
    onVerify: (String) -> Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPasswordText by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerError() {
        errorMsg = "Sandi salah, coba lagi"
        input = ""
        scope.launch {
            listOf(-22f, 22f, -16f, 16f, -8f, 8f, 0f).forEach { target ->
                shakeOffset.animateTo(target, tween(45))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE8050916))
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(VitroCyanAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = VitroCyanAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = if (mode == LockSecurityMode.PIN) "Masukkan PIN ($pinLength Digit)" else "Masukkan Sandi Layar Kunci",
                style = MaterialTheme.typography.titleLarge,
                color = VitroTextPrimary
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = VitroCoral
                )
            }

            if (mode == LockSecurityMode.PIN) {
                // Animated Glass PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pinLength) { idx ->
                        val filled = idx < input.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) VitroCyanAccent else Color.Transparent)
                                .border(1.5.dp, if (filled) VitroCyanAccent else Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // iOS Frosted Glass Numeric Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            row.forEach { digit ->
                                GlassPinKey(
                                    label = digit,
                                    onClick = {
                                        errorMsg = null
                                        if (input.length < pinLength) {
                                            val next = input + digit
                                            input = next
                                            if (next.length == pinLength) {
                                                if (onVerify(next)) onSuccess() else triggerError()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onCancel),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Batal", style = MaterialTheme.typography.labelLarge, color = VitroTextSecondary)
                        }
                        GlassPinKey(
                            label = "0",
                            onClick = {
                                errorMsg = null
                                if (input.length < pinLength) {
                                    val next = input + "0"
                                    input = next
                                    if (next.length == pinLength) {
                                        if (onVerify(next)) onSuccess() else triggerError()
                                    }
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (input.isNotEmpty()) input = input.dropLast(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Hapus digit",
                                tint = VitroTextPrimary
                            )
                        }
                    }
                }
            } else {
                // Alphanumeric Password Mode
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        errorMsg = null
                        input = it
                    },
                    singleLine = true,
                    placeholder = { Text("Ketik kata sandi…", color = VitroTextSecondary) },
                    visualTransformation = if (showPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPasswordText = !showPasswordText }) {
                            Icon(
                                imageVector = if (showPasswordText) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Tampilkan sandi",
                                tint = VitroTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VitroCyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                        focusedTextColor = VitroTextPrimary,
                        unfocusedTextColor = VitroTextPrimary
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
                        onClick = {
                            if (onVerify(input)) onSuccess() else triggerError()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Buka Kunci", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPinKey(
    label: String,
    onClick: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 50.dp,
        glassOpacity = 0.22f,
        modifier = Modifier
            .size(74.dp)
            .testTag("lock_pin_key_$label")
            .bouncyClickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
            color = VitroTextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun LockMainClockPage(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    notifications: List<LiveNotification>,
    isNotificationAccessEnabled: Boolean,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onEnableNotificationAccess: () -> Unit
) {
    val clockFontSize = (72f * config.lockClockScale).coerceIn(50f, 96f).sp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = telemetry.dateText,
                style = MaterialTheme.typography.titleMedium,
                color = VitroTextPrimary.copy(alpha = 0.92f)
            )
            Text(
                text = telemetry.timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = clockFontSize,
                    lineHeight = clockFontSize * 1.05f,
                    fontWeight = FontWeight.Light
                ),
                color = VitroTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VitroGlassContainer(cornerRadius = 50.dp, glassOpacity = 0.22f) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbTwilight,
                            contentDescription = null,
                            tint = VitroAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = telemetry.networkLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroTextPrimary
                        )
                    }
                }
                VitroGlassContainer(cornerRadius = 50.dp, glassOpacity = 0.22f) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull
                            else Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = VitroMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroTextPrimary
                        )
                    }
                }
            }
        }

        // Real Live Phone Notifications on Lock Screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(top = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isNotificationAccessEnabled) {
                VitroGlassContainer(
                    cornerRadius = config.popupCornerRadius.dp,
                    glassOpacity = 0.26f,
                    tintColor = VitroCyanAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(onClick = onEnableNotificationAccess)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Aktifkan Notifikasi HP Nyata",
                            style = MaterialTheme.typography.titleSmall,
                            color = VitroCyanAccent
                        )
                        Text(
                            text = "Ketuk di sini untuk memberi izin Akses Notifikasi agar pesan WhatsApp, SMS & aplikasi tampil langsung di Layar Kunci dan Beranda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            } else if (notifications.isEmpty()) {
                VitroGlassContainer(
                    cornerRadius = config.popupCornerRadius.dp,
                    glassOpacity = 0.20f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Notifikasi aktif (${config.lockPopupStyle.label}) • Belum ada pesan baru",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitroTextSecondary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifikasi (${notifications.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = VitroTextPrimary
                    )
                    Text(
                        text = "Bersihkan semua",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroCyanAccent,
                        modifier = Modifier.clickable(onClick = onClearAllNotifications)
                    )
                }

                notifications.take(5).forEach { notif ->
                    VitroGlassContainer(
                        cornerRadius = config.popupCornerRadius.dp,
                        glassOpacity = config.popupGlassOpacity.coerceIn(0.20f, 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNotification(notif) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (notif.appIcon != null) {
                                Image(
                                    bitmap = notif.appIcon,
                                    contentDescription = notif.appName,
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(VitroCyanAccent.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${notif.title} • ${notif.appName}",
                                    style = MaterialTheme.typography.labelLarge,
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
                            IconButton(
                                onClick = { onDismissNotification(notif.key) },
                                modifier = Modifier.size(32.dp)
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
private fun LockQuickWidgetsPage(
    config: LauncherConfig,
    telemetry: DeviceTelemetry
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ringkasan Perangkat",
            style = MaterialTheme.typography.headlineMedium,
            color = VitroTextPrimary
        )
        VitroGlassContainer(cornerRadius = 26.dp, glassOpacity = config.widgetGlassOpacity) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Baterai & Suhu", style = MaterialTheme.typography.labelLarge, color = VitroMint)
                Text(
                    text = "${telemetry.batteryPercent}% • ${"%.1f".format(telemetry.batteryTempC)}°C",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VitroTextPrimary
                )
                Text(
                    text = if (telemetry.isCharging) "Pengisian daya aktif" else "Mode hemat & stabil",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VitroTextSecondary
                )
            }
        }
        VitroGlassContainer(cornerRadius = 26.dp, glassOpacity = config.widgetGlassOpacity) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("RAM & Penyimpanan", style = MaterialTheme.typography.labelLarge, color = VitroCyanAccent)
                Text(
                    text = "RAM ${"%.1f".format(telemetry.ramUsedGb)}/${"%.1f".format(telemetry.ramTotalGb)} GB",
                    style = MaterialTheme.typography.titleLarge,
                    color = VitroTextPrimary
                )
                Text(
                    text = "Memori ${"%.0f".format(telemetry.storageUsedGb)}/${"%.0f".format(telemetry.storageTotalGb)} GB terpakai",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VitroTextSecondary
                )
            }
        }
    }
}

@Composable
private fun LockQuickControlsPage(
    telemetry: DeviceTelemetry,
    onToggleFlashlight: () -> Unit,
    onTriggerLockPopup: () -> Unit,
    onOpenStudioFromLock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pintasan Layar Kunci",
            style = MaterialTheme.typography.headlineMedium,
            color = VitroTextPrimary
        )
        VitroGlassContainer(
            cornerRadius = 24.dp,
            glassOpacity = 0.24f,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(onClick = onToggleFlashlight)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = VitroAmber)
                Column {
                    Text("Senter Cepat", style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
                    Text(
                        if (telemetry.isFlashlightOn) "Menyala • Ketuk matikan" else "Mati • Ketuk nyalakan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VitroTextSecondary
                    )
                }
            }
        }
        VitroGlassContainer(
            cornerRadius = 24.dp,
            glassOpacity = 0.24f,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(onClick = onTriggerLockPopup)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VitroCyanAccent)
                Column {
                    Text("Uji Pop-Up Layar Kunci", style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
                    Text("Tampilkan pratinjau notifikasi bergaya kaca", style = MaterialTheme.typography.bodyMedium, color = VitroTextSecondary)
                }
            }
        }
        VitroGlassContainer(
            cornerRadius = 24.dp,
            glassOpacity = 0.24f,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(onClick = onOpenStudioFromLock)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = VitroCyanAccent)
                Column {
                    Text("Ubah Sandi & Layar Kunci", style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
                    Text("Buka Studio Vitro secara langsung", style = MaterialTheme.typography.bodyMedium, color = VitroTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun LockFocusNotePage(
    config: LauncherConfig,
    telemetry: DeviceTelemetry
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VitroGlassContainer(
            cornerRadius = 28.dp,
            glassOpacity = 0.25f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Catatan Layar Kunci",
                    style = MaterialTheme.typography.labelLarge,
                    color = VitroAmber
                )
                Text(
                    text = config.quickNoteText,
                    style = MaterialTheme.typography.titleMedium,
                    color = VitroTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${telemetry.timeText} • ${telemetry.dateText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VitroTextSecondary
                )
            }
        }
    }
}
