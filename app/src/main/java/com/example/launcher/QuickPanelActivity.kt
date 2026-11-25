package com.example.launcher

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.data.SystemMonitor
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.system.NotificationHub
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.system.VitroAccessibilityService
import com.example.launcher.system.VitroEdgeControlManager
import com.example.launcher.ui.components.QuickPanelOverlay
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroLauncherTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Translucent system-wide Control Center overlay Activity.
 * Launched by [com.example.launcher.system.VitroEdgeControlManager] when swiping down
 * from the top edge over any app on the phone.
 */
class QuickPanelActivity : ComponentActivity() {

    private var hideStatusBarPref: Boolean = true
    private var toastJob: Job? = null

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

        val prefsRepo = LauncherPreferencesRepository(applicationContext)
        val systemMonitor = SystemMonitor(applicationContext)

        setContent {
            val config by prefsRepo.configFlow.collectAsStateWithLifecycle(initialValue = LauncherConfig())
            val telemetry by systemMonitor.observeTelemetry().collectAsStateWithLifecycle(initialValue = DeviceTelemetry())
            val nowPlaying by NotificationHub.nowPlaying.collectAsStateWithLifecycle()
            var localPlaying by remember { mutableStateOf(false) }
            var feedbackToast by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(config.hideSystemStatusBar) {
                hideStatusBarPref = config.hideSystemStatusBar
                applyStatusBarVisibility(config.hideSystemStatusBar)
            }

            LaunchedEffect(telemetry.brightnessFraction) {
                val params = window.attributes
                params.screenBrightness = telemetry.brightnessFraction.coerceIn(0.08f, 1.0f)
                window.attributes = params
            }

            fun saveConfigUpdate(transform: (LauncherConfig) -> LauncherConfig) {
                val updated = transform(config)
                lifecycleScope.launch { prefsRepo.saveConfig(updated) }
            }

            fun showToast(msg: String) {
                toastJob?.cancel()
                feedbackToast = msg
                toastJob = lifecycleScope.launch {
                    delay(2_200L)
                    feedbackToast = null
                }
            }

            VitroLauncherTheme {
                QuickPanelOverlay(
                    visible = true,
                    config = config,
                    telemetry = telemetry,
                    overlays = LauncherUiOverlays(
                        isQuickPanelOpen = true,
                        isMediaPlaying = localPlaying || nowPlaying.isPlaying
                    ),
                    nowPlaying = nowPlaying,
                    feedbackToast = feedbackToast,
                    onDismiss = { dismissSmoothly() },
                    onModuleClick = { type ->
                        when (type) {
                            QuickModuleType.SCREENSHOT -> {
                                dismissSmoothly()
                                lifecycleScope.launch {
                                    delay(280L)
                                    if (!VitroAccessibilityService.takeScreenshot()) {
                                        SystemSyncChecker.openAccessibilitySettings(applicationContext)
                                    }
                                }
                            }
                            QuickModuleType.CAMERA -> {
                                systemMonitor.openCameraApp()
                                dismissSmoothly()
                            }
                            QuickModuleType.LOCK_SCREEN -> {
                                dismissSmoothly()
                                if (!VitroAccessibilityService.lockPhoneScreen()) {
                                    SystemSyncChecker.openAccessibilitySettings(applicationContext)
                                }
                            }
                            else -> {
                                val msg = systemMonitor.handleDirectInstantToggle(type, telemetry)
                                showToast(msg)
                            }
                        }
                    },
                    onModuleLongPress = { type ->
                        systemMonitor.openModuleDetailSettings(type)
                    },
                    onToggleOrientation = {
                        saveConfigUpdate { cur ->
                            val next = if (cur.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS) {
                                QuickPanelOrientation.HORIZONTAL_STRIP
                            } else QuickPanelOrientation.VERTICAL_IOS
                            cur.copy(quickPanelOrientation = next)
                        }
                    },
                    onUpdateModuleSize = { type, widthPct, heightDp ->
                        saveConfigUpdate { cur ->
                            cur.copy(
                                quickModules = cur.quickModules.map { m ->
                                    if (m.type == type) {
                                        m.copy(
                                            widthPercent = widthPct.coerceIn(0.23f, 1.0f),
                                            heightDp = heightDp.coerceIn(56, 220)
                                        )
                                    } else m
                                }
                            )
                        }
                    },
                    onMoveModule = { type, moveUp ->
                        saveConfigUpdate { cur ->
                            val list = cur.quickModules.toMutableList()
                            val idx = list.indexOfFirst { it.type == type }
                            if (idx == -1) return@saveConfigUpdate cur
                            val target = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(list.lastIndex)
                            if (idx != target) {
                                val item = list.removeAt(idx)
                                list.add(target, item)
                            }
                            cur.copy(quickModules = list)
                        }
                    },
                    onBrightnessChange = { systemMonitor.setBrightnessFraction(it) },
                    onVolumeChange = { systemMonitor.setMediaVolume(it) },
                    onMediaPlayPause = {
                        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        localPlaying = !localPlaying
                    },
                    onMediaNext = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
                    onMediaPrev = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        VitroEdgeControlManager.onVitroActivityResumed(this, isQuickPanel = true)
        applyStatusBarVisibility(hideStatusBarPref)
    }

    override fun onPause() {
        VitroEdgeControlManager.onVitroActivityPaused(this, isQuickPanel = true)
        super.onPause()
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

    private fun dismissSmoothly() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
