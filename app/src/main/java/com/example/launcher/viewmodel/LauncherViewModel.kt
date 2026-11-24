package com.example.launcher.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.AppRepository
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.data.SystemMonitor
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.ModuleSize
import com.example.launcher.model.PopupMessage
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.WidgetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val prefsRepo = LauncherPreferencesRepository(application)
    private val appRepo = AppRepository(application)
    private val systemMonitor = SystemMonitor(application)

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

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _overlays = MutableStateFlow(LauncherUiOverlays())
    val overlays: StateFlow<LauncherUiOverlays> = _overlays.asStateFlow()

    private var popupDismissJob: Job? = null
    private var focusTimerJob: Job? = null

    init {
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = appRepo.loadInstalledApps()
            _installedApps.value = apps
        }
    }

    fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) {
        val updated = transform(config.value)
        viewModelScope.launch {
            prefsRepo.saveConfig(updated)
        }
    }

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

    fun toggleQuickModule(type: QuickModuleType) {
        updateConfig { current ->
            val updated = current.quickModules.map { mod ->
                if (mod.type == type) mod.copy(enabled = !mod.enabled) else mod
            }
            current.copy(quickModules = updated)
        }
    }

    fun cycleQuickModuleSize(type: QuickModuleType) {
        updateConfig { current ->
            val sizes = ModuleSize.entries
            val updated = current.quickModules.map { mod ->
                if (mod.type == type) {
                    val nextSize = sizes[(sizes.indexOf(mod.size) + 1) % sizes.size]
                    mod.copy(size = nextSize)
                } else mod
            }
            current.copy(quickModules = updated)
        }
    }

    fun setQuickModuleSize(type: QuickModuleType, size: ModuleSize) {
        updateConfig { current ->
            val updated = current.quickModules.map { mod ->
                if (mod.type == type) mod.copy(size = size) else mod
            }
            current.copy(quickModules = updated)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            prefsRepo.saveConfig(LauncherConfig())
        }
    }

    // Overlays & navigation state
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

    fun setSmartStackIndex(index: Int) {
        val count = config.value.enabledWidgets.size.coerceAtLeast(1)
        _overlays.value = _overlays.value.copy(activeSmartStackIndex = index.coerceIn(0, count - 1))
    }

    // Pop-ups (Home Screen & Lock Screen)
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
        val msg = if (isLockScreen) {
            PopupMessage(
                title = "Pesan Masuk • Vitro",
                body = "Pratinjau gaya pop-up Layar Kunci (${config.value.lockPopupStyle.label}) aktif dan siap digunakan.",
                badge = "Layar Kunci",
                isLockScreenContext = true
            )
        } else {
            PopupMessage(
                title = "Notifikasi Beranda",
                body = "Gaya pop-up ${config.value.homePopupStyle.label} dengan efek kaca halus aktif.",
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

    // Real hardware & app controls
    fun launchApp(app: InstalledApp) {
        dismissPopup()
        closeAppDrawer()
        appRepo.launchApp(app)
    }

    fun openAppInfo(app: InstalledApp) {
        dismissPopup()
        appRepo.openAppInfo(app)
    }

    fun setVolume(fraction: Float) = systemMonitor.setMediaVolume(fraction)

    fun setBrightness(fraction: Float) = systemMonitor.setBrightnessFraction(fraction)

    fun handleQuickModuleClick(type: QuickModuleType) {
        when (type) {
            QuickModuleType.WIFI -> systemMonitor.openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
            QuickModuleType.CELLULAR -> systemMonitor.openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS)
            QuickModuleType.BLUETOOTH -> {
                systemMonitor.toggleBluetoothVisual()
                systemMonitor.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            QuickModuleType.FLASHLIGHT -> systemMonitor.toggleFlashlight()
            QuickModuleType.DND -> systemMonitor.toggleDnd()
            QuickModuleType.ROTATION -> systemMonitor.toggleAutoRotate()
            QuickModuleType.LOCK_SCREEN -> enterLockScreen()
            QuickModuleType.STUDIO -> openStudio()
        }
    }

    fun toggleMediaPlayback() {
        _overlays.value = _overlays.value.copy(isMediaPlaying = !_overlays.value.isMediaPlaying)
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
