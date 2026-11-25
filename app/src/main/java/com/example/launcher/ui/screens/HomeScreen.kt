package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.DockSideAlignment
import com.example.launcher.model.HomeNavTab
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.SearchBarConfig
import com.example.launcher.model.SearchBarPosition
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.system.VitroAppWidgetManager
import com.example.launcher.ui.components.AndroidSystemWidgetCard
import com.example.launcher.ui.components.CompactStatusCapsule
import com.example.launcher.ui.components.SingleBuiltInGlassWidgetCard
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.UpwardCurvedGlassSidebar
import com.example.launcher.ui.components.VitroCustomizableSearchBar
import com.example.launcher.ui.components.VitroDockBar
import com.example.launcher.ui.components.VitroWidgetSection
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    isNotificationAccessEnabled: Boolean,
    appWidgetManager: VitroAppWidgetManager,
    activeNavTab: HomeNavTab,
    onLaunchApp: (InstalledApp) -> Unit,
    onLaunchAppPackage: (String) -> Unit,
    onLongPressApp: (InstalledApp) -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenWallpaperStudio: () -> Unit = onOpenStudio,
    onOpenUniversalSearch: () -> Unit,
    onEnterLockScreen: () -> Unit,
    onLockPhoneNow: () -> Unit,
    onDoubleTapLock: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onTriggerHomePopup: () -> Unit,
    onCycleWidgetStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onUpdateHomeWidgetLayout: (String, Float, Int, Float, Float) -> Unit,
    onMoveHomeWidgetPage: (String, Int) -> Unit,
    onRemoveHomeWidget: (String) -> Unit,
    onUpdateSearchBarLayout: (Float, Int, Float, Float) -> Unit,
    onUpdateSideDockOffset: (Float) -> Unit = {},
    onUpdateStatusLayout: (Float, Float) -> Unit = { _, _ -> }
) {
    val dockApps = remember(installedApps, config.pinnedDockPackages, config.dockMaxIcons) {
        val pinned = config.pinnedDockPackages.mapNotNull { pkg ->
            installedApps.firstOrNull { it.packageName == pkg }
        }
        if (pinned.isNotEmpty()) pinned else installedApps.take(config.dockMaxIcons)
    }

    val totalPages = config.homePageCount.coerceIn(1, 7)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )
    val scope = rememberCoroutineScope()
    var gestureAccum by remember { mutableFloatStateOf(0f) }
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var isDiyMode by remember { mutableStateOf(false) }

    if (isDiyMode) {
        BackHandler { isDiyMode = false }
    }

    val topSafeInset = if (config.hideSystemStatusBar) {
        Modifier.displayCutoutPadding().padding(top = 4.dp)
    } else {
        Modifier.statusBarsPadding()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(topSafeInset)
            .navigationBarsPadding()
            .pointerInput(config.doubleTapToLock, isDiyMode) {
                detectTapGestures(
                    onDoubleTap = { if (!isDiyMode) onDoubleTapLock() },
                    onLongPress = { isDiyMode = !isDiyMode }
                )
            }
            .pointerInput(isDiyMode) {
                if (!isDiyMode) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (gestureAccum > 95f) onOpenQuickPanel()
                            else if (gestureAccum < -95f) onOpenAppDrawer()
                            gestureAccum = 0f
                        },
                        onDragCancel = { gestureAccum = 0f },
                        onVerticalDrag = { _, dragAmount -> gestureAccum += dragAmount }
                    )
                }
            }
            .testTag("home_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Extra safe space if Dynamic Island is enabled so nothing overlaps the camera cutout
            if (config.dynamicIsland.enabled) {
                Spacer(modifier = Modifier.height((config.dynamicIsland.heightDp + config.dynamicIsland.topOffsetDp + 6f).dp))
            }

            // Floating Home DIY Mode Toolbar
            AnimatedVisibility(
                visible = isDiyMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE6071126))
                        .border(1.dp, VitroCyanAccent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode DIY Beranda",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = VitroCyanAccent
                        )
                        Text(
                            text = "Seret objek atau sudut kanan bawah untuk atur bebas",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextPrimary.copy(alpha = 0.8f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable {
                                    isDiyMode = false
                                    onOpenWallpaperStudio()
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("home_diy_wallpaper_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                                Text("Wallpaper", style = MaterialTheme.typography.labelSmall, color = VitroTextPrimary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable {
                                    isDiyMode = false
                                    onOpenStudio()
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                                Text("Widget", style = MaterialTheme.typography.labelSmall, color = VitroTextPrimary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VitroCyanAccent)
                                .clickable { isDiyMode = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("home_diy_done_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF040814), modifier = Modifier.size(15.dp))
                                Text("Selesai", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF040814))
                            }
                        }
                    }
                }
            }

            val showHomeTopStatus = (config.statusPosition == StatusIndicatorPosition.TOP_STATUS_BAR ||
                config.statusPosition == StatusIndicatorPosition.DYNAMIC_PILL) &&
                !(config.dynamicIsland.enabled && config.statusPosition == StatusIndicatorPosition.DYNAMIC_PILL)

            if (showHomeTopStatus) {
                DiyDraggableStatusWrapper(
                    offsetXDp = config.statusOffsetXDp,
                    offsetYDp = config.statusOffsetYDp,
                    scale = config.statusScale,
                    isDiyMode = isDiyMode,
                    onUpdateOffset = onUpdateStatusLayout
                ) {
                    TopStatusIndicatorArea(
                        config = config,
                        telemetry = telemetry,
                        onOpenQuickPanel = onOpenQuickPanel,
                        onTriggerPopup = onTriggerHomePopup
                    )
                }
            }

            // Home Search Bar at TOP position
            if (config.searchBar.enabledOnHome && config.searchBar.position == SearchBarPosition.TOP) {
                DiyHomeSearchBarBlock(
                    searchConfig = config.searchBar,
                    isDiyMode = isDiyMode,
                    onEnterDiy = { isDiyMode = true },
                    onTapSearch = onOpenUniversalSearch,
                    onOpenStudio = onOpenStudio,
                    onUpdateLayout = onUpdateSearchBarLayout
                )
            }

            // Middle main workspace (Side Dock sits beside widgets so widgets scroll cleanly without overlap)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (config.dockLayout == DockLayoutMode.LEFT_RAIL) {
                    DiySideDockSlot(
                        config = config,
                        dockApps = dockApps,
                        telemetry = telemetry,
                        isDiyMode = isDiyMode,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        onOpenQuickPanel = onOpenQuickPanel,
                        onUpdateSideDockOffset = onUpdateSideDockOffset
                    )
                }

                // Multi-page home canvas with DIY draggable & resizable Glass + Android AppWidgets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                    ) { page ->
                        val pageWidgets = config.homeWidgets.filter { it.pageIndex == page }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(config.homePageScale)
                                .applyPageTransition(
                                    pagerState = pagerState,
                                    page = page,
                                    style = config.pageTransitionStyle
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (page == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = { isDiyMode = true }
                                        )
                                ) {
                                    VitroWidgetSection(
                                        config = config,
                                        telemetry = telemetry,
                                        overlays = overlays,
                                        notifications = notifications,
                                        nowPlaying = nowPlaying,
                                        installedApps = installedApps,
                                        isNotificationAccessEnabled = isNotificationAccessEnabled,
                                        onCycleStack = onCycleWidgetStack,
                                        onToggleMedia = onToggleMedia,
                                        onToggleFocusTimer = onToggleFocusTimer,
                                        onResetFocusTimer = onResetFocusTimer,
                                        onOpenNotification = onOpenNotification,
                                        onDismissNotification = onDismissNotification,
                                        onEnableNotificationAccess = onEnableNotificationAccess,
                                        onLaunchApp = onLaunchApp,
                                        onOpenStudio = onOpenStudio
                                    )
                                }
                            }

                            // Home Search Bar at MIDDLE position (on Page 0)
                            if (page == 0 &&
                                config.searchBar.enabledOnHome &&
                                config.searchBar.position == SearchBarPosition.MIDDLE
                            ) {
                                DiyHomeSearchBarBlock(
                                    searchConfig = config.searchBar,
                                    isDiyMode = isDiyMode,
                                    onEnterDiy = { isDiyMode = true },
                                    onTapSearch = onOpenUniversalSearch,
                                    onOpenStudio = onOpenStudio,
                                    onUpdateLayout = onUpdateSearchBarLayout
                                )
                            }

                            // DIY Draggable & Resizable Home Widgets (2-column FlowRow for half-width or full-width!)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pageWidgets.forEach { placement ->
                                    DiyWidgetFrame(
                                        placement = placement,
                                        isDiyMode = isDiyMode,
                                        totalPages = totalPages,
                                        onEnterDiy = { isDiyMode = true },
                                        onUpdateLayout = { wPct, hDp, xOff, yOff ->
                                            onUpdateHomeWidgetLayout(placement.id, wPct, hDp, xOff, yOff)
                                        },
                                        onMovePage = { targetPage ->
                                            onMoveHomeWidgetPage(placement.id, targetPage)
                                        },
                                        onRemove = { onRemoveHomeWidget(placement.id) }
                                    ) { effectiveHeightDp ->
                                        when {
                                            placement.appWidgetId != null -> {
                                                AndroidSystemWidgetCard(
                                                    appWidgetId = placement.appWidgetId,
                                                    label = placement.label,
                                                    height = effectiveHeightDp.dp,
                                                    glassOpacity = config.widgetGlassOpacity,
                                                    appWidgetManager = appWidgetManager
                                                )
                                            }
                                            placement.builtInType != null -> {
                                                SingleBuiltInGlassWidgetCard(
                                                    type = placement.builtInType,
                                                    config = config,
                                                    telemetry = telemetry,
                                                    overlays = overlays,
                                                    notifications = notifications,
                                                    nowPlaying = nowPlaying,
                                                    installedApps = installedApps,
                                                    isNotificationAccessEnabled = isNotificationAccessEnabled,
                                                    onToggleMedia = onToggleMedia,
                                                    onToggleFocusTimer = onToggleFocusTimer,
                                                    onResetFocusTimer = onResetFocusTimer,
                                                    onOpenNotification = onOpenNotification,
                                                    onDismissNotification = onDismissNotification,
                                                    onEnableNotificationAccess = onEnableNotificationAccess,
                                                    onLaunchApp = onLaunchApp,
                                                    onOpenStudio = onOpenStudio,
                                                    modifier = Modifier.height(effectiveHeightDp.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }

                    // Page indicator dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalPages) { idx ->
                            val selected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(7.dp)
                                    .width(if (selected) 22.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }

                if (config.dockLayout == DockLayoutMode.RIGHT_RAIL) {
                    DiySideDockSlot(
                        config = config,
                        dockApps = dockApps,
                        telemetry = telemetry,
                        isDiyMode = isDiyMode,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        onOpenQuickPanel = onOpenQuickPanel,
                        onUpdateSideDockOffset = onUpdateSideDockOffset
                    )
                }
            }

            // Home Search Bar at ABOVE_DOCK position
            if (config.searchBar.enabledOnHome && config.searchBar.position == SearchBarPosition.ABOVE_DOCK) {
                DiyHomeSearchBarBlock(
                    searchConfig = config.searchBar,
                    isDiyMode = isDiyMode,
                    onEnterDiy = { isDiyMode = true },
                    onTapSearch = onOpenUniversalSearch,
                    onOpenStudio = onOpenStudio,
                    onUpdateLayout = onUpdateSearchBarLayout
                )
            }

            if (config.dockLayout == DockLayoutMode.FLOATING_BOTTOM ||
                config.dockLayout == DockLayoutMode.FULL_EDGE_BOTTOM
            ) {
                VitroDockBar(
                    config = config,
                    dockApps = dockApps,
                    telemetry = telemetry,
                    onLaunchApp = onLaunchApp,
                    onLongPressApp = onLongPressApp,
                    onOpenQuickPanel = onOpenQuickPanel
                )
            }

            // Home Search Bar at BOTTOM position (easy thumb reach at the bottom of Home Screen!)
            if (config.searchBar.enabledOnHome && config.searchBar.position == SearchBarPosition.BOTTOM) {
                DiyHomeSearchBarBlock(
                    searchConfig = config.searchBar,
                    isDiyMode = isDiyMode,
                    onEnterDiy = { isDiyMode = true },
                    onTapSearch = onOpenUniversalSearch,
                    onOpenStudio = onOpenStudio,
                    onUpdateLayout = onUpdateSearchBarLayout
                )
            }

            if (config.showGlassySidebar) {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        // Floating Free-DIY Search Bar (outside Column -> freely draggable ANYWHERE on the entire screen!)
        if (config.searchBar.enabledOnHome && config.searchBar.position == SearchBarPosition.FLOATING_DIY) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (config.showGlassySidebar) 68.dp else 20.dp)
            ) {
                DiyHomeSearchBarBlock(
                    searchConfig = config.searchBar,
                    isDiyMode = isDiyMode,
                    onEnterDiy = { isDiyMode = true },
                    onTapSearch = onOpenUniversalSearch,
                    onOpenStudio = onOpenStudio,
                    onUpdateLayout = onUpdateSearchBarLayout
                )
            }
        }

        // Free-DIY Status Indicator (draggable anywhere on the full screen!)
        if (config.statusPosition == StatusIndicatorPosition.FREE_DIY) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (config.dynamicIsland.enabled) 52.dp else 8.dp)
            ) {
                DiyDraggableStatusWrapper(
                    offsetXDp = config.statusOffsetXDp,
                    offsetYDp = config.statusOffsetYDp,
                    scale = config.statusScale,
                    isDiyMode = isDiyMode,
                    onUpdateOffset = onUpdateStatusLayout
                ) {
                    CompactStatusCapsule(
                        config = config,
                        telemetry = telemetry,
                        onClick = onOpenQuickPanel
                    )
                }
            }
        }

        if (config.statusPosition == StatusIndicatorPosition.BOTTOM_CAPSULE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 108.dp)
            ) {
                DiyDraggableStatusWrapper(
                    offsetXDp = config.statusOffsetXDp,
                    offsetYDp = config.statusOffsetYDp,
                    scale = config.statusScale,
                    isDiyMode = isDiyMode,
                    onUpdateOffset = onUpdateStatusLayout
                ) {
                    CompactStatusCapsule(
                        config = config,
                        telemetry = telemetry,
                        onClick = onOpenQuickPanel
                    )
                }
            }
        }

        // Fully Customizable Upward-Expanding Curved Glassy Sidebar
        if (config.showGlassySidebar) {
            UpwardCurvedGlassSidebar(
                isOpen = isSidebarExpanded,
                config = config,
                installedApps = installedApps,
                bottomClearance = if (config.dockLayout == DockLayoutMode.FLOATING_BOTTOM || config.dockLayout == DockLayoutMode.FULL_EDGE_BOTTOM) 96.dp else 18.dp,
                activeNavTab = activeNavTab,
                telemetry = telemetry,
                onToggleOpen = { isSidebarExpanded = it },
                onSelectHome = { scope.launch { pagerState.animateScrollToPage(0) } },
                onCycleWidget = { onCycleWidgetStack(1) },
                onOpenQuickPanel = onOpenQuickPanel,
                onOpenAppDrawer = onOpenAppDrawer,
                onTriggerPopup = onTriggerHomePopup,
                onToggleFlashlight = onToggleFlashlight,
                onEnterLockScreen = onEnterLockScreen,
                onLockPhoneNow = onLockPhoneNow,
                onOpenStudio = onOpenStudio,
                onLaunchAppPackage = onLaunchAppPackage
            )
        }
    }
}

@Composable
private fun DiySideDockSlot(
    config: LauncherConfig,
    dockApps: List<InstalledApp>,
    telemetry: DeviceTelemetry,
    isDiyMode: Boolean,
    onLaunchApp: (InstalledApp) -> Unit,
    onLongPressApp: (InstalledApp) -> Unit,
    onOpenQuickPanel: () -> Unit,
    onUpdateSideDockOffset: (Float) -> Unit
) {
    val density = LocalDensity.current
    var sideYOff by remember(config.dockSideVerticalOffsetDp) {
        mutableFloatStateOf(config.dockSideVerticalOffsetDp)
    }

    val verticalAlign = when (config.dockSideAlignment) {
        DockSideAlignment.TOP -> Alignment.TopCenter
        DockSideAlignment.CENTER -> Alignment.Center
        DockSideAlignment.BOTTOM -> Alignment.BottomCenter
    }

    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = verticalAlign
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, with(density) { sideYOff.dp.roundToPx() }) }
                .then(
                    if (isDiyMode) {
                        Modifier
                            .border(1.5.dp, VitroCyanAccent, RoundedCornerShape((config.dockCornerRadius + 4f).dp))
                            .pointerInput(config.dockSideAlignment) {
                                detectDragGestures(
                                    onDragEnd = { onUpdateSideDockOffset(sideYOff) },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        sideYOff = (sideYOff + with(density) { drag.y.toDp().value })
                                            .coerceIn(-260f, 260f)
                                    }
                                )
                            }
                    } else Modifier
                )
                .testTag("diy_side_dock_slot")
        ) {
            VitroDockBar(
                config = config,
                dockApps = dockApps,
                telemetry = telemetry,
                onLaunchApp = onLaunchApp,
                onLongPressApp = onLongPressApp,
                onOpenQuickPanel = onOpenQuickPanel
            )
            if (isDiyMode && sideYOff != 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE0B132B))
                        .clickable {
                            sideYOff = 0f
                            onUpdateSideDockOffset(0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Reset Posisi Dock",
                        tint = VitroCyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiyDraggableStatusWrapper(
    offsetXDp: Float,
    offsetYDp: Float,
    scale: Float,
    isDiyMode: Boolean,
    onUpdateOffset: (Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var xOff by remember(offsetXDp) { mutableFloatStateOf(offsetXDp) }
    var yOff by remember(offsetYDp) { mutableFloatStateOf(offsetYDp) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { xOff.dp.roundToPx() },
                    with(density) { yOff.dp.roundToPx() }
                )
            }
            .scale(scale.coerceIn(0.70f, 1.50f))
            .then(
                if (isDiyMode) {
                    Modifier
                        .border(1.4.dp, VitroCyanAccent, RoundedCornerShape(24.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onUpdateOffset(xOff, yOff) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    xOff = (xOff + with(density) { drag.x.toDp().value }).coerceIn(-180f, 180f)
                                    yOff = (yOff + with(density) { drag.y.toDp().value }).coerceIn(-40f, 680f)
                                }
                            )
                        }
                } else Modifier
            )
            .testTag("diy_draggable_status_indicator")
    ) {
        content()
        if (isDiyMode && (xOff != 0f || yOff != 0f)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xEE0B132B))
                    .clickable {
                        xOff = 0f
                        yOff = 0f
                        onUpdateOffset(0f, 0f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset Posisi Indikator", tint = VitroCyanAccent, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiyHomeSearchBarBlock(
    searchConfig: SearchBarConfig,
    isDiyMode: Boolean,
    onEnterDiy: () -> Unit,
    onTapSearch: () -> Unit,
    onOpenStudio: () -> Unit,
    onUpdateLayout: (Float, Int, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var widthPct by remember(searchConfig.widthPercent) { mutableFloatStateOf(searchConfig.widthPercent) }
    var heightDp by remember(searchConfig.heightDp) { mutableFloatStateOf(searchConfig.heightDp.toFloat()) }
    var xOff by remember(searchConfig.xOffsetDp) { mutableFloatStateOf(searchConfig.xOffsetDp) }
    var yOff by remember(searchConfig.yOffsetDp) { mutableFloatStateOf(searchConfig.yOffsetDp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthPct.coerceIn(0.45f, 1.0f))
                .offset {
                    IntOffset(
                        with(density) { xOff.dp.roundToPx() },
                        with(density) { yOff.dp.roundToPx() }
                    )
                }
                .then(
                    if (isDiyMode) {
                        Modifier
                            .border(1.4.dp, VitroCyanAccent, RoundedCornerShape((searchConfig.shape.cornerDp + 2f).dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        xOff = (xOff + with(density) { drag.x.toDp().value }).coerceIn(-180f, 180f)
                                        yOff = (yOff + with(density) { drag.y.toDp().value }).coerceIn(-460f, 760f)
                                    }
                                )
                            }
                    } else {
                        Modifier.combinedClickable(
                            onClick = onTapSearch,
                            onLongClick = onEnterDiy
                        )
                    }
                )
        ) {
            VitroCustomizableSearchBar(
                searchConfig = searchConfig.copy(
                    widthPercent = widthPct,
                    heightDp = heightDp.roundToInt()
                ),
                query = "",
                readOnly = true,
                onClick = if (isDiyMode) null else onTapSearch,
                placeholder = "Cari aplikasi, widget, pengaturan...",
                trailingActionIcon = if (!isDiyMode) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Atur Gaya Pencarian",
                            tint = searchConfig.colorPreset.contentColor.copy(alpha = 0.78f),
                            modifier = Modifier
                                .size(19.dp)
                                .clickable(onClick = onOpenStudio)
                        )
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            if (isDiyMode) {
                // Reset position pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-10).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE0B132B))
                        .clickable {
                            xOff = 0f
                            yOff = 0f
                            widthPct = 0.94f
                            heightDp = 48f
                            onUpdateLayout(0.94f, 48, 0f, 0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = VitroCyanAccent, modifier = Modifier.size(13.dp))
                }

                // Live drag resize corner handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(VitroCyanAccent)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    widthPct = (widthPct + with(density) { drag.x.toDp().value } / 320f).coerceIn(0.45f, 1.0f)
                                    heightDp = (heightDp + with(density) { drag.y.toDp().value }).coerceIn(40f, 68f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.OpenWith, contentDescription = "Ubah Ukuran Cari", tint = Color(0xFF040814), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiyWidgetFrame(
    placement: HomeWidgetPlacement,
    isDiyMode: Boolean,
    totalPages: Int,
    onEnterDiy: () -> Unit,
    onUpdateLayout: (Float, Int, Float, Float) -> Unit,
    onMovePage: (Int) -> Unit,
    onRemove: () -> Unit,
    content: @Composable (effectiveHeightDp: Int) -> Unit
) {
    val density = LocalDensity.current
    var widthPct by remember(placement.id, placement.widthPercent) { mutableFloatStateOf(placement.widthPercent) }
    var heightDp by remember(placement.id, placement.heightDp) { mutableFloatStateOf(placement.heightDp.toFloat()) }
    var xOff by remember(placement.id, placement.xOffsetDp) { mutableFloatStateOf(placement.xOffsetDp) }
    var yOff by remember(placement.id, placement.yOffsetDp) { mutableFloatStateOf(placement.yOffsetDp) }

    val effectiveWidth = (if (isDiyMode) widthPct else placement.widthPercent).coerceIn(0.44f, 0.99f)
    val effectiveHeight = (if (isDiyMode) heightDp.roundToInt() else placement.heightDp).coerceIn(72, 420)

    Box(
        modifier = Modifier
            .fillMaxWidth(effectiveWidth)
            .offset {
                IntOffset(
                    with(density) { xOff.dp.roundToPx() },
                    with(density) { yOff.dp.roundToPx() }
                )
            }
            .then(
                if (isDiyMode) {
                    Modifier
                        .border(1.5.dp, VitroCyanAccent.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
                        .pointerInput(placement.id) {
                            detectDragGestures(
                                onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    xOff = (xOff + with(density) { drag.x.toDp().value }).coerceIn(-120f, 120f)
                                    yOff = (yOff + with(density) { drag.y.toDp().value }).coerceIn(-120f, 240f)
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
    ) {
        content(effectiveHeight)

        if (isDiyMode) {
            // Top-left remove + reset position buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(VitroCoral)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Hapus Widget", tint = Color.White, modifier = Modifier.size(15.dp))
                }
                if (xOff != 0f || yOff != 0f) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xDD0B132B))
                            .clickable {
                                xOff = 0f
                                yOff = 0f
                                onUpdateLayout(widthPct, heightDp.roundToInt(), 0f, 0f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset Posisi", tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Top-right page shift controls
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (placement.pageIndex > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xDD0B132B))
                                .clickable { onMovePage(placement.pageIndex - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Pindah Halaman Kiri", tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (placement.pageIndex < totalPages - 1) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xDD0B132B))
                                .clickable { onMovePage(placement.pageIndex + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Pindah Halaman Kanan", tint = VitroCyanAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Bottom-right corner resize drag handle (resizes BOTH width & height freely!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VitroCyanAccent)
                    .pointerInput(placement.id) {
                        detectDragGestures(
                            onDragEnd = { onUpdateLayout(widthPct, heightDp.roundToInt(), xOff, yOff) },
                            onDrag = { change, drag ->
                                change.consume()
                                widthPct = (widthPct + with(density) { drag.x.toDp().value } / 320f).coerceIn(0.44f, 1.0f)
                                heightDp = (heightDp + with(density) { drag.y.toDp().value }).coerceIn(72f, 420f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.OpenWith, contentDescription = "Ubah Ukuran Widget", tint = Color(0xFF040814), modifier = Modifier.size(15.dp))
            }
        }
    }
}
