package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.SystemSyncStatus
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WidgetType
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.system.SystemWallpaperTarget
import com.example.launcher.ui.components.VitroCustomizableSearchBar
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

private enum class StudioCategory(val label: String, val keywords: String, val icon: ImageVector) {
    SYNC("Sinkron HP", "izin aksesibilitas notifikasi overlay default launcher", Icons.Rounded.PhoneAndroid),
    WALLPAPER("Wallpaper", "wallpaper live animasi glass kaca blur galeri foto bokeh paralaks 3d", Icons.Rounded.Wallpaper),
    SEARCH("Pencarian", "search cari bentuk warna posisi diy", Icons.Rounded.Search),
    DYNAMIC_ISLAND("Dynamic Island", "iphone 18 pro max musik notifikasi gelembung", Icons.Rounded.AutoAwesome),
    POPUP("Notif & Suara", "suara notifikasi audio popup ganti bawaan", Icons.Rounded.NotificationsActive),
    PAGES("Layar Kunci & Jam", "adaptive clock sandi pin ketuk 2x diy", Icons.Rounded.ViewCarousel),
    WIDGETS("Widget", "widget kaca analog dunia cuaca musik android diy", Icons.Rounded.DashboardCustomize),
    QUICK_PANEL("Quick Panel", "kontrol modul ukuran horizontal vertikal", Icons.Rounded.Tune),
    DOCK("Dock Bar", "dock aplikasi semat ikon", Icons.Rounded.Dock),
    SIDEBAR("Sidebar", "sidebar kiri kanan aplikasi", Icons.Rounded.ViewSidebar),
    STATUS("Indikator", "status bar baterai jam sinyal", Icons.Rounded.SignalCellularAlt),
    TRANSITION("Transisi", "animasi kubus geser kecepatan", Icons.Rounded.Animation),
    THEME("Tema", "tema glow default reset launcher", Icons.Rounded.Palette)
}

@Composable
fun CustomizationStudioOverlay(
    visible: Boolean,
    startOnWallpaperTab: Boolean = false,
    wallpaperFeedback: String? = null,
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    availableAndroidWidgets: List<AvailableAndroidWidget>,
    syncStatus: SystemSyncStatus,
    onDismiss: () -> Unit,
    onUpdateConfig: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onUpdateHomeWallpaper: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onUpdateLockWallpaper: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onSetSeparateLockWallpaper: (Boolean) -> Unit,
    onPickGalleryPhoto: (Boolean) -> Unit,
    onApplySystemWallpaper: (WallpaperConfig, SystemWallpaperTarget) -> Unit,
    onRefreshSyncStatus: () -> Unit,
    onAddDockApp: (String) -> Unit,
    onRemoveDockApp: (String) -> Unit,
    onMoveDockApp: (String, Boolean) -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onMoveWidget: (WidgetType, Boolean) -> Unit,
    onAddBuiltInWidget: (WidgetType, Int) -> Unit,
    onPickAndroidWidget: (AvailableAndroidWidget, Int) -> Unit,
    onRemoveHomeWidget: (String) -> Unit,
    onResizeHomeWidget: (String, Int) -> Unit,
    onToggleQuickModule: (QuickModuleType) -> Unit,
    onUpdateQuickModuleSize: (QuickModuleType, Float, Int) -> Unit,
    onMoveQuickModule: (QuickModuleType, Boolean) -> Unit,
    onToggleSidebarItem: (String) -> Unit,
    onMoveSidebarItem: (String, Boolean) -> Unit,
    onAddAppToSidebar: (String) -> Unit,
    onRemoveSidebarItem: (String) -> Unit,
    onSetLockPasscode: (LockSecurityMode, String) -> Unit,
    onSelectNotifSound: (String) -> Unit,
    onPreviewNotifSound: (String) -> Unit,
    onRequestAddCustomSound: () -> Unit,
    onRemoveCustomSound: (String) -> Unit,
    onPreviewQuickPanel: () -> Unit,
    onPreviewHomePopup: () -> Unit,
    onPreviewLockPopup: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onOpenDndPolicy: () -> Unit,
    onResetDefaults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(StudioCategory.SYNC) }
    var studioSearchQuery by remember(visible) { mutableStateOf("") }

    LaunchedEffect(visible, startOnWallpaperTab) {
        if (visible && startOnWallpaperTab) {
            selectedTab = StudioCategory.WALLPAPER
        }
    }

    val filteredTabs = remember(studioSearchQuery) {
        val q = studioSearchQuery.trim()
        if (q.isBlank()) StudioCategory.entries
        else StudioCategory.entries.filter {
            it.label.contains(q, ignoreCase = true) || it.keywords.contains(q, ignoreCase = true)
        }.ifEmpty { StudioCategory.entries }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            spring(
                dampingRatio = config.transitionSpeed.dampingRatio,
                stiffness = config.transitionSpeed.stiffness
            )
        ) { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 }
    ) {
        BackHandler(onBack = onDismiss)

        val safeTop = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 6.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEB060B18))
                .then(safeTop)
                .navigationBarsPadding()
                .testTag("customization_studio_overlay"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = VitroTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.studio_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = VitroTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("studio_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = VitroTextPrimary
                        )
                    }
                }

                // Unified customizable Search Bar right inside Studio Vitro!
                VitroCustomizableSearchBar(
                    searchConfig = config.searchBar,
                    query = studioSearchQuery,
                    onQueryChange = { q ->
                        studioSearchQuery = q
                        val trimmed = q.trim()
                        if (trimmed.isNotBlank()) {
                            StudioCategory.entries.firstOrNull {
                                it.label.contains(trimmed, ignoreCase = true) ||
                                    it.keywords.contains(trimmed, ignoreCase = true)
                            }?.let { selectedTab = it }
                        }
                    },
                    placeholder = "Cari menu pengaturan (mis: Dynamic Island, Suara, Jam)...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredTabs) { tab ->
                        val isSelected = selectedTab == tab
                        VitroGlassContainer(
                            cornerRadius = 50.dp,
                            tintColor = if (isSelected) VitroCyanAccent else Color.White,
                            glassOpacity = if (isSelected) 0.36f else 0.16f,
                            borderColor = if (isSelected) VitroCyanAccent else Color.White.copy(alpha = 0.28f),
                            modifier = Modifier
                                .testTag("studio_tab_${tab.name.lowercase()}")
                                .bouncyClickable { selectedTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else VitroCyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VitroTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 28.dp)
                ) {
                    when (selectedTab) {
                        StudioCategory.SYNC -> SystemSyncStudioTab(
                            syncStatus = syncStatus,
                            onRefreshStatus = onRefreshSyncStatus,
                            onOpenDefaultLauncher = onOpenDefaultLauncher,
                            onOpenNotificationAccess = onOpenNotificationAccess,
                            onOpenAccessibility = onOpenAccessibility,
                            onOpenOverlayPermission = onOpenOverlayPermission,
                            onOpenWriteSettings = onOpenWriteSettings,
                            onOpenDndPolicy = onOpenDndPolicy
                        )
                        StudioCategory.WALLPAPER -> WallpaperStudioTab(
                            config = config,
                            wallpaperFeedback = wallpaperFeedback,
                            onUpdateHomeWallpaper = onUpdateHomeWallpaper,
                            onUpdateLockWallpaper = onUpdateLockWallpaper,
                            onSetSeparateLockWallpaper = onSetSeparateLockWallpaper,
                            onUpdateAmbientGlow = { v -> onUpdateConfig { it.copy(ambientGlowIntensity = v) } },
                            onPickGalleryPhoto = onPickGalleryPhoto,
                            onApplyToSystemWallpaper = onApplySystemWallpaper
                        )
                        StudioCategory.SEARCH -> SearchBarStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.DOCK -> DockStudioTab(
                            config = config,
                            installedApps = installedApps,
                            onUpdate = onUpdateConfig,
                            onAddDockApp = onAddDockApp,
                            onRemoveDockApp = onRemoveDockApp,
                            onMoveDockApp = onMoveDockApp
                        )
                        StudioCategory.WIDGETS -> WidgetStudioTab(
                            config = config,
                            availableAndroidWidgets = availableAndroidWidgets,
                            onUpdate = onUpdateConfig,
                            onToggleWidget = onToggleWidget,
                            onMoveWidget = onMoveWidget,
                            onAddBuiltInWidget = onAddBuiltInWidget,
                            onPickAndroidWidget = onPickAndroidWidget,
                            onRemoveHomeWidget = onRemoveHomeWidget,
                            onResizeHomeWidget = onResizeHomeWidget
                        )
                        StudioCategory.QUICK_PANEL -> QuickPanelStudioTab(
                            config = config,
                            isOverlayGranted = syncStatus.isOverlayPermissionGranted,
                            onUpdate = onUpdateConfig,
                            onToggleModule = onToggleQuickModule,
                            onUpdateModuleSize = onUpdateQuickModuleSize,
                            onMoveModule = onMoveQuickModule,
                            onPreviewQuickPanel = onPreviewQuickPanel,
                            onOpenOverlayPermission = onOpenOverlayPermission
                        )
                        StudioCategory.DYNAMIC_ISLAND -> DynamicIslandStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.SIDEBAR -> SidebarStudioTab(
                            config = config,
                            installedApps = installedApps,
                            onUpdate = onUpdateConfig,
                            onToggleSidebarItem = onToggleSidebarItem,
                            onMoveSidebarItem = onMoveSidebarItem,
                            onAddAppToSidebar = onAddAppToSidebar,
                            onRemoveSidebarItem = onRemoveSidebarItem
                        )
                        StudioCategory.STATUS -> StatusIndicatorStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.POPUP -> PopupStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onSelectSound = onSelectNotifSound,
                            onPreviewSound = onPreviewNotifSound,
                            onRequestAddCustomSound = onRequestAddCustomSound,
                            onRemoveCustomSound = onRemoveCustomSound,
                            onPreviewHomePopup = onPreviewHomePopup,
                            onPreviewLockPopup = onPreviewLockPopup
                        )
                        StudioCategory.PAGES -> PagesStudioTab(
                            config = config,
                            isAccessibilityGranted = syncStatus.isAccessibilityEnabled,
                            onUpdate = onUpdateConfig,
                            onSetLockPasscode = onSetLockPasscode,
                            onOpenAccessibilitySettings = onOpenAccessibility,
                            onPreviewLockScreen = onPreviewLockScreen
                        )
                        StudioCategory.TRANSITION -> TransitionStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.THEME -> ThemeWallpaperStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onOpenWallpaperStudio = { selectedTab = StudioCategory.WALLPAPER },
                            onSetDefaultLauncher = onOpenDefaultLauncher,
                            onResetDefaults = onResetDefaults
                        )
                    }
                }
            }
        }
    }
}
