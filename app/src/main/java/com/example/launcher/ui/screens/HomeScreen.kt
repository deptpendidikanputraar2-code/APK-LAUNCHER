package com.example.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.HomeNavTab
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import com.example.launcher.ui.components.CompactStatusCapsule
import com.example.launcher.ui.components.TopStatusIndicatorArea
import com.example.launcher.ui.components.UpwardCurvedGlassSidebar
import com.example.launcher.ui.components.VitroDockBar
import com.example.launcher.ui.components.VitroWidgetSection
import com.example.launcher.ui.components.applyPageTransition
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    telemetry: DeviceTelemetry,
    overlays: LauncherUiOverlays,
    activeNavTab: HomeNavTab,
    onLaunchApp: (InstalledApp) -> Unit,
    onLongPressApp: (InstalledApp) -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenStudio: () -> Unit,
    onEnterLockScreen: () -> Unit,
    onTriggerHomePopup: () -> Unit,
    onCycleWidgetStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit
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

    // Upward-expanding Curved-Outside Active Tab Sidebar (starts closed as a sleek aesthetic pill)
    var isSidebarOpen by remember { mutableStateOf(false) }

    val hasBottomDock = config.dockLayout == DockLayoutMode.FLOATING_BOTTOM ||
        config.dockLayout == DockLayoutMode.FULL_EDGE_BOTTOM

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer A: Serene, Minimalist Widget-Only Home Screen Canvas (zero menu clutter!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { verticalSwipeSum = 0f },
                        onVerticalDrag = { _, dragAmount -> verticalSwipeSum += dragAmount },
                        onDragEnd = {
                            if (verticalSwipeSum > 95f) onOpenQuickPanel()
                            else if (verticalSwipeSum < -95f) onOpenAppDrawer()
                            verticalSwipeSum = 0f
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Minimal Status Bar / Dynamic Glass Pill
                TopStatusIndicatorArea(
                    config = config,
                    telemetry = telemetry,
                    onOpenQuickPanel = onOpenQuickPanel,
                    onTriggerPopup = onTriggerHomePopup
                )

                // 2. Pure Widget-Only Multi-Page Home Workspace
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
                                onCycleWidgetStack = onCycleWidgetStack,
                                onToggleMedia = onToggleMedia,
                                onToggleFocusTimer = onToggleFocusTimer,
                                onResetFocusTimer = onResetFocusTimer,
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

                        // Subtle Page Indicator Dots
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

                // Reserved sleek space right above Dock Bar for the closed Aesthetic Sidebar Pill
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

            // Optional Bottom Corner Status Capsule
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

        // Layer B: Upward-Expanding Curved-Outside Active Tab Sidebar anchored cleanly above Dock Bar
        if (config.showGlassySidebar) {
            UpwardCurvedGlassSidebar(
                isOpen = isSidebarOpen,
                sidebarSide = config.sidebarSide,
                bottomClearance = if (hasBottomDock) (if (config.showDockLabels) 104.dp else 90.dp) else 16.dp,
                activeNavTab = activeNavTab,
                glowIntensity = config.ambientGlowIntensity,
                telemetry = telemetry,
                onToggleOpen = { isSidebarOpen = it },
                onSelectHome = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                onCycleWidget = { onCycleWidgetStack(1) },
                onOpenQuickPanel = onOpenQuickPanel,
                onOpenAppDrawer = onOpenAppDrawer,
                onTriggerPopup = onTriggerHomePopup,
                onEnterLockScreen = onEnterLockScreen,
                onOpenStudio = onOpenStudio
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
    onCycleWidgetStack: (Int) -> Unit,
    onToggleMedia: () -> Unit,
    onToggleFocusTimer: () -> Unit,
    onResetFocusTimer: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageScale = config.homePageScale.coerceIn(0.85f, 1.15f)
    val horizontalMargin = ((1.18f - pageScale) * 40f).coerceIn(14f, 28f).dp

    Column(
        modifier = modifier.padding(horizontal = horizontalMargin, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pageIndex == 0) {
            // Primary Home Page: Clean Widget Showcase ONLY (Smart Stack / Card Deck / Separate Grid)
            VitroWidgetSection(
                config = config,
                telemetry = telemetry,
                overlays = overlays,
                onCycleStack = onCycleWidgetStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
                onOpenStudio = onOpenStudio
            )

            // Secondary companion widget card on Page 0 if in Smart Stack / Card Deck mode so Home feels rich yet uncluttered
            if (config.widgetStackMode != WidgetStackMode.GRID_SEPARATE && config.enabledWidgets.size > 1) {
                val secondaryWidget = config.enabledWidgets.getOrNull(
                    (overlays.activeSmartStackIndex + 1) % config.enabledWidgets.size
                ) ?: WidgetType.BATTERY_SYSTEM
                val secondaryConfig = config.copy(
                    widgetStackMode = WidgetStackMode.GRID_SEPARATE,
                    enabledWidgets = listOf(secondaryWidget)
                )
                VitroWidgetSection(
                    config = secondaryConfig,
                    telemetry = telemetry,
                    overlays = overlays,
                    onCycleStack = onCycleWidgetStack,
                    onToggleMedia = onToggleMedia,
                    onToggleFocusTimer = onToggleFocusTimer,
                    onResetFocusTimer = onResetFocusTimer,
                    onOpenStudio = onOpenStudio
                )
            }
        } else {
            // Additional Home Pages: Dedicated Serene Widget Cards
            val allTypes = WidgetType.entries
            val primaryForPage = allTypes[pageIndex % allTypes.size]
            val secondaryForPage = allTypes[(pageIndex + 1) % allTypes.size]
            val pageWidgetConfig = config.copy(
                widgetStackMode = WidgetStackMode.GRID_SEPARATE,
                enabledWidgets = listOf(primaryForPage, secondaryForPage)
            )
            Text(
                text = "Halaman Widget ${pageIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = VitroTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            VitroWidgetSection(
                config = pageWidgetConfig,
                telemetry = telemetry,
                overlays = overlays,
                onCycleStack = onCycleWidgetStack,
                onToggleMedia = onToggleMedia,
                onToggleFocusTimer = onToggleFocusTimer,
                onResetFocusTimer = onResetFocusTimer,
                onOpenStudio = onOpenStudio
            )
        }
    }
}
