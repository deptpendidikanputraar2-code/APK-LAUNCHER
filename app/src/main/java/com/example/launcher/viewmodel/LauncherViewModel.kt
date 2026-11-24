package com.example.launcher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.AppRepository
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.data.SystemMonitor
import com.example.launcher.model.BuiltInNotifSound
import com.example.launcher.model.CustomNotificationSound
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LiveNotification
import com.example.launcher.model.LockClockColor
import com.example.launcher.model.LockClockStyle
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.NowPlayingInfo
import com.example.launcher.model.PopupMessage
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.model.SidebarItemConfig
import com.example.launcher.model.SidebarItemType
import com.example.launcher.model.SystemSyncStatus
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.model.WidgetType
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.system.NotificationHub
import com.example.launcher.system.NotificationSoundEngine
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.system.SystemWallpaperTarget
import com.example.launcher.system.VitroAccessibilityService
import com.example.launcher.system.VitroAppWidgetManager
import com.example.launcher.system.VitroEdgeControlManager
import com.example.launcher.system.WallpaperStorageManager
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
    val openStudioOnWallpaper: Boolean = false,
    val isAppDrawerOpen: Boolean = false,
    val isUniversalSearchOpen: Boolean = false,
    val isLockScreenActive: Boolean = false,
    val activePopup: PopupMessage? = null,
    val quickPanelFeedback: String? = null,
    val wallpaperFeedback: String? = null,
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
    private var feedbackDismissJob: Job? = null
    private var wallpaperFeedbackJob: Job? = null
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
            _installedApps.value = appRepo.loadInstalledApps()
        }
    }

    fun refreshAvailableAndroidWidgets() {
        viewModelScope.launch {
            _availableAndroidWidgets.value = withContext(Dispatchers.IO) {
                appWidgetManager.loadInstalledAndroidWidgets()
            }
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
                list.addAll(_installedApps.value.take(current.dockMaxIcons).map { it.packageName })
            }
            if (!list.contains(packageName)) {
                if (list.size >= current.dockMaxIcons) list.removeAt(list.lastIndex)
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

    // --- Widget Customization & DIY Layout (Home + Lock Screen) ---
    fun toggleWidgetEnabled(type: WidgetType) {
        updateConfig { current ->
            val list = current.enabledWidgets.toMutableList()
            if (list.contains(type) && list.size > 1) list.remove(type)
            else if (!list.contains(type)) list.add(type)
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

    fun addBuiltInWidgetToPage(type: WidgetType, pageIndex: Int = 0) {
        updateConfig { current ->
            val newWidget = HomeWidgetPlacement(
                id = "w_${type.name.lowercase()}_${System.currentTimeMillis()}",
                pageIndex = pageIndex.coerceIn(0, current.homePageCount - 1),
                builtInType = type,
                label = type.label,
                widthPercent = 1.0f,
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
                widthPercent = 1.0f,
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
                    if (w.id == widgetId) w.copy(heightDp = heightDp.coerceIn(72, 420)) else w
                }
            )
        }
    }

    fun updateHomeWidgetLayout(
        widgetId: String,
        widthPercent: Float,
        heightDp: Int,
        xOffsetDp: Float,
        yOffsetDp: Float
    ) {
        updateConfig { current ->
            current.copy(
                homeWidgets = current.homeWidgets.map { w ->
                    if (w.id == widgetId) w.copy(
                        widthPercent = widthPercent.coerceIn(0.44f, 1.0f),
                        heightDp = heightDp.coerceIn(72, 420),
                        xOffsetDp = xOffsetDp.coerceIn(-120f, 120f),
                        yOffsetDp = yOffsetDp.coerceIn(-120f, 240f)
                    ) else w
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

    // --- Home Search Bar DIY Layout ---
    fun updateSearchBarLayout(
        widthPercent: Float,
        heightDp: Int,
        xOffsetDp: Float,
        yOffsetDp: Float
    ) {
        updateConfig { current ->
            current.copy(
                searchBar = current.searchBar.copy(
                    widthPercent = widthPercent.coerceIn(0.50f, 1.0f),
                    heightDp = heightDp.coerceIn(40, 68),
                    xOffsetDp = xOffsetDp.coerceIn(-120f, 120f),
                    yOffsetDp = yOffsetDp.coerceIn(-120f, 360f)
                )
            )
        }
    }

    // --- Lock Screen Adaptive Clock & Lock Widget DIY ---
    fun updateLockClockLayout(scale: Float, verticalOffsetDp: Float) {
        updateConfig { current ->
            current.copy(
                lockClockScale = scale.coerceIn(0.55f, 1.85f),
                lockClockVerticalOffsetDp = verticalOffsetDp.coerceIn(-60f, 160f)
            )
        }
    }

    fun updateLockClockStyle(style: LockClockStyle) {
        updateConfig { it.copy(lockClockStyle = style) }
    }

    fun updateLockClockColor(color: LockClockColor) {
        updateConfig { it.copy(lockClockColor = color) }
    }

    fun updateLockClockWeight(weight: Int) {
        updateConfig { it.copy(lockClockWeight = weight.coerceIn(300, 900)) }
    }

    fun addLockWidget(type: WidgetType) {
        updateConfig { current ->
            val widget = HomeWidgetPlacement(
                id = "lw_${type.name.lowercase()}_${System.currentTimeMillis()}",
                pageIndex = 0,
                builtInType = type,
                label = type.label,
                widthPercent = 0.48f,
                heightDp = 112
            )
            current.copy(lockWidgets = current.lockWidgets + widget)
        }
    }

    fun updateLockWidgetLayout(
        widgetId: String,
        widthPercent: Float,
        heightDp: Int,
        xOffsetDp: Float,
        yOffsetDp: Float
    ) {
        updateConfig { current ->
            current.copy(
                lockWidgets = current.lockWidgets.map { w ->
                    if (w.id == widgetId) w.copy(
                        widthPercent = widthPercent.coerceIn(0.44f, 1.0f),
                        heightDp = heightDp.coerceIn(80, 320),
                        xOffsetDp = xOffsetDp.coerceIn(-100f, 100f),
                        yOffsetDp = yOffsetDp.coerceIn(-80f, 160f)
                    ) else w
                }
            )
        }
    }

    fun removeLockWidget(widgetId: String) {
        updateConfig { current ->
            current.copy(lockWidgets = current.lockWidgets.filterNot { it.id == widgetId })
        }
    }

    // --- Aesthetic Notification Sounds (Built-in + User Custom Audio) ---
    fun selectNotificationSound(soundId: String) {
        updateConfig { it.copy(activeNotifSoundId = soundId) }
    }

    fun previewNotificationSound(soundId: String) {
        NotificationSoundEngine.playNotificationSound(
            context = appContext,
            soundId = soundId,
            customSounds = config.value.customNotifSounds,
            throttleMs = 0L
        )
    }

    fun addCustomNotificationSound(name: String, uriString: String) {
        val newSound = CustomNotificationSound(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Suara Kustom" },
            uriString = uriString
        )
        updateConfig { current ->
            current.copy(
                customNotifSounds = current.customNotifSounds + newSound,
                activeNotifSoundId = newSound.id
            )
        }
        NotificationSoundEngine.playNotificationSound(
            context = appContext,
            soundId = newSound.id,
            customSounds = config.value.customNotifSounds + newSound,
            throttleMs = 0L
        )
    }

    fun removeCustomNotificationSound(id: String) {
        updateConfig { current ->
            val filtered = current.customNotifSounds.filterNot { it.id == id }
            val active = if (current.activeNotifSoundId == id) BuiltInNotifSound.CRYSTAL_DEW.id
            else current.activeNotifSoundId
            current.copy(customNotifSounds = filtered, activeNotifSoundId = active)
        }
    }

    // --- Quick Panel Direct Customization ---
    fun toggleQuickPanelOrientation() {
        updateConfig { current ->
            val next = if (current.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS) {
                QuickPanelOrientation.HORIZONTAL_STRIP
            } else {
                QuickPanelOrientation.VERTICAL_IOS
            }
            current.copy(quickPanelOrientation = next)
        }
    }

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
            isStudioOpen = false,
            isUniversalSearchOpen = false
        )
    }

    fun closeQuickPanel() {
        _overlays.value = _overlays.value.copy(isQuickPanelOpen = false, quickPanelFeedback = null)
    }

    // --- Modern Wallpaper Studio (Live, Glass, Gallery Photo, System Wallpaper) ---
    fun updateHomeWallpaper(transform: (WallpaperConfig) -> WallpaperConfig) {
        updateConfig { current ->
            val updated = transform(current.wallpaperConfig)
            current.copy(
                wallpaperPreset = updated.preset,
                wallpaperConfig = updated
            )
        }
    }

    fun updateLockWallpaper(transform: (WallpaperConfig) -> WallpaperConfig) {
        updateConfig { current ->
            val updated = transform(current.lockWallpaperConfig)
            current.copy(
                separateLockWallpaper = true,
                lockWallpaperConfig = updated
            )
        }
    }

    fun setSeparateLockWallpaper(enabled: Boolean) {
        updateConfig { current ->
            current.copy(separateLockWallpaper = enabled)
        }
    }

    fun importGalleryWallpaper(uri: Uri, isLockScreen: Boolean) {
        viewModelScope.launch {
            showWallpaperFeedback("Memproses foto galeri...")
            val savedPath = WallpaperStorageManager.importCustomWallpaper(appContext, uri, isLockScreen)
            if (savedPath != null) {
                if (isLockScreen) {
                    updateLockWallpaper {
                        it.copy(preset = WallpaperPreset.CUSTOM_PHOTO, customImagePath = savedPath)
                    }
                    showWallpaperFeedback("Foto galeri diterapkan ke Wallpaper Layar Kunci!")
                } else {
                    updateHomeWallpaper {
                        it.copy(preset = WallpaperPreset.CUSTOM_PHOTO, customImagePath = savedPath)
                    }
                    showWallpaperFeedback("Foto galeri diterapkan ke Wallpaper Beranda!")
                }
            } else {
                showWallpaperFeedback("Gagal memuat foto galeri, coba pilih gambar lain.")
            }
        }
    }

    fun applyWallpaperToSystem(wpConfig: WallpaperConfig, target: SystemWallpaperTarget) {
        viewModelScope.launch {
            showWallpaperFeedback("Menerapkan ke ${target.label}...")
            val result = WallpaperStorageManager.applyToAndroidSystemWallpaper(appContext, wpConfig, target)
            val msg = result.getOrElse { "Gagal menerapkan wallpaper sistem: ${it.localizedMessage.orEmpty()}" }
            showWallpaperFeedback(msg)
        }
    }

    private fun showWallpaperFeedback(message: String) {
        wallpaperFeedbackJob?.cancel()
        _overlays.value = _overlays.value.copy(wallpaperFeedback = message)
        wallpaperFeedbackJob = viewModelScope.launch {
            delay(3_200L)
            _overlays.value = _overlays.value.copy(wallpaperFeedback = null)
        }
    }

    fun openStudio(startOnWallpaper: Boolean = false) {
        refreshSyncStatus()
        _overlays.value = _overlays.value.copy(
            isStudioOpen = true,
            openStudioOnWallpaper = startOnWallpaper,
            isQuickPanelOpen = false,
            isAppDrawerOpen = false,
            isUniversalSearchOpen = false
        )
    }

    fun closeStudio() {
        _overlays.value = _overlays.value.copy(
            isStudioOpen = false,
            openStudioOnWallpaper = false
        )
    }

    fun openAppDrawer() {
        _overlays.value = _overlays.value.copy(
            isAppDrawerOpen = true,
            isQuickPanelOpen = false,
            isStudioOpen = false,
            isUniversalSearchOpen = false
        )
    }

    fun closeAppDrawer() {
        _overlays.value = _overlays.value.copy(isAppDrawerOpen = false)
    }

    fun openUniversalSearch() {
        _overlays.value = _overlays.value.copy(
            isUniversalSearchOpen = true,
            isQuickPanelOpen = false,
            isStudioOpen = false,
            isAppDrawerOpen = false
        )
    }

    fun closeUniversalSearch() {
        _overlays.value = _overlays.value.copy(isUniversalSearchOpen = false)
    }

    fun enterLockScreen() {
        _overlays.value = _overlays.value.copy(
            isLockScreenActive = true,
            isQuickPanelOpen = false,
            isStudioOpen = false,
            isAppDrawerOpen = false,
            isUniversalSearchOpen = false,
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
        if (config.value.replaceSystemNotifications) {
            previewNotificationSound(config.value.activeNotifSoundId)
        }
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
                body = "Gaya ${config.value.lockPopupStyle.label} & suara estetik aktif.",
                badge = "Layar Kunci",
                isLockScreenContext = true
            )
        } else {
            PopupMessage(
                title = "Notifikasi Beranda",
                body = "Gaya ${config.value.homePopupStyle.label} & suara estetik aktif.",
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
        closeUniversalSearch()
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

    fun mediaNext() = systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)

    fun mediaPrevious() = systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun showQuickPanelFeedback(message: String) {
        feedbackDismissJob?.cancel()
        _overlays.value = _overlays.value.copy(quickPanelFeedback = message)
        feedbackDismissJob = viewModelScope.launch {
            delay(2_400L)
            _overlays.value = _overlays.value.copy(quickPanelFeedback = null)
        }
    }

    /** Direct instant module response without kicking user out to native Settings app! */
    fun handleQuickModuleClick(type: QuickModuleType) {
        when (type) {
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
            else -> {
                val feedback = systemMonitor.handleDirectInstantToggle(type, telemetry.value)
                showQuickPanelFeedback(feedback)
            }
        }
    }

    /** Long-press a Quick Panel module if the user explicitly wants native Android detail settings. */
    fun handleQuickModuleLongPress(type: QuickModuleType) {
        systemMonitor.openModuleDetailSettings(type)
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
