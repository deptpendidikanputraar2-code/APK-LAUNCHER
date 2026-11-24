package com.example.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.HomeNavTab
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.SidebarItemType
import com.example.launcher.model.SidebarSide
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroSapphire
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ResolvedSidebarItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val appIcon: ImageBitmap? = null,
    val accent: Color,
    val navTab: HomeNavTab? = null,
    val keepOpenOnClick: Boolean = false,
    val action: () -> Unit
)

/**
 * Fully customizable Upward-Expanding Curved-Outside Active Tab Glassmorphism Sidebar.
 * Users can toggle, reorder, and add ANY installed Android app inside Studio Vitro → Sidebar.
 */
@Composable
fun UpwardCurvedGlassSidebar(
    isOpen: Boolean,
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    bottomClearance: Dp,
    activeNavTab: HomeNavTab,
    telemetry: DeviceTelemetry,
    onToggleOpen: (Boolean) -> Unit,
    onSelectHome: () -> Unit,
    onCycleWidget: () -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onTriggerPopup: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onEnterLockScreen: () -> Unit,
    onLockPhoneNow: () -> Unit,
    onOpenStudio: () -> Unit,
    onLaunchAppPackage: (String) -> Unit
) {
    if (isOpen) {
        BackHandler { onToggleOpen(false) }
    }

    val scope = rememberCoroutineScope()
    val isLeft = config.sidebarSide == SidebarSide.LEFT

    val menuItems = remember(
        config.sidebarItems,
        installedApps,
        onSelectHome,
        onCycleWidget,
        onOpenQuickPanel,
        onOpenAppDrawer,
        onTriggerPopup,
        onToggleFlashlight,
        onEnterLockScreen,
        onLockPhoneNow,
        onOpenStudio,
        onLaunchAppPackage
    ) {
        config.sidebarItems
            .filter { it.enabled }
            .mapNotNull { item ->
                when (item.type) {
                    SidebarItemType.HOME -> ResolvedSidebarItem(
                        item.id, "Beranda", Icons.Rounded.Home, null, VitroCyanAccent, HomeNavTab.HOME, false, onSelectHome
                    )
                    SidebarItemType.WIDGETS -> ResolvedSidebarItem(
                        item.id, "Widget", Icons.Rounded.Widgets, null, VitroIndigo, null, true, onCycleWidget
                    )
                    SidebarItemType.QUICK_PANEL -> ResolvedSidebarItem(
                        item.id, "Pusat Kontrol", Icons.Rounded.ControlCamera, null, VitroSapphire, HomeNavTab.PANEL, false, onOpenQuickPanel
                    )
                    SidebarItemType.APPS -> ResolvedSidebarItem(
                        item.id, "Semua Aplikasi", Icons.Rounded.Apps, null, VitroMint, HomeNavTab.APPS, false, onOpenAppDrawer
                    )
                    SidebarItemType.NOTIFICATIONS -> ResolvedSidebarItem(
                        item.id, "Notifikasi", Icons.Rounded.NotificationsActive, null, VitroAmber, null, false, onTriggerPopup
                    )
                    SidebarItemType.FLASHLIGHT -> ResolvedSidebarItem(
                        item.id, "Senter", Icons.Rounded.FlashlightOn, null, VitroAmber, null, true, onToggleFlashlight
                    )
                    SidebarItemType.LOCK_SCREEN -> ResolvedSidebarItem(
                        item.id, "Layar Kunci", Icons.Rounded.Lock, null, VitroCoral, HomeNavTab.LOCK, false, onEnterLockScreen
                    )
                    SidebarItemType.LOCK_PHONE -> ResolvedSidebarItem(
                        item.id, "Kunci HP", Icons.Rounded.PhonelinkLock, null, VitroCoral, null, false, onLockPhoneNow
                    )
                    SidebarItemType.STUDIO -> ResolvedSidebarItem(
                        item.id, "Studio Vitro", Icons.Rounded.Tune, null, VitroCyanAccent, HomeNavTab.STUDIO, false, onOpenStudio
                    )
                    SidebarItemType.APP_SHORTCUT -> {
                        val pkg = item.packageName ?: return@mapNotNull null
                        val app = installedApps.firstOrNull { it.packageName == pkg }
                        val label = app?.label ?: pkg.substringAfterLast('.')
                        ResolvedSidebarItem(
                            id = item.id,
                            label = label,
                            icon = Icons.Rounded.Apps,
                            appIcon = app?.iconBitmap,
                            accent = VitroCyanAccent,
                            navTab = null,
                            keepOpenOnClick = false,
                            action = { onLaunchAppPackage(pkg) }
                        )
                    }
                }
            }
            .ifEmpty {
                listOf(
                    ResolvedSidebarItem("studio_fallback", "Studio Vitro", Icons.Rounded.Tune, null, VitroCyanAccent, HomeNavTab.STUDIO, false, onOpenStudio)
                )
            }
    }

    var selectedIndex by remember(activeNavTab, menuItems.size) {
        val matched = menuItems.indexOfFirst { it.navTab == activeNavTab }
        mutableIntStateOf(if (matched >= 0) matched else 0)
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, menuItems.lastIndex)

    val expandedHeight = (108 + menuItems.size * 52).coerceIn(220, 490).dp
    val containerHeight by animateDpAsState(
        targetValue = if (isOpen) expandedHeight else 44.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 310f),
        label = "sidebar_upward_height"
    )
    val containerWidth by animateDpAsState(
        targetValue = if (isOpen) config.sidebarWidthDp.coerceIn(180f, 270f).dp else 156.dp,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
        label = "sidebar_upward_width"
    )
    val containerRadius by animateDpAsState(
        targetValue = if (isOpen) 28.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "sidebar_corner_radius"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.48f else 0f,
        animationSpec = tween(260),
        label = "sidebar_scrim_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pill_glow_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_pulse_alpha"
    )

    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isOpen || scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF040814).copy(alpha = scrimAlpha))
                    .then(
                        if (isOpen) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { onToggleOpen(false) }
                            }
                        } else Modifier
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomClearance),
            contentAlignment = if (isLeft) Alignment.BottomStart else Alignment.BottomEnd
        ) {
            val shape = RoundedCornerShape(containerRadius)
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isOpen) 0.52f else 0.62f),
                    VitroCyanAccent.copy(alpha = (0.55f * pulse * config.ambientGlowIntensity.coerceAtLeast(0.35f)).coerceAtMost(0.85f)),
                    VitroIndigo.copy(alpha = 0.42f)
                )
            )
            val panelAlpha = config.sidebarGlassOpacity.coerceIn(0.45f, 0.96f)

            Box(
                modifier = Modifier
                    .width(containerWidth)
                    .height(containerHeight)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D1936).copy(alpha = if (isOpen) panelAlpha else 0.74f),
                                Color(0xFF132245).copy(alpha = if (isOpen) (panelAlpha - 0.04f).coerceAtLeast(0.4f) else 0.66f),
                                Color(0xFF0A1228).copy(alpha = if (isOpen) (panelAlpha + 0.04f).coerceAtMost(0.96f) else 0.76f)
                            )
                        )
                    )
                    .border(width = 1.dp, brush = borderBrush, shape = shape)
                    .pointerInput(isOpen) {
                        detectVerticalDragGestures(
                            onDragStart = { verticalDrag = 0f },
                            onVerticalDrag = { _, dragAmount -> verticalDrag += dragAmount },
                            onDragEnd = {
                                if (!isOpen && verticalDrag < -20f) onToggleOpen(true)
                                else if (isOpen && verticalDrag > 32f) onToggleOpen(false)
                                verticalDrag = 0f
                            }
                        )
                    }
                    .testTag(if (isOpen) "upward_curved_sidebar_open" else "upward_curved_sidebar_pill")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    AnimatedVisibility(
                        visible = isOpen,
                        enter = fadeIn(tween(240, delayMillis = 60)),
                        exit = fadeOut(tween(130)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Vitro v7",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    ),
                                    color = VitroTextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${telemetry.timeText} • ${telemetry.batteryPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VitroCyanAccent
                                    )
                                    // Always-accessible subtle Studio shortcut so user can never hide settings permanently
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .clickable {
                                                onToggleOpen(false)
                                                onOpenStudio()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Pengaturan Studio Vitro",
                                            tint = VitroTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val itemHeight = 52.dp
                            val verticalPadding = 14.dp
                            val activeTopDp by animateDpAsState(
                                targetValue = verticalPadding + itemHeight * safeSelectedIndex,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
                                label = "curved_outside_tab_y"
                            )
                            val activeAccent by animateColorAsState(
                                targetValue = menuItems[safeSelectedIndex].accent,
                                animationSpec = tween(260),
                                label = "curved_tab_accent"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val totalListHeight = verticalPadding * 2 + itemHeight * menuItems.size

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(totalListHeight)
                                ) {
                                    val w = size.width
                                    val tabTop = activeTopDp.toPx()
                                    val tabH = itemHeight.toPx()
                                    val tabBot = tabTop + tabH
                                    val inset = 12.dp.toPx()
                                    val pillR = tabH / 2f
                                    val curveR = 18.dp.toPx()

                                    val path = Path().apply {
                                        if (isLeft) {
                                            moveTo(w, tabTop - curveR)
                                            quadraticTo(w, tabTop, w - curveR, tabTop)
                                            lineTo(inset + pillR, tabTop)
                                            arcTo(
                                                rect = Rect(inset, tabTop, inset + tabH, tabBot),
                                                startAngleDegrees = -90f,
                                                sweepAngleDegrees = -180f,
                                                forceMoveTo = false
                                            )
                                            lineTo(w - curveR, tabBot)
                                            quadraticTo(w, tabBot, w, tabBot + curveR)
                                            close()
                                        } else {
                                            moveTo(0f, tabTop - curveR)
                                            quadraticTo(0f, tabTop, curveR, tabTop)
                                            lineTo(w - inset - pillR, tabTop)
                                            arcTo(
                                                rect = Rect(w - inset - tabH, tabTop, w - inset, tabBot),
                                                startAngleDegrees = -90f,
                                                sweepAngleDegrees = 180f,
                                                forceMoveTo = false
                                            )
                                            lineTo(curveR, tabBot)
                                            quadraticTo(0f, tabBot, 0f, tabBot + curveR)
                                            close()
                                        }
                                    }

                                    drawPath(
                                        path = path,
                                        brush = Brush.horizontalGradient(
                                            colors = if (isLeft) listOf(
                                                activeAccent.copy(alpha = 0.28f),
                                                activeAccent.copy(alpha = 0.44f),
                                                Color.White.copy(alpha = 0.28f)
                                            ) else listOf(
                                                Color.White.copy(alpha = 0.28f),
                                                activeAccent.copy(alpha = 0.44f),
                                                activeAccent.copy(alpha = 0.28f)
                                            )
                                        )
                                    )

                                    drawPath(
                                        path = path,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                activeAccent.copy(alpha = 0.85f),
                                                Color.White.copy(alpha = 0.72f),
                                                activeAccent.copy(alpha = 0.85f)
                                            )
                                        ),
                                        style = Stroke(width = 1.25.dp.toPx())
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = verticalPadding)
                                ) {
                                    menuItems.forEachIndexed { index, item ->
                                        CurvedSidebarMenuRow(
                                            item = item,
                                            selected = index == safeSelectedIndex,
                                            isLeftSidebar = isLeft,
                                            height = itemHeight,
                                            onClick = {
                                                selectedIndex = index
                                                if (item.keepOpenOnClick) {
                                                    item.action()
                                                } else {
                                                    scope.launch {
                                                        delay(180L)
                                                        onToggleOpen(false)
                                                        item.action()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SidebarBottomAnchorPillRow(
                        isOpen = isOpen,
                        pulse = pulse,
                        onClick = { onToggleOpen(!isOpen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CurvedSidebarMenuRow(
    item: ResolvedSidebarItem,
    selected: Boolean,
    isLeftSidebar: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            isHovered -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "curved_row_scale"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            isHovered -> item.accent
            else -> VitroTextSecondary
        },
        animationSpec = tween(200),
        label = "curved_row_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = item.accent),
                onClick = onClick
            )
            .padding(
                start = if (isLeftSidebar) 24.dp else 18.dp,
                end = if (isLeftSidebar) 18.dp else 24.dp
            )
            .testTag("curved_sidebar_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (item.appIcon != null) {
            Image(
                bitmap = item.appIcon,
                contentDescription = item.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) item.accent.copy(alpha = 0.92f)
                        else Color.White.copy(alpha = if (isHovered) 0.16f else 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) Color(0xFF070B18) else contentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.2.sp
            ),
            color = if (selected) VitroTextPrimary else contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SidebarBottomAnchorPillRow(
    isOpen: Boolean,
    pulse: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            isHovered -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 520f),
        label = "anchor_pill_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .scale(scale)
            .background(if (isOpen) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = VitroCyanAccent),
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
            .testTag("sidebar_anchor_toggle_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = if (isOpen) "Tutup sidebar" else "Buka sidebar",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = if (isOpen) "Tutup Menu" else "Vitro v7",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = VitroTextPrimary,
                maxLines = 1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!isOpen) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(VitroCyanAccent.copy(alpha = pulse))
                )
            }
            Icon(
                imageVector = if (isOpen) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = if (isOpen) "Tutup" else "Buka ke atas",
                tint = VitroCyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
