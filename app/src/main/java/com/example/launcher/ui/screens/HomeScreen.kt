package com.example.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.HomeNavTab
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.system.VitroAppWidgetManager
import com.example.launcher.ui.components.AndroidSystemWidgetCard
import com.example.launcher.ui.components.CompactStatusCapsule
import com.example.launcher.ui.components.SingleBuiltInGlassWidgetCard
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.UpwardCurvedGlassSidebar
import com.example.launcher.ui.components.VitroDockBar
import com.example.launcher.ui.components.VitroWidgetSection
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroCyanAccent
import kotlinx.coroutines.launch

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
    onEnableNotificationAccess: () -> Unit
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
    val coroutineScope = rememberCoroutineScope()
    var verticalSwipeSum by remember { mutableFloatStateOf(0f) }

    var isSidebarOpen by remember { mutableStateOf(false) }

    val hasBottomDock = config.dockLayout == DockLayoutMode.FLOATING_BOTTOM ||
        config.dockLayout == DockLayoutMode.FULL_EDGE_BOTTOM

    // Prevent overlap with native status bar or display camera cutout
    val topSafeModifier = if (config.hideSystemStatusBar) {
        Modifier.displayCutoutPadding().padding(top = 6.dp)
    } else {
        Modifier.statusBarsPadding()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(topSafeModifier)
                .navigationBarsPadding()
                .pointerInput(config.doubleTapToLock) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTapLock() },
                        onLongPress = { onOpenStudio() }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { verticalSwipeSum = 0f },
                        onVerticalDrag = { _, dragAmount -> verticalSwipeSum += dragAmount },
                        onDragEnd = {
                            if (verticalSwipeSum > 85f) onOpenQuickPanel()
                            else if (verticalSwipeSum < -85f) onOpenAppDrawer()
                            verticalSwipeSum = 0f
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Custom Status Indicators (zero native overlap when hideSystemStatusBar=true)
                TopStatusIndicatorArea(
                    config = config,
                    telemetry = telemetry,
                    onOpenQuickPanel = onOpenQuickPanel,
                    onTriggerPopup = onTriggerHomePopup
                )

                // Extra clearance when Dynamic Island is enabled so widgets sit cleanly below it
                if (config.dynamicIsland.enabled && config.statusPosition == StatusIndicatorPosition.HIDDEN) {
                    Spacer(modifier = Modifier.height((config.dynamicIsland.heightDp + 10f).dp))
                }

                // 2. Pure Widget-Only Multi-Page Workspace (Glass Widgets + Real Android AppWidgets)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.dockLayout == DockLayoutMode.LEFT_RAIL) {
                        VitroDockBar(
                            config = config,
                            dockApps = dockApps,
                            telemetry = telemetry,
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = onLongPressApp,
                            onOpenQuickPanel = onOpenQuickPanel
                        )
                    }

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
                                .testTag("home_screen_pager")
                        ) { pageIndex ->
                            WidgetOnlyHomePage(
                                pageIndex = pageIndex,
                                config = config,
                                telemetry = telemetry,
                                overlays = overlays,
                                notifications = notifications,
                                nowPlaying = nowPlaying,
                                installedApps = installedApps,
                                isNotificationAccessEnabled = isNotificationAccessEnabled,
                                appWidgetManager = appWidgetManager,
                                onCycleWidgetStack = onCycleWidgetStack,
                                onToggleMedia = onToggleMedia,
                                onToggleFocusTimer = onToggleFocusTimer,
                                onResetFocusTimer = onResetFocusTimer,
                                onOpenNotification = onOpenNotification,
                                onDismissNotification = onDismissNotification,
                                onEnableNotificationAccess = onEnableNotificationAccess,
                                onLaunchApp = onLaunchApp,
                                onOpenStudio = onOpenStudio,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .applyPageTransition(
                                        pagerState = pagerState,
                                        page = pageIndex,
                                        style = config.pageTransitionStyle
                                    )
                            )
                        }

                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(totalPages) { idx ->
                                    val selected = pagerState.currentPage == idx
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.5.dp)
                                            .width(if (selected) 20.dp else 5.dp)
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) VitroCyanAccent
                                                else Color.White.copy(alpha = 0.28f)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    if (config.dockLayout == DockLayoutMode.RIGHT_RAIL) {
                        VitroDockBar(
                            config = config,
                            dockApps = dockApps,
                            telemetry = telemetry,
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = onLongPressApp,
                            onOpenQuickPanel = onOpenQuickPanel
                        )
                    }
                }

                if (config.showGlassySidebar) {
                    Spacer(modifier = Modifier.height(54.dp))
                }

                // 3. Clean App-Only Glass Dock Bar
                if (hasBottomDock) {
                    VitroDockBar(
                        config = config,
                        dockApps = dockApps,
                        telemetry = telemetry,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        onOpenQuickPanel = onOpenQuickPanel
                    )
                }
            }

            if (config.statusPosition == StatusIndicatorPosition.BOTTOM_CAPSULE) {
                CompactStatusCapsule(
                    config = config,
                    telemetry = telemetry,
                    onClick = onOpenQuickPanel,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (hasBottomDock) 138.dp else 66.dp)
                )
            }
        }

        // Layer B: Customizable Upward-Expanding Curved-Outside Active Tab Sidebar
        if (config.showGlassySidebar) {
            UpwardCurvedGlassSidebar(
                isOpen = isSidebarOpen,
                config = config,
                installedApps = installedApps,
                bottomClearance = if (hasBottomDock) (if (config.showDockLabels) 104.dp else 90.dp) else 16.dp,
                activeNavTab = activeNavTab,
                telemetry = telemetry,
                onToggleOpen = { isSidebarOpen = it },
                onSelectHome = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
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
private fun WidgetOnlyHomePage(
    pageIndex: Int,
    config: LauncherConfig,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    notifications: List<LiveNotification>,
    nowPlaying: NowPlayingInfo,
    installedApps: List<InstalledApp>,
    isNotificationAccessEnabled: Boolean,
    appWidgetManager: VitroAppWidgetManager,
    onCycleWidgetStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenNotification: (LiveNotification) -> Unit,
    onDismissNotification: (String) -> Unit,
    onEnableNotificationAccess: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageScale = config.homePageScale.coerceIn(0.85f, 1.15f)
    val horizontalMargin = ((1.18f - pageScale) * 40f).coerceIn(14f, 28f).dp
    val pagePlacements = remember(config.homeWidgets, pageIndex) {
        config.homeWidgets.filter { it.pageIndex == pageIndex }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalMargin, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Primary Smart Stack / Card Deck showcase on Page 0
        if (pageIndex == 0) {
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

        // Render all user-added widgets assigned to this page (both Vitro Glass & Real Android AppWidgets!)
        pagePlacements.forEach { placement ->
            if (placement.appWidgetId != null) {
                AndroidSystemWidgetCard(
                    appWidgetId = placement.appWidgetId,
                    label = placement.label,
                    height = placement.heightDp.coerceIn(96, 420).dp,
                    glassOpacity = config.widgetGlassOpacity,
                    appWidgetManager = appWidgetManager
                )
            } else if (placement.builtInType != null) {
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
                    onOpenStudio = onOpenStudio
                )
            }
        }
    }
}
