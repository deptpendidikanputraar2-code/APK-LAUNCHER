package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.model.HomeNavTab
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.ui.components.QuickPanelOverlay
import com.example.launcher.ui.components.VitroDynamicIsland
import com.example.launcher.ui.components.VitroPopupOverlay
import com.example.launcher.ui.components.VitroWallpaperBackdrop
import com.example.launcher.ui.screens.AppDrawerOverlay
import com.example.launcher.ui.screens.CustomizationStudioOverlay
import com.example.launcher.ui.screens.HomeScreen
import com.example.launcher.ui.screens.LockScreenOverlay
import com.example.launcher.viewmodel.LauncherViewModel
import com.example.ui.theme.VitroLauncherTheme

class MainActivity : ComponentActivity() {

    private val launcherViewModel: LauncherViewModel by viewModels()

    private var hideStatusBarPref: Boolean = true
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetLabel: String = ""
    private var pendingWidgetPage: Int = 0
    private var pendingWidgetProvider: AppWidgetProviderInfo? = null

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        val provider = pendingWidgetProvider
        val label = pendingWidgetLabel
        val page = pendingWidgetPage
        if (result.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            proceedToConfigureOrCommitWidget(widgetId, provider, label, page)
        } else {
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                launcherViewModel.appWidgetManager.deleteWidgetId(widgetId)
            }
            clearPendingWidget()
        }
    }

    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        val label = pendingWidgetLabel
        val page = pendingWidgetPage
        if (result.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            launcherViewModel.addAndroidWidgetToPage(widgetId, label, page)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            launcherViewModel.appWidgetManager.deleteWidgetId(widgetId)
        }
        clearPendingWidget()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            VitroLauncherTheme {
                VitroLauncherRoot(
                    viewModel = launcherViewModel,
                    onApplyStatusBarMode = { hide ->
                        hideStatusBarPref = hide
                        applyStatusBarVisibility(hide)
                    },
                    onApplySystemWallpaperMode = { useSystemLive ->
                        if (useSystemLive) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                        }
                    },
                    onApplyWindowBrightness = { brightness ->
                        val params = window.attributes
                        params.screenBrightness = brightness.coerceIn(0.08f, 1.0f)
                        window.attributes = params
                    },
                    onRequestAddAndroidWidget = { widget, pageIndex ->
                        startAddAndroidWidgetFlow(widget, pageIndex)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        launcherViewModel.appWidgetManager.startListening()
    }

    override fun onResume() {
        super.onResume()
        launcherViewModel.refreshSyncStatus()
        applyStatusBarVisibility(hideStatusBarPref)
    }

    override fun onStop() {
        super.onStop()
        launcherViewModel.appWidgetManager.stopListening()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyStatusBarVisibility(hideStatusBarPref)
        }
    }

    private fun applyStatusBarVisibility(hide: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hide) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    private fun startAddAndroidWidgetFlow(widget: AvailableAndroidWidget, pageIndex: Int) {
        val allocatedId = runCatching {
            launcherViewModel.appWidgetManager.allocateWidgetId()
        }.getOrElse { return }

        pendingWidgetId = allocatedId
        pendingWidgetLabel = "${widget.appName} • ${widget.widgetLabel}"
        pendingWidgetPage = pageIndex
        pendingWidgetProvider = widget.providerInfo

        val bound = launcherViewModel.appWidgetManager.bindIfAllowed(allocatedId, widget.providerInfo)
        if (bound) {
            proceedToConfigureOrCommitWidget(
                allocatedId,
                widget.providerInfo,
                pendingWidgetLabel,
                pageIndex
            )
        } else {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widget.providerInfo.provider)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, widget.providerInfo.profile)
                }
            }
            runCatching {
                bindWidgetLauncher.launch(bindIntent)
            }.onFailure {
                launcherViewModel.appWidgetManager.deleteWidgetId(allocatedId)
                clearPendingWidget()
            }
        }
    }

    private fun proceedToConfigureOrCommitWidget(
        widgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
        label: String,
        pageIndex: Int
    ) {
        val configureComponent = providerInfo?.configure
        if (configureComponent != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configureComponent
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val launched = runCatching {
                configureWidgetLauncher.launch(configureIntent)
                true
            }.getOrDefault(false)
            if (launched) return
        }
        launcherViewModel.addAndroidWidgetToPage(widgetId, label, pageIndex)
        clearPendingWidget()
    }

    private fun clearPendingWidget() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetLabel = ""
        pendingWidgetPage = 0
        pendingWidgetProvider = null
    }
}

@Composable
private fun VitroLauncherRoot(
    viewModel: LauncherViewModel,
    onApplyStatusBarMode: (Boolean) -> Unit,
    onApplySystemWallpaperMode: (Boolean) -> Unit,
    onApplyWindowBrightness: (Float) -> Unit,
    onRequestAddAndroidWidget: (AvailableAndroidWidget, Int) -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val availableAndroidWidgets by viewModel.availableAndroidWidgets.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()
    val notifications by viewModel.liveNotifications.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    LaunchedEffect(config.hideSystemStatusBar) {
        onApplyStatusBarMode(config.hideSystemStatusBar)
    }

    LaunchedEffect(config.wallpaperPreset) {
        onApplySystemWallpaperMode(config.wallpaperPreset == WallpaperPreset.SYSTEM_LIVE)
    }

    LaunchedEffect(telemetry.brightnessFraction) {
        onApplyWindowBrightness(telemetry.brightnessFraction)
    }

    val activeNavTab = when {
        overlays.isStudioOpen -> HomeNavTab.STUDIO
        overlays.isLockScreenActive -> HomeNavTab.LOCK
        overlays.isQuickPanelOpen -> HomeNavTab.PANEL
        overlays.isAppDrawerOpen -> HomeNavTab.APPS
        else -> HomeNavTab.HOME
    }

    val showDynamicIsland = config.dynamicIsland.enabled &&
        !overlays.isStudioOpen &&
        !overlays.isQuickPanelOpen &&
        !overlays.isAppDrawerOpen &&
        (!overlays.isLockScreenActive || config.dynamicIsland.showOnLockScreen)

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: High-res frosted glass wallpaper or real live phone wallpaper backdrop
        VitroWallpaperBackdrop(
            preset = config.wallpaperPreset,
            glowIntensity = config.ambientGlowIntensity
        )

        // Layer 2: Responsive Home Screen with customizable Sidebar, Dock, Glass & Android Widgets
        HomeScreen(
            config = config,
            installedApps = installedApps,
            telemetry = telemetry,
            overlays = overlays,
            notifications = notifications,
            nowPlaying = nowPlaying,
            isNotificationAccessEnabled = syncStatus.isNotificationListenerEnabled,
            appWidgetManager = viewModel.appWidgetManager,
            activeNavTab = activeNavTab,
            onLaunchApp = viewModel::launchApp,
            onLaunchAppPackage = viewModel::launchAppByPackage,
            onLongPressApp = viewModel::showAppQuickPopup,
            onOpenQuickPanel = viewModel::openQuickPanel,
            onOpenAppDrawer = viewModel::openAppDrawer,
            onOpenStudio = viewModel::openStudio,
            onEnterLockScreen = viewModel::enterLockScreen,
            onLockPhoneNow = viewModel::lockPhoneOrVitroScreen,
            onDoubleTapLock = viewModel::handleDoubleTapHomeScreen,
            onToggleFlashlight = { viewModel.handleQuickModuleClick(QuickModuleType.FLASHLIGHT) },
            onTriggerHomePopup = { viewModel.triggerSamplePopup(isLockScreen = false) },
            onCycleWidgetStack = viewModel::cycleSmartStack,
            onToggleMedia = viewModel::mediaPlayPause,
            onToggleFocusTimer = viewModel::toggleFocusTimer,
            onResetFocusTimer = viewModel::resetFocusTimer,
            onOpenNotification = viewModel::openNotification,
            onDismissNotification = viewModel::dismissLiveNotification,
            onEnableNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(context) }
        )

        // Layer 3: App Drawer Overlay
        AppDrawerOverlay(
            visible = overlays.isAppDrawerOpen,
            config = config,
            installedApps = installedApps,
            onDismiss = viewModel::closeAppDrawer,
            onLaunchApp = viewModel::launchApp,
            onToggleDockPin = viewModel::toggleDockPin
        )

        // Layer 4: Pure iPhone-Glass Control Center (ZERO settings inside)
        QuickPanelOverlay(
            visible = overlays.isQuickPanelOpen,
            config = config,
            telemetry = telemetry,
            overlays = overlays,
            nowPlaying = nowPlaying,
            onDismiss = viewModel::closeQuickPanel,
            onModuleClick = viewModel::handleQuickModuleClick,
            onBrightnessChange = viewModel::setBrightness,
            onVolumeChange = viewModel::setVolume,
            onMediaPlayPause = viewModel::mediaPlayPause,
            onMediaNext = viewModel::mediaNext,
            onMediaPrev = viewModel::mediaPrevious
        )

        // Layer 5: Multi-page Lock Screen with Real Notifications & PIN / Password Security
        LockScreenOverlay(
            visible = overlays.isLockScreenActive,
            config = config,
            telemetry = telemetry,
            notifications = notifications,
            isNotificationAccessEnabled = syncStatus.isNotificationListenerEnabled,
            onUnlock = viewModel::unlockScreen,
            onVerifyPasscode = viewModel::verifyLockPasscode,
            onToggleFlashlight = { viewModel.handleQuickModuleClick(QuickModuleType.FLASHLIGHT) },
            onTriggerLockPopup = { viewModel.triggerSamplePopup(isLockScreen = true) },
            onOpenNotification = viewModel::openNotification,
            onDismissNotification = viewModel::dismissLiveNotification,
            onClearAllNotifications = viewModel::clearAllNotifications,
            onEnableNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(context) },
            onOpenStudioFromLock = {
                viewModel.unlockScreen()
                viewModel.openStudio()
            }
        )

        // Layer 6: Customization Studio Overlay (ALL settings live exclusively here)
        CustomizationStudioOverlay(
            visible = overlays.isStudioOpen,
            config = config,
            installedApps = installedApps,
            availableAndroidWidgets = availableAndroidWidgets,
            syncStatus = syncStatus,
            onDismiss = viewModel::closeStudio,
            onUpdateConfig = viewModel::updateConfig,
            onRefreshSyncStatus = viewModel::refreshSyncStatus,
            onAddDockApp = viewModel::addAppToDock,
            onRemoveDockApp = viewModel::removeAppFromDock,
            onMoveDockApp = viewModel::moveDockApp,
            onToggleWidget = viewModel::toggleWidgetEnabled,
            onMoveWidget = viewModel::moveWidgetOrder,
            onAddBuiltInWidget = viewModel::addBuiltInWidgetToPage,
            onPickAndroidWidget = { widget, pageIndex ->
                onRequestAddAndroidWidget(widget, pageIndex)
            },
            onRemoveHomeWidget = viewModel::removeHomeWidget,
            onResizeHomeWidget = viewModel::resizeHomeWidget,
            onToggleQuickModule = viewModel::toggleQuickModule,
            onUpdateQuickModuleSize = viewModel::updateQuickModuleSize,
            onMoveQuickModule = viewModel::moveQuickModule,
            onToggleSidebarItem = viewModel::toggleSidebarItem,
            onMoveSidebarItem = viewModel::moveSidebarItem,
            onAddAppToSidebar = viewModel::addAppToSidebar,
            onRemoveSidebarItem = viewModel::removeSidebarItem,
            onSetLockPasscode = viewModel::setLockPasscode,
            onPreviewQuickPanel = viewModel::openQuickPanel,
            onPreviewHomePopup = {
                viewModel.closeStudio()
                viewModel.triggerSamplePopup(isLockScreen = false)
            },
            onPreviewLockPopup = {
                viewModel.enterLockScreen()
                viewModel.triggerSamplePopup(isLockScreen = true)
            },
            onPreviewLockScreen = viewModel::enterLockScreen,
            onOpenDefaultLauncher = { SystemSyncChecker.openDefaultLauncherSettings(context) },
            onOpenNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(context) },
            onOpenAccessibility = { SystemSyncChecker.openAccessibilitySettings(context) },
            onOpenOverlayPermission = { SystemSyncChecker.openOverlaySettings(context) },
            onOpenWriteSettings = { SystemSyncChecker.openWriteSettings(context) },
            onOpenDndPolicy = { SystemSyncChecker.openDndPolicySettings(context) },
            onResetDefaults = viewModel::resetDefaults
        )

        // Layer 7: Fully Customizable Interactive Dynamic Island
        if (showDynamicIsland) {
            VitroDynamicIsland(
                islandConfig = config.dynamicIsland,
                telemetry = telemetry,
                overlays = overlays,
                notifications = notifications,
                nowPlaying = nowPlaying,
                onMediaPlayPause = viewModel::mediaPlayPause,
                onMediaNext = viewModel::mediaNext,
                onMediaPrev = viewModel::mediaPrevious,
                onToggleFlashlight = { viewModel.handleQuickModuleClick(QuickModuleType.FLASHLIGHT) },
                onOpenQuickPanel = viewModel::openQuickPanel,
                onOpenNotification = viewModel::openNotification,
                onDismissNotification = viewModel::dismissLiveNotification,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Layer 8: Topmost Styled Pop-Up Overlay (live notifications & app quick actions)
        val pinnedTarget = overlays.activePopup?.targetApp?.packageName
        VitroPopupOverlay(
            popup = overlays.activePopup,
            config = config,
            isPinnedToDock = pinnedTarget != null && config.pinnedDockPackages.contains(pinnedTarget),
            onDismiss = viewModel::dismissPopup,
            onLaunchApp = viewModel::launchApp,
            onToggleDockPin = viewModel::toggleDockPin,
            onOpenAppInfo = viewModel::openAppInfo,
            onOpenLiveNotification = viewModel::openNotification,
            onDismissLiveNotification = viewModel::dismissLiveNotification
        )
    }
}
