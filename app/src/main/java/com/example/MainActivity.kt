package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
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
import com.example.launcher.system.VitroEdgeControlManager
import com.example.launcher.ui.components.BlackBerryDualLedOverlay
import com.example.launcher.ui.components.QuickPanelOverlay
import com.example.launcher.ui.components.UniversalSearchOverlay
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
    private var pendingGalleryForLockScreen: Boolean = false
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

    // Zero-permission SAF Audio Picker so user can add unlimited custom notification sounds
    private val customAudioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = resolveDisplayName(uri)
                .substringBeforeLast('.')
                .ifBlank { "Audio Kustom" }
            launcherViewModel.addCustomNotificationSound(displayName, uri.toString())
        }
    }

    // Zero-permission Android Photo Picker for custom Home & Lock Screen wallpapers
    private val galleryPhotoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            launcherViewModel.importGalleryWallpaper(uri, pendingGalleryForLockScreen)
        }
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
                    },
                    onRequestAddCustomSound = {
                        runCatching {
                            customAudioPickerLauncher.launch(arrayOf("audio/*"))
                        }
                    },
                    onRequestPickGalleryWallpaper = { isLockScreen ->
                        pendingGalleryForLockScreen = isLockScreen
                        runCatching {
                            galleryPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
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
        VitroEdgeControlManager.onVitroActivityResumed(this)
        launcherViewModel.refreshSyncStatus()
        applyStatusBarVisibility(hideStatusBarPref)
    }

    override fun onPause() {
        VitroEdgeControlManager.onVitroActivityPaused(this)
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        launcherViewModel.appWidgetManager.stopListening()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyStatusBarVisibility(hideStatusBarPref)
    }

    private fun applyStatusBarVisibility(hide: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hide) controller.hide(WindowInsetsCompat.Type.statusBars())
        else controller.show(WindowInsetsCompat.Type.statusBars())
    }

    private fun resolveDisplayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')
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
            proceedToConfigureOrCommitWidget(allocatedId, widget.providerInfo, pendingWidgetLabel, pageIndex)
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
    onRequestAddAndroidWidget: (AvailableAndroidWidget, Int) -> Unit,
    onRequestAddCustomSound: () -> Unit,
    onRequestPickGalleryWallpaper: (Boolean) -> Unit
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

    LaunchedEffect(config.wallpaperConfig.preset) {
        onApplySystemWallpaperMode(config.effectiveHomeWallpaper().preset == WallpaperPreset.SYSTEM_LIVE)
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
        !overlays.isUniversalSearchOpen &&
        (!overlays.isLockScreenActive || config.dynamicIsland.showOnLockScreen)

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Modern Live / Liquid Glass / Gallery / Parallax 3D Wallpaper Engine
        VitroWallpaperBackdrop(
            wallpaperConfig = config.effectiveHomeWallpaper(),
            glowIntensity = config.ambientGlowIntensity
        )

        // Layer 2: Responsive Home Screen with DIY Drag/Resize Widgets, Search Bar, Sidebar & Dock
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
            onOpenStudio = { viewModel.openStudio(startOnWallpaper = false) },
            onOpenWallpaperStudio = { viewModel.openStudio(startOnWallpaper = true) },
            onOpenUniversalSearch = viewModel::openUniversalSearch,
            onEnterLockScreen = viewModel::enterLockScreen,
            onLockPhoneNow = viewModel::lockPhoneOrVitroScreen,
            onDoubleTapLock = viewModel::handleDoubleTapHomeScreen,
            onToggleFlashlight = { viewModel.handleQuickModuleClick(QuickModuleType.FLASHLIGHT) },
            onTriggerHomePopup = { viewModel.triggerSamplePopup(isLockScreen = false) },
            onCycleWidgetStack = viewModel::cycleSmartStack,
            onToggleMedia = viewModel::mediaPlayPause,
            onToggleFocusTimer = viewModel::toggleFocusTimer,
            onResetFocusTimer = viewModel::resetFocusTimer,
            onOpenNotification = { notif -> viewModel.openNotification(notif, context) },
            onDismissNotification = viewModel::dismissLiveNotification,
            onEnableNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(context) },
            onUpdateHomeWidgetLayout = viewModel::updateHomeWidgetLayout,
            onMoveHomeWidgetPage = viewModel::moveHomeWidgetPage,
            onRemoveHomeWidget = viewModel::removeHomeWidget,
            onUpdateSearchBarLayout = viewModel::updateSearchBarLayout
        )

        // Layer 3: Universal Glass Search Overlay (Apps + Widgets + Vitro Menus)
        UniversalSearchOverlay(
            visible = overlays.isUniversalSearchOpen,
            config = config,
            installedApps = installedApps,
            onDismiss = viewModel::closeUniversalSearch,
            onLaunchApp = viewModel::launchApp,
            onOpenStudio = { viewModel.openStudio(startOnWallpaper = false) },
            onOpenQuickPanel = viewModel::openQuickPanel,
            onAddBuiltInWidget = { wt -> viewModel.addBuiltInWidgetToPage(wt, 0) }
        )

        // Layer 4: App Drawer Overlay (uses unified custom Search Bar)
        AppDrawerOverlay(
            visible = overlays.isAppDrawerOpen,
            config = config,
            installedApps = installedApps,
            onDismiss = viewModel::closeAppDrawer,
            onLaunchApp = viewModel::launchApp,
            onToggleDockPin = viewModel::toggleDockPin
        )

        // Layer 5: iPhone-Glass Control Center (direct instant toggles + live Horizontal/Vertical & DIY resize)
        QuickPanelOverlay(
            visible = overlays.isQuickPanelOpen,
            config = config,
            telemetry = telemetry,
            overlays = overlays,
            nowPlaying = nowPlaying,
            feedbackToast = overlays.quickPanelFeedback,
            onDismiss = viewModel::closeQuickPanel,
            onModuleClick = viewModel::handleQuickModuleClick,
            onModuleLongPress = viewModel::handleQuickModuleLongPress,
            onToggleOrientation = viewModel::toggleQuickPanelOrientation,
            onUpdateModuleSize = viewModel::updateQuickModuleSize,
            onMoveModule = viewModel::moveQuickModule,
            onBrightnessChange = viewModel::setBrightness,
            onVolumeChange = viewModel::setVolume,
            onMediaPlayPause = viewModel::mediaPlayPause,
            onMediaNext = viewModel::mediaNext,
            onMediaPrev = viewModel::mediaPrevious
        )

        // Layer 6: Multi-page Lock Screen with Adaptive Clock DIY, Lock Widgets DIY, Notifications & PIN/Password
        LockScreenOverlay(
            visible = overlays.isLockScreenActive,
            config = config,
            telemetry = telemetry,
            overlays = overlays,
            nowPlaying = nowPlaying,
            installedApps = installedApps,
            notifications = notifications,
            isNotificationAccessEnabled = syncStatus.isNotificationListenerEnabled,
            onUnlock = viewModel::unlockScreen,
            onVerifyPasscode = viewModel::verifyLockPasscode,
            onToggleFlashlight = { viewModel.handleQuickModuleClick(QuickModuleType.FLASHLIGHT) },
            onToggleMedia = viewModel::mediaPlayPause,
            onTriggerLockPopup = { viewModel.triggerSamplePopup(isLockScreen = true) },
            onOpenNotification = { notif -> viewModel.openNotification(notif, context) },
            onDismissNotification = viewModel::dismissLiveNotification,
            onClearAllNotifications = viewModel::clearAllNotifications,
            onEnableNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(context) },
            onOpenStudioFromLock = {
                viewModel.unlockScreen()
                viewModel.openStudio()
            },
            onUpdateLockClockLayout = viewModel::updateLockClockLayout,
            onUpdateLockClockStyle = viewModel::updateLockClockStyle,
            onUpdateLockClockColor = viewModel::updateLockClockColor,
            onUpdateLockClockWeight = viewModel::updateLockClockWeight,
            onAddLockWidget = viewModel::addLockWidget,
            onUpdateLockWidgetLayout = viewModel::updateLockWidgetLayout,
            onRemoveLockWidget = viewModel::removeLockWidget
        )

        // Layer 7: Customization Studio Overlay (searchable settings + all 13 tabs including Modern Wallpaper Studio)
        CustomizationStudioOverlay(
            visible = overlays.isStudioOpen,
            startOnWallpaperTab = overlays.openStudioOnWallpaper,
            wallpaperFeedback = overlays.wallpaperFeedback,
            config = config,
            installedApps = installedApps,
            availableAndroidWidgets = availableAndroidWidgets,
            syncStatus = syncStatus,
            onDismiss = viewModel::closeStudio,
            onUpdateConfig = viewModel::updateConfig,
            onUpdateHomeWallpaper = viewModel::updateHomeWallpaper,
            onUpdateLockWallpaper = viewModel::updateLockWallpaper,
            onSetSeparateLockWallpaper = viewModel::setSeparateLockWallpaper,
            onPickGalleryPhoto = onRequestPickGalleryWallpaper,
            onApplySystemWallpaper = viewModel::applyWallpaperToSystem,
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
            onSelectNotifSound = viewModel::selectNotificationSound,
            onPreviewNotifSound = viewModel::previewNotificationSound,
            onRequestAddCustomSound = onRequestAddCustomSound,
            onRemoveCustomSound = viewModel::removeCustomNotificationSound,
            onAddLedAppRule = viewModel::addLedAppRule,
            onUpdateLedAppColor = viewModel::updateLedAppColor,
            onRemoveLedAppRule = viewModel::removeLedAppRule,
            onTestLed = viewModel::triggerLedTest,
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

        // Layer 8: iPhone 18 Pro Max Dynamic Island (dynamically expanding/collapsing)
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
                onOpenNotification = { notif -> viewModel.openNotification(notif, context) },
                onDismissNotification = viewModel::dismissLiveNotification,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Layer 8b: Dual BlackBerry Notification & System Indicator LEDs (Home & Lock Screen)
        if (config.ledConfig.masterEnabled && !overlays.isQuickPanelOpen) {
            BlackBerryDualLedOverlay(
                ledConfig = config.ledConfig,
                islandConfig = config.dynamicIsland,
                telemetry = telemetry,
                notifications = notifications,
                isLockScreen = overlays.isLockScreenActive,
                testLed1Timestamp = overlays.testLed1Timestamp,
                testLed2Timestamp = overlays.testLed2Timestamp,
                onTapAppLedNotification = { notif -> viewModel.openNotification(notif, context) }
            )
        }

        // Layer 9: Topmost Styled Pop-Up Overlay (live notifications & app quick actions)
        val pinnedTarget = overlays.activePopup?.targetApp?.packageName
        VitroPopupOverlay(
            popup = overlays.activePopup,
            config = config,
            isPinnedToDock = pinnedTarget != null && config.pinnedDockPackages.contains(pinnedTarget),
            onDismiss = viewModel::dismissPopup,
            onLaunchApp = viewModel::launchApp,
            onToggleDockPin = viewModel::toggleDockPin,
            onOpenAppInfo = viewModel::openAppInfo,
            onOpenLiveNotification = { notif -> viewModel.openNotification(notif, context) },
            onDismissLiveNotification = viewModel::dismissLiveNotification
        )
    }
}
