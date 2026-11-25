package com.example.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.launcher.model.BatteryIconStyle
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.StatusSlotAlignment
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary

@Composable
fun TopStatusIndicatorArea(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    onOpenQuickPanel: () -> Unit,
    onTriggerPopup: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (config.statusPosition) {
        StatusIndicatorPosition.TOP_STATUS_BAR -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TopClassicStatusBar(
                    config = config,
                    telemetry = telemetry,
                    onOpenQuickPanel = onOpenQuickPanel,
                    onTriggerPopup = onTriggerPopup,
                    modifier = Modifier.fillMaxWidth(config.statusWidthPercent.coerceIn(0.45f, 1.0f))
                )
            }
        }
        StatusIndicatorPosition.DYNAMIC_PILL,
        StatusIndicatorPosition.FREE_DIY -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                VitroGlassContainer(
                    cornerRadius = 50.dp,
                    tintColor = Color(0xFF0A1024),
                    glassOpacity = 0.65f,
                    borderColor = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .testTag("dynamic_status_pill")
                        .bouncyClickable(onClick = onOpenQuickPanel)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (config.showClockIndicator) {
                            Text(
                                text = telemetry.timeText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = VitroTextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.isWifiConnected) VitroMint else VitroAmber)
                        )
                        StatusHardwareCluster(config = config, telemetry = telemetry)
                    }
                }
            }
        }
        else -> {
            // Subtle pull handle at top to still open Quick Panel smoothly
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clickable(onClick = onOpenQuickPanel),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun TopClassicStatusBar(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    onOpenQuickPanel: () -> Unit,
    onTriggerPopup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (config.statusAlignment) {
            StatusSlotAlignment.CENTERED_PILL -> Arrangement.Center
            else -> Arrangement.SpaceBetween
        }
    ) {
        when (config.statusAlignment) {
            StatusSlotAlignment.CLASSIC_SPLIT -> {
                if (config.showClockIndicator) {
                    Text(
                        text = telemetry.timeText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = VitroTextPrimary,
                        modifier = Modifier
                            .testTag("status_clock_button")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onTriggerPopup)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Row(
                    modifier = Modifier
                        .testTag("status_quick_panel_trigger")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenQuickPanel)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusHardwareCluster(config = config, telemetry = telemetry)
                }
            }

            StatusSlotAlignment.REVERSED_SPLIT -> {
                Row(
                    modifier = Modifier
                        .testTag("status_quick_panel_trigger")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenQuickPanel)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusHardwareCluster(config = config, telemetry = telemetry)
                }
                if (config.showClockIndicator) {
                    Text(
                        text = telemetry.timeText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = VitroTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onTriggerPopup)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            StatusSlotAlignment.CENTERED_PILL -> {
                Row(
                    modifier = Modifier
                        .testTag("status_quick_panel_trigger")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenQuickPanel)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (config.showClockIndicator) {
                        Text(
                            text = telemetry.timeText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = VitroTextPrimary
                        )
                    }
                    StatusHardwareCluster(config = config, telemetry = telemetry)
                }
            }
        }
    }
}

@Composable
fun CompactStatusCapsule(
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 50.dp,
        tintColor = Color(0xFF0C152D),
        glassOpacity = 0.58f,
        modifier = modifier
            .testTag("compact_status_capsule")
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (config.showClockIndicator) {
                Text(
                    text = telemetry.timeText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = VitroTextPrimary
                )
            }
            StatusHardwareCluster(config = config, telemetry = telemetry)
        }
    }
}

@Composable
fun StatusHardwareCluster(
    config: LauncherConfig,
    telemetry: DeviceTelemetry
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (config.showSignalIndicator) {
            Icon(
                imageVector = if (telemetry.signalBars >= 4) Icons.Rounded.SignalCellular4Bar
                else Icons.Rounded.SignalCellularAlt,
                contentDescription = "Sinyal seluler",
                tint = VitroTextPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
        if (config.showWifiIndicator) {
            Icon(
                imageVector = if (telemetry.isWifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = "Status Wi-Fi",
                tint = if (telemetry.isWifiConnected) VitroTextPrimary else VitroTextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
        BatteryBadge(
            percent = telemetry.batteryPercent,
            isCharging = telemetry.isCharging,
            style = config.batteryStyle,
            showPercent = config.showBatteryPercent
        )
    }
}

@Composable
fun BatteryBadge(
    percent: Int,
    isCharging: Boolean,
    style: BatteryIconStyle,
    showPercent: Boolean
) {
    val fillColor = when {
        isCharging -> VitroMint
        percent <= 20 -> VitroCoral
        percent <= 40 -> VitroAmber
        else -> VitroTextPrimary
    }

    when (style) {
        BatteryIconStyle.CAPSULE -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showPercent) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = VitroTextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, VitroTextPrimary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((percent / 100f).coerceIn(0.1f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(fillColor)
                    )
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Mengisi daya",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        BatteryIconStyle.RING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = fillColor,
                        startAngle = -90f,
                        sweepAngle = (percent / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                if (showPercent) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = VitroTextPrimary
                    )
                }
            }
        }

        BatteryIconStyle.TEXT_ONLY -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = stringResource(R.string.cd_open_quick_panel),
                        tint = VitroMint,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = fillColor
                )
            }
        }
    }
}
