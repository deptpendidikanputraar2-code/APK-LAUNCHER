package com.example.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.PopupMessage
import com.example.launcher.model.PopupStyle
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun VitroPopupOverlay(
    popup: PopupMessage?,
    config: LauncherConfig,
    isPinnedToDock: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onToggleDockPin: (String) -> Unit,
    onOpenAppInfo: (InstalledApp) -> Unit
) {
    val activeStyle = if (popup?.isLockScreenContext == true) {
        config.lockPopupStyle
    } else {
        config.homePopupStyle
    }

    val alignment = when (activeStyle) {
        PopupStyle.DYNAMIC_ISLAND, PopupStyle.COMPACT_BUBBLE -> Alignment.TopCenter
        PopupStyle.FLOATING_GLASS_CARD -> Alignment.Center
        PopupStyle.BOTTOM_GLASS_SHEET, PopupStyle.STACKED_DECK -> Alignment.BottomCenter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = popup != null,
            enter = when (activeStyle) {
                PopupStyle.BOTTOM_GLASS_SHEET, PopupStyle.STACKED_DECK ->
                    fadeIn() + slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f)) { it / 2 }
                PopupStyle.DYNAMIC_ISLAND, PopupStyle.COMPACT_BUBBLE ->
                    fadeIn() + slideInVertically(spring(dampingRatio = 0.72f, stiffness = 460f)) { -it } + scaleIn(initialScale = 0.85f)
                PopupStyle.FLOATING_GLASS_CARD ->
                    fadeIn() + scaleIn(spring(dampingRatio = 0.72f, stiffness = 460f), initialScale = 0.88f)
            },
            exit = fadeOut() + scaleOut(targetScale = 0.92f) + slideOutVertically { if (alignment == Alignment.BottomCenter) it / 3 else -it / 3 }
        ) {
            if (popup != null) {
                PopupContentByStyle(
                    popup = popup,
                    style = activeStyle,
                    config = config,
                    isPinnedToDock = isPinnedToDock,
                    onDismiss = onDismiss,
                    onLaunchApp = onLaunchApp,
                    onToggleDockPin = onToggleDockPin,
                    onOpenAppInfo = onOpenAppInfo
                )
            }
        }
    }
}

@Composable
private fun PopupContentByStyle(
    popup: PopupMessage,
    style: PopupStyle,
    config: LauncherConfig,
    isPinnedToDock: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onToggleDockPin: (String) -> Unit,
    onOpenAppInfo: (InstalledApp) -> Unit
) {
    val corner = config.popupCornerRadius.dp

    when (style) {
        PopupStyle.DYNAMIC_ISLAND -> {
            VitroGlassContainer(
                cornerRadius = corner.coerceAtLeast(28.dp),
                tintColor = Color(0xFF070C1B),
                glassOpacity = (config.popupGlassOpacity + 0.42f).coerceAtMost(0.86f),
                borderColor = VitroCyanAccent.copy(alpha = 0.55f),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.94f)
                    .testTag("popup_dynamic_island")
            ) {
                PopupInnerBody(
                    popup = popup,
                    isPinnedToDock = isPinnedToDock,
                    onDismiss = onDismiss,
                    onLaunchApp = onLaunchApp,
                    onToggleDockPin = onToggleDockPin,
                    onOpenAppInfo = onOpenAppInfo
                )
            }
        }

        PopupStyle.STACKED_DECK -> {
            Box(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("popup_stacked_deck")
            ) {
                VitroGlassContainer(
                    cornerRadius = corner,
                    tintColor = Color.White,
                    glassOpacity = config.popupGlassOpacity * 0.65f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .offset(y = 14.dp)
                        .scale(0.92f)
                        .alpha(0.6f)
                ) {}
                VitroGlassContainer(
                    cornerRadius = corner,
                    tintColor = Color.White,
                    glassOpacity = config.popupGlassOpacity * 0.8f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .offset(y = 7.dp)
                        .scale(0.96f)
                        .alpha(0.8f)
                ) {}
                VitroGlassContainer(
                    cornerRadius = corner,
                    tintColor = Color(0xFF101C36),
                    glassOpacity = (config.popupGlassOpacity + 0.35f).coerceAtMost(0.82f),
                    borderColor = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PopupInnerBody(
                        popup = popup,
                        isPinnedToDock = isPinnedToDock,
                        onDismiss = onDismiss,
                        onLaunchApp = onLaunchApp,
                        onToggleDockPin = onToggleDockPin,
                        onOpenAppInfo = onOpenAppInfo
                    )
                }
            }
        }

        PopupStyle.COMPACT_BUBBLE -> {
            VitroGlassContainer(
                cornerRadius = 50.dp,
                tintColor = Color(0xFF0E1A33),
                glassOpacity = (config.popupGlassOpacity + 0.38f).coerceAtMost(0.85f),
                borderColor = VitroCyanAccent.copy(alpha = 0.55f),
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .testTag("popup_compact_bubble")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = VitroCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${popup.title}: ${popup.body}",
                        style = MaterialTheme.typography.labelLarge,
                        color = VitroTextPrimary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = VitroTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        PopupStyle.FLOATING_GLASS_CARD, PopupStyle.BOTTOM_GLASS_SHEET -> {
            VitroGlassContainer(
                cornerRadius = corner,
                tintColor = Color(0xFF0D172E),
                glassOpacity = (config.popupGlassOpacity + 0.35f).coerceAtMost(0.84f),
                borderColor = Color.White.copy(alpha = 0.42f),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .testTag("popup_glass_card")
            ) {
                PopupInnerBody(
                    popup = popup,
                    isPinnedToDock = isPinnedToDock,
                    onDismiss = onDismiss,
                    onLaunchApp = onLaunchApp,
                    onToggleDockPin = onToggleDockPin,
                    onOpenAppInfo = onOpenAppInfo
                )
            }
        }
    }
}

@Composable
private fun PopupInnerBody(
    popup: PopupMessage,
    isPinnedToDock: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onToggleDockPin: (String) -> Unit,
    onOpenAppInfo: (InstalledApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VitroCyanAccent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = VitroCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = popup.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = VitroTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VitroIndigo.copy(alpha = 0.35f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = popup.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = VitroTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = popup.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VitroTextSecondary
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = VitroTextSecondary
                )
            }
        }

        if (popup.targetApp != null) {
            val app = popup.targetApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PopupActionChip(
                    label = "Buka",
                    icon = Icons.Rounded.OpenInNew,
                    accent = VitroCyanAccent,
                    onClick = { onLaunchApp(app) },
                    modifier = Modifier.weight(1f)
                )
                PopupActionChip(
                    label = if (isPinnedToDock) "Lepas Dock" else "Ke Dock",
                    icon = Icons.Rounded.PushPin,
                    accent = VitroIndigo,
                    onClick = {
                        onToggleDockPin(app.packageName)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                PopupActionChip(
                    label = "Info",
                    icon = Icons.Rounded.Info,
                    accent = Color.White,
                    onClick = { onOpenAppInfo(app) },
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun PopupActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 16.dp,
        tintColor = accent,
        glassOpacity = 0.22f,
        modifier = modifier.bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VitroTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = VitroTextPrimary
            )
        }
    }
}
