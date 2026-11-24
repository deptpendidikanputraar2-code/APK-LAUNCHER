package com.example.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.StatusIndicatorPosition
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun VitroDockBar(
    config: LauncherConfig,
    dockApps: List<InstalledApp>,
    telemetry: DeviceTelemetry,
    onLaunchApp: (InstalledApp) -> Unit,
    onLongPressApp: (InstalledApp) -> Unit,
    onOpenQuickPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVertical = config.dockLayout == DockLayoutMode.LEFT_RAIL ||
        config.dockLayout == DockLayoutMode.RIGHT_RAIL
    val isFullEdge = config.dockLayout == DockLayoutMode.FULL_EDGE_BOTTOM

    val shape = if (isFullEdge) {
        RoundedCornerShape(
            topStart = config.dockCornerRadius.dp,
            topEnd = config.dockCornerRadius.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    } else {
        RoundedCornerShape(config.dockCornerRadius.dp)
    }

    val containerPadding = when {
        isFullEdge -> Modifier.fillMaxWidth()
        isVertical -> Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        else -> Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    }

    val visibleApps = dockApps.take(config.dockMaxIcons.coerceIn(3, 6))

    VitroGlassContainer(
        shape = shape,
        tintColor = config.dockColor.baseColor,
        glassOpacity = config.dockOpacity,
        borderColor = config.dockColor.borderColor,
        glowColor = VitroCyanAccent,
        glowIntensity = config.ambientGlowIntensity * 0.45f,
        modifier = modifier
            .then(containerPadding)
            .testTag("launcher_dock_bar")
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (config.statusPosition == StatusIndicatorPosition.INSIDE_DOCK) {
                    Text(
                        text = telemetry.timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroTextPrimary,
                        modifier = Modifier.bouncyClickable(onClick = onOpenQuickPanel)
                    )
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = VitroCyanAccent,
                        modifier = Modifier.bouncyClickable(onClick = onOpenQuickPanel)
                    )
                }
                visibleApps.forEach { app ->
                    DockAppIconItem(
                        app = app,
                        showLabel = config.showDockLabels,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onLongPressApp(app) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (config.statusPosition == StatusIndicatorPosition.INSIDE_DOCK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .bouncyClickable(onClick = onOpenQuickPanel),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${telemetry.timeText} • ${telemetry.dateText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextPrimary.copy(alpha = 0.85f)
                        )
                        StatusHardwareCluster(config = config, telemetry = telemetry)
                    }
                }
                if (visibleApps.isEmpty()) {
                    Text(
                        text = "Sematkan aplikasi favorit ke Dock Bar",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroTextSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleApps.forEach { app ->
                            DockAppIconItem(
                                app = app,
                                showLabel = config.showDockLabels,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppIconItem(
    app: InstalledApp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            isHovered -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "dock_icon_scale"
    )

    Column(
        modifier = Modifier
            .width(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .hoverable(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = VitroCyanAccent),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = if (isHovered) 0.24f else 0.14f))
                .border(
                    width = 1.dp,
                    color = if (isHovered) VitroCyanAccent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = VitroTextPrimary
                )
            }
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
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
