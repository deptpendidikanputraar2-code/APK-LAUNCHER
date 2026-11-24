package com.example.launcher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.AppRepository
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.data.SystemMonitor
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.PopupMessage
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.SidebarItemConfig
import com.example.launcher.model.SidebarItemType
import com.example.launcher.model.SystemSyncStatus
import com.example.launcher.model.WidgetType
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.system.NotificationHub
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.system.VitroAccessibilityService
import com.example.launcher.system.VitroAppWidgetManager
import com.example.launcher.system.VitroEdgeControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherUiOverlays(
    val isQuickPanelOpen: Boolean = false,
    val isStudioOpen: Boolean = false,
    val isAppDrawerOpen: Boolean = false,
    val isLockScreenActive: Boolean = false,
    val activePopup: PopupMessage? = null,
    val focusSecondsRemaining: Int = 25 * 60,
    val isFocusRunning: Boolean = false,
    val isMediaPlaying: Boolean = false,
    val activeSmartStackIndex: Int = 0
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefsRepo = LauncherPreferencesRepository(application)
    private val appRepo = AppRepository(application)
    private val systemMonitor = SystemMonitor(application)
    val appWidgetManager = VitroAppWidgetManager(application)

    val config: StateFlow<LauncherConfig> = prefsRepo.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherConfig()
    )

    val telemetry: StateFlow<DeviceTelemetry> = systemMonitor.observeTelemetry().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DeviceTelemetry()
    )

    val liveNotifications: StateFlow<List<LiveNotification>> = NotificationHub.notifications
    val nowPlaying: StateFlow<NowPlayingInfo> = NotificationHub.nowPlaying

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _availableAndroidWidgets = MutableStateFlow<List<AvailableAndroidWidget>>(emptyList())
    val availableAndroidWidgets: StateFlow<List<AvailableAndroidWidget>> = _availableAndroidWidgets.asStateFlow()

    private val _syncStatus = MutableStateFlow(SystemSyncChecker.checkStatus(application))
    val syncStatus: StateFlow<SystemSyncStatus> = _syncStatus.asStateFlow()

    private val _overlays = MutableStateFlow(LauncherUiOverlays())
    val overlays: StateFlow<LauncherUiOverlays> = _overlays.asStateFlow()

    private var popupDismissJob: Job? = null
    private var focusTimerJob: Job? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshInstalledApps()
            refreshAvailableAndroidWidgets()
        }
    }

    init {
        refreshInstalledApps()
        refreshAvailableAndroidWidgets()
        refreshSyncStatus()

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        runCatching { appContext.registerReceiver(packageReceiver, pkgFilter) }

        // Listen to real incoming phone notifications & show styled Pop-Up on Home / Lock Screen
        viewModelScope.launch {
            NotificationHub.incomingEvents.collect { notif ->
                showLiveNotificationPopup(notif)
            }
        }

        // Sync edge control panel trigger whenever config changes
        viewModelScope.launch {
            config.collect { cfg ->
                VitroEdgeControlManager.syncEdgeTrigger(
                    context = appContext,
                    enabled = cfg.replaceSystemControlPanel,
                    zone = cfg.edgeTriggerZone
                )
            }
        }
    }

    override fun onCleared() {
        runCatching { appContext.unregisterReceiver(packageReceiver) }
        super.onCleared()
    }

    fun refreshSyncStatus() {
        _syncStatus.value = SystemSyncChecker.checkStatus(appContext)
        NotificationHub.requestRefresh()
        val cfg = config.value
        VitroEdgeControlManager.syncEdgeTrigger(
            context = appContext,
            enabled = cfg.replaceSystemControlPanel,
            zone = cfg.edgeTriggerZone
        )
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = appRepo.loadInstalledApps()
            _installedApps.value = apps
        }
    }

    fun refreshAvailableAndroidWidgets() {
        viewModelScope.launch {
            val widgets = withContext(Dispatchers.IO) {
                appWidgetManager.loadInstalledAndroidWidgets()
            }
            _availableAndroidWidgets.value = widgets
        }
    }

    fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) {
        val updated = transform(config.value)
        viewModelScope.launch {
            prefsRepo.saveConfig(updated)
        }
    }

    // --- Dock Bar Customization ---
    fun toggleDockPin(packageName: String) {
        updateConfig { current ->
            val currentList = current.pinnedDockPackages.toMutableList()
            if (currentList.contains(packageName)) {
                currentList.remove(packageName)
            } else {
                if (currentList.size >= current.dockMaxIcons) {
                    currentList.removeAt(currentList.lastIndex)
                }
                currentList.add(packageName)
            }
            current.copy(pinnedDockPackages = currentList)
        }
    }

    fun addAppToDock(packageName: String) {
        updateConfig { current ->
            val list = current.pinnedDockPackages.toMutableList()
            if (list.isEmpty()) {
                val fallback = _installedApps.value.take(current.dockMaxIcons).map { it.packageName }
                list.addAll(fallback)
            }
            if (!list.contains(packageName)) {
                if (list.size >= current.dockMaxIcons) {
                    list.removeAt(list.lastIndex)
                }
                list.add(packageName)
            }
            current.copy(pinnedDockPackages = list)
        }
    }

    fun removeAppFromDock(packageName: String) {
        updateConfig { current ->
            val list = current.pinnedDockPackages.toMutableList()
            if (list.isEmpty()) {
                val fallback = _installedApps.value.take(current.dockMaxIcons).map { it.packageName }.toMutableList()
                fallback.remove(packageName)
                return@updateConfig current.copy(pinnedDockPackages = fallback)
            }
            list.remove(packageName)
            current.copy(pinnedDockPackages = list)
        }
    }

    fun moveDockApp(packageName: String, moveLeft: Boolean) {
        updateConfig { current ->
            val list = (if (current.pinnedDockPackages.isEmpty()) {
                _installedApps.value.take(current.dockMaxIcons).map { it.packageName }
            } else current.pinnedDockPackages).toMutableList()
            val idx = list.indexOf(packageName)
            if (idx == -1) return@updateConfig current
            val target = if (moveLeft) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
            if (idx != target) {
                list.removeAt(idx)
                list.add(target, packageName)
            }
            current.copy(pinnedDockPackages = list)
        }
    }

    // --- Widget Customization (Built-in + Real Android AppWidgets) ---
    fun toggleWidgetEnabled(type: WidgetType) {
        updateConfig { current ->
            val list = current.enabledWidgets.toMutableList()
            if (list.contains(type) && list.size > 1) {
                list.remove(type)
            } else if (!list.contains(type)) {
                list.add(type)
            }
            current.copy(enabledWidgets = list)
        }
    }

    fun moveWidgetOrder(type: WidgetType, moveUp: Boolean) {
        updateConfig { current ->
            val list = current.enabledWidgets.toMutableList()
            val idx = list.indexOf(type)
            if (idx == -1) return@updateConfig current
            val targetIdx = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
            if (idx != targetIdx) {
                list.removeAt(idx)
                list.add(targetIdx, type)
            }
            current.copy(enabledWidgets = list)
        }
    }

    fun addBuiltInWidgetToPage(type: WidgetType, pageIndex: Int) {
        updateConfig { current ->
            val newWidget = HomeWidgetPlacement(
                id = "w_${type.name.lowercase()}_${System.currentTimeMillis()}",
                pageIndex = pageIndex.coerceIn(0, current.homePageCount - 1),
                builtInType = type,
                label = type.label,
                heightDp = 148
            )
            val updatedEnabled = if (type in current.enabledWidgets) current.enabledWidgets
            else current.enabledWidgets + type
            current.copy(
                enabledWidgets = updatedEnabled,
                homeWidgets = current.homeWidgets + newWidget
            )
        }
    }

    fun addAndroidWidgetToPage(appWidgetId: Int, label: String, pageIndex: Int) {
        updateConfig { current ->
            val newWidget = HomeWidgetPlacement(
                id = "aw_${appWidgetId}_${System.currentTimeMillis()}",
                pageIndex = pageIndex.coerceIn(0, current.homePageCount - 1),
                builtInType = null,
                appWidgetId = appWidgetId,
                label = label,
                heightDp = 180
            )
            current.copy(homeWidgets = current.homeWidgets + newWidget)
        }
    }

    fun removeHomeWidget(widgetId: String) {
        val target = config.value.homeWidgets.firstOrNull { it.id == widgetId }
        target?.appWidgetId?.let { appWidgetManager.deleteWidgetId(it) }
        updateConfig { current ->
            current.copy(homeWidgets = current.homeWidgets.filterNot { it.id == widgetId })
        }
    }

    fun resizeHomeWidget(widgetId: String, heightDp: Int) {
        updateConfig { current ->
            current.copy(
                homeWidgets = current.homeWidgets.map { w ->
                    if (w.id == widgetId) w.copy(heightDp = heightDp.coerceIn(96, 420)) else w
                }
            )
        }
    }

    fun moveHomeWidgetPage(widgetId: String, pageIndex: Int) {
        updateConfig { current ->
            current.copy(
                homeWidgets = current.homeWidgets.map { w ->
                    if (w.id == widgetId) w.copy(pageIndex = pageIndex.coerceIn(0, current.homePageCount - 1)) else w
                }
            )
        }
    }

    // --- Quick Panel Module Free Customization ---
    fun toggleQuickModule(type: QuickModuleType) {
        updateConfig { current ->
            val updated = current.quickModules.map { mod ->
                if (mod.type == type) mod.copy(enabled = !mod.enabled) else mod
            }
            current.copy(quickModules = updated)
        }
    }

    fun updateQuickModuleSize(type: QuickModuleType, widthPercent: Float, heightDp: Int) {
        updateConfig { current ->
            val updated = current.quickModules.map { mod ->
                if (mod.type == type) {
                    mod.copy(
                        widthPercent = widthPercent.coerceIn(0.23f, 1.0f),
                        heightDp = heightDp.coerceIn(56, 220)
                    )
                } else mod
            }
            current.copy(quickModules = updated)
        }
    }

    fun moveQuickModule(type: QuickModuleType, moveUp: Boolean) {
        updateConfig { current ->
            val list = current.quickModules.toMutableList()
            val idx = list.indexOfFirst { it.type == type }
            if (idx == -1) return@updateConfig current
            val target = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
            if (idx != target) {
                val item = list.removeAt(idx)
                list.add(target, item)
            }
            current.copy(quickModules = list)
        }
    }

    // --- Sidebar Customization ---
    fun toggleSidebarItem(id: String) {
        updateConfig { current ->
            val enabledCount = current.sidebarItems.count { it.enabled }
            val updated = current.sidebarItems.map { item ->
                if (item.id == id) {
                    if (item.enabled && enabledCount <= 1) item else item.copy(enabled = !item.enabled)
                } else item
            }
            current.copy(sidebarItems = updated)
        }
    }

    fun moveSidebarItem(id: String, moveUp: Boolean) {
        updateConfig { current ->
            val list = current.sidebarItems.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx == -1) return@updateConfig current
            val target = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
            if (idx != target) {
                val item = list.removeAt(idx)
                list.add(target, item)
            }
            current.copy(sidebarItems = list)
        }
    }

    fun addAppToSidebar(packageName: String) {
        updateConfig { current ->
            if (current.sidebarItems.any { it.type == SidebarItemType.APP_SHORTCUT && it.packageName == packageName }) {
                return@updateConfig current
            }
            val newItem = SidebarItemConfig(
                id = "app_${packageName.replace('.', '_')}",
                type = SidebarItemType.APP_SHORTCUT,
                packageName = packageName,
                enabled = true
            )
            current.copy(sidebarItems = current.sidebarItems + newItem)
        }
    }

    fun removeSidebarItem(id: String) {
        updateConfig { current ->
            current.copy(sidebarItems = current.sidebarItems.filterNot { it.id == id })
        }
    }

    // --- Lock Screen Security Passcode ---
    fun setLockPasscode(mode: LockSecurityMode, rawPasscode: String) {
        updateConfig { current ->
            if (mode == LockSecurityMode.NONE || rawPasscode.isBlank()) {
                current.copy(
                    lockSecurityMode = LockSecurityMode.NONE,
                    lockPasscodeHash = "",
                    lockPasscodeLength = 4
                )
            } else {
                current.copy(
                    lockSecurityMode = mode,
                    lockPasscodeHash = SystemSyncChecker.hashPasscode(rawPasscode),
                    lockPasscodeLength = rawPasscode.length.coerceIn(4, 6)
                )
            }
        }
    }

    fun verifyLockPasscode(input: String): Boolean {
        val cfg = config.value
        if (cfg.lockSecurityMode == LockSecurityMode.NONE || cfg.lockPasscodeHash.isBlank()) {
            return true
        }
        return SystemSyncChecker.verifyPasscode(input, cfg.lockPasscodeHash)
    }

    // --- Double-Tap & Real Phone Locking ---
    fun handleDoubleTapHomeScreen() {
        if (!config.value.doubleTapToLock) return
        lockPhoneOrVitroScreen()
    }

    fun lockPhoneOrVitroScreen() {
        val lockedRealPhone = VitroAccessibilityService.lockPhoneScreen()
        if (!lockedRealPhone) {
            enterLockScreen()
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            prefsRepo.saveConfig(LauncherConfig())
        }
    }

    // --- Overlays & Navigation ---
    fun openQuickPanel() {
        _overlays.value = _overlays.value.copy(
            isQuickPanelOpen = true,
            isAppDrawerOpen = false,
            isStudioOpen = false
        )
    }

    fun closeQuickPanel() {
        _overlays.value = _overlays.value.copy(isQuickPanelOpen = false)
    }

    fun openStudio() {
        refreshSyncStatus()
        _overlays.value = _overlays.value.copy(
            isStudioOpen = true,
            isQuickPanelOpen = false,
            isAppDrawerOpen = false
        )
    }

    fun closeStudio() {
        _overlays.value = _overlays.value.copy(isStudioOpen = false)
    }

    fun openAppDrawer() {
        _overlays.value = _overlays.value.copy(
            isAppDrawerOpen = true,
            isQuickPanelOpen = false,
            isStudioOpen = false
        )
    }

    fun closeAppDrawer() {
        _overlays.value = _overlays.value.copy(isAppDrawerOpen = false)
    }

    fun enterLockScreen() {
        _overlays.value = _overlays.value.copy(
            isLockScreenActive = true,
            isQuickPanelOpen = false,
            isStudioOpen = false,
            isAppDrawerOpen = false,
            activePopup = null
        )
    }

    fun unlockScreen() {
        _overlays.value = _overlays.value.copy(
            isLockScreenActive = false,
            activePopup = null
        )
    }

    fun cycleSmartStack(delta: Int = 1) {
        val count = config.value.enabledWidgets.size.coerceAtLeast(1)
        val next = (_overlays.value.activeSmartStackIndex + delta + count) % count
        _overlays.value = _overlays.value.copy(activeSmartStackIndex = next)
    }

    // --- Real Notifications & Pop-Ups ---
    fun showLiveNotificationPopup(notif: LiveNotification) {
        popupDismissJob?.cancel()
        val isLock = _overlays.value.isLockScreenActive
        _overlays.value = _overlays.value.copy(
            activePopup = PopupMessage(
                id = notif.timestampMillis,
                title = notif.title,
                body = notif.text,
                badge = notif.appName,
                liveNotification = notif,
                isLockScreenContext = isLock
            )
        )
        popupDismissJob = viewModelScope.launch {
            delay(5_500L)
            dismissPopup()
        }
    }

    fun openNotification(notif: LiveNotification) {
        dismissPopup()
        if (_overlays.value.isLockScreenActive && config.value.lockSecurityMode == LockSecurityMode.NONE) {
            unlockScreen()
        }
        NotificationHub.openNotification(appContext, notif)
    }

    fun dismissLiveNotification(key: String) {
        NotificationHub.dismissNotification(key)
        if (_overlays.value.activePopup?.liveNotification?.key == key) {
            dismissPopup()
        }
    }

    fun clearAllNotifications() {
        NotificationHub.clearAllDismissible()
    }

    fun showAppQuickPopup(app: InstalledApp) {
        popupDismissJob?.cancel()
        _overlays.value = _overlays.value.copy(
            activePopup = PopupMessage(
                title = app.label,
                body = "Pilih tindakan cepat untuk aplikasi ini",
                badge = "Aplikasi",
                targetApp = app,
                isLockScreenContext = false
            )
        )
    }

    fun triggerSamplePopup(isLockScreen: Boolean) {
        popupDismissJob?.cancel()
        val latest = liveNotifications.value.firstOrNull()
        val msg = if (latest != null) {
            PopupMessage(
                title = latest.title,
                body = latest.text,
                badge = latest.appName,
                liveNotification = latest,
                isLockScreenContext = isLockScreen
            )
        } else if (isLockScreen) {
            PopupMessage(
                title = "Notifikasi Layar Kunci",
                body = "Gaya ${config.value.lockPopupStyle.label} siap menampilkan notifikasi nyata HP Anda.",
                badge = "Layar Kunci",
                isLockScreenContext = true
            )
        } else {
            PopupMessage(
                title = "Notifikasi Beranda",
                body = "Gaya ${config.value.homePopupStyle.label} aktif. Aktifkan Akses Notifikasi untuk pesan nyata HP.",
                badge = "Beranda",
                isLockScreenContext = false
            )
        }
        _overlays.value = _overlays.value.copy(activePopup = msg)
        popupDismissJob = viewModelScope.launch {
            delay(5_500L)
            dismissPopup()
        }
    }

    fun dismissPopup() {
        popupDismissJob?.cancel()
        _overlays.value = _overlays.value.copy(activePopup = null)
    }

    // --- Real Hardware & App Controls ---
    fun launchApp(app: InstalledApp) {
        dismissPopup()
        closeAppDrawer()
        appRepo.launchApp(app)
    }

    fun launchAppByPackage(packageName: String) {
        val app = _installedApps.value.firstOrNull { it.packageName == packageName }
        if (app != null) {
            launchApp(app)
        } else {
            runCatching {
                val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(intent)
                }
            }
        }
    }

    fun openAppInfo(app: InstalledApp) {
        dismissPopup()
        appRepo.openAppInfo(app)
    }

    fun setVolume(fraction: Float) = systemMonitor.setMediaVolume(fraction)

    fun setBrightness(fraction: Float) = systemMonitor.setBrightnessFraction(fraction)

    fun mediaPlayPause() {
        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        _overlays.value = _overlays.value.copy(isMediaPlaying = !_overlays.value.isMediaPlaying)
    }

    fun mediaNext() {
        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun mediaPrevious() {
        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun handleQuickModuleClick(type: QuickModuleType) {
        when (type) {
            QuickModuleType.WIFI -> systemMonitor.openWifiPanel()
            QuickModuleType.CELLULAR -> systemMonitor.openSystemSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)
            QuickModuleType.BLUETOOTH -> systemMonitor.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            QuickModuleType.FLASHLIGHT -> systemMonitor.toggleFlashlight()
            QuickModuleType.DND -> systemMonitor.toggleDndReal()
            QuickModuleType.ROTATION -> systemMonitor.toggleAutoRotateReal()
            QuickModuleType.AIRPLANE -> systemMonitor.openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            QuickModuleType.HOTSPOT -> systemMonitor.openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS)
            QuickModuleType.LOCATION -> systemMonitor.openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            QuickModuleType.BATTERY_SAVER -> systemMonitor.openSystemSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            QuickModuleType.SCREENSHOT -> {
                closeQuickPanel()
                viewModelScope.launch {
                    delay(280L)
                    if (!VitroAccessibilityService.takeScreenshot()) {
                        SystemSyncChecker.openAccessibilitySettings(appContext)
                    }
                }
            }
            QuickModuleType.CAMERA -> {
                closeQuickPanel()
                systemMonitor.openCameraApp()
            }
            QuickModuleType.LOCK_SCREEN -> {
                closeQuickPanel()
                lockPhoneOrVitroScreen()
            }
        }
    }

    fun toggleFocusTimer() {
        val nextRunning = !_overlays.value.isFocusRunning
        _overlays.value = _overlays.value.copy(isFocusRunning = nextRunning)
        focusTimerJob?.cancel()
        if (nextRunning) {
            focusTimerJob = viewModelScope.launch {
                while (_overlays.value.isFocusRunning && _overlays.value.focusSecondsRemaining > 0) {
                    delay(1_000L)
                    _overlays.value = _overlays.value.copy(
                        focusSecondsRemaining = (_overlays.value.focusSecondsRemaining - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun resetFocusTimer() {
        focusTimerJob?.cancel()
        _overlays.value = _overlays.value.copy(
            isFocusRunning = false,
            focusSecondsRemaining = 25 * 60
        )
    }
}
