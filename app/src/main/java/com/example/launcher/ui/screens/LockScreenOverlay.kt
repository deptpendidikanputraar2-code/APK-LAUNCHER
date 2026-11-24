package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.VitroWallpaperBackdrop
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun LockScreenOverlay(
    visible: Boolean,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    onUnlock: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onTriggerLockPopup: () -> Unit,
    onOpenStudioFromLock: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 5 },
        exit = fadeOut() + slideOutVertically { -it / 3 }
    ) {
        BackHandler(onBack = onUnlock)

        val lockPagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { config.lockPageCount.coerceIn(1, 4) }
        )
        var swipeAccum by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("lock_screen_overlay")
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { swipeAccum = 0f },
                        onVerticalDrag = { _, dragAmount -> swipeAccum += dragAmount },
                        onDragEnd = {
                            if (swipeAccum < -120f) onUnlock()
                            swipeAccum = 0f
                        }
                    )
                }
        ) {
            VitroWallpaperBackdrop(preset = config.wallpaperPreset, dimAlpha = 0.28f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Indicators on Lock Screen
                TopStatusIndicatorArea(
                    config = config,
                    telemetry = telemetry,
                    onOpenQuickPanel = onTriggerLockPopup,
                    onTriggerPopup = onTriggerLockPopup
                )

                // Multi-page Lock Screen Pager (1-4 pages with customizable scale & transitions)
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
                            .padding(horizontal = horizontalMargin, vertical = 12.dp)
                            .scale(config.lockPageScale.coerceIn(0.88f, 1.06f))
                    ) {
                        when (page) {
                            0 -> LockMainClockPage(
                                config = config,
                                telemetry = telemetry,
                                onTriggerLockPopup = onTriggerLockPopup
                            )
                            1 -> LockQuickWidgetsPage(
                                config = config,
                                telemetry = telemetry
                            )
                            2 -> LockQuickControlsPage(
                                telemetry = telemetry,
                                onToggleFlashlight = onToggleFlashlight,
                                onTriggerLockPopup = onTriggerLockPopup,
                                onOpenStudioFromLock = onOpenStudioFromLock
                            )
                            else -> LockFocusNotePage(config = config, telemetry = telemetry)
                        }
                    }
                }

                // Lock Screen Page Dots
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

                // Bottom Lock Screen Action Bar (Flashlight • Unlock Pill • Pop-up Test)
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
                            .bouncyClickable(onClick = onUnlock)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = "Buka kunci layar",
                                tint = VitroCyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Buka Layar",
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
                            contentDescription = "Uji pop-up layar kunci",
                            tint = VitroCyanAccent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.lock_screen_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = VitroTextSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LockMainClockPage(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    onTriggerLockPopup: () -> Unit
) {
    val clockFontSize = (72f * config.lockClockScale).coerceIn(50f, 96f).sp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 28.dp),
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
                            text = "27°C Cerah",
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

        // Interactive Lock Screen Popup Info Card
        VitroGlassContainer(
            cornerRadius = config.popupCornerRadius.dp,
            glassOpacity = 0.24f,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable(onClick = onTriggerLockPopup)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Gaya Pop-Up Layar Kunci: ${config.lockPopupStyle.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = VitroCyanAccent
                )
                Text(
                    text = "Ketuk kartu ini atau ikon lonceng di bawah untuk melihat pratinjau pop-up Layar Kunci. Geser ke kiri/kanan untuk halaman lainnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VitroTextSecondary
                )
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
                Text("Kalender", style = MaterialTheme.typography.labelLarge, color = VitroCyanAccent)
                Text(
                    text = telemetry.dateText,
                    style = MaterialTheme.typography.titleLarge,
                    color = VitroTextPrimary
                )
                Text(
                    text = telemetry.monthText,
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
                    Text("Ubah Halaman & Jam Layar Kunci", style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
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
