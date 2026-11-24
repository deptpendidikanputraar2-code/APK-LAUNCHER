package com.example.launcher

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.launcher.system.NotificationHub
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.system.VitroAccessibilityService
import com.example.launcher.ui.components.QuickPanelOverlay
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Translucent system-wide Control Center overlay Activity.
 * Launched by [com.example.launcher.system.VitroEdgeControlManager] when the user swipes down
 * from the top edge over any app on the phone, replacing the native Quick Settings shade.
 */
class QuickPanelActivity : ComponentActivity() {

    private var hideStatusBarPref: Boolean = true

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

            LaunchedEffect(config.hideSystemStatusBar) {
                hideStatusBarPref = config.hideSystemStatusBar
                applyStatusBarVisibility(config.hideSystemStatusBar)
            }

            LaunchedEffect(telemetry.brightnessFraction) {
                val params = window.attributes
                params.screenBrightness = telemetry.brightnessFraction.coerceIn(0.08f, 1.0f)
                window.attributes = params
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
                    onDismiss = { dismissSmoothly() },
                    onModuleClick = { type ->
                        handleModuleAction(type, systemMonitor)
                    },
                    onBrightnessChange = { fraction ->
                        systemMonitor.setBrightnessFraction(fraction)
                    },
                    onVolumeChange = { fraction ->
                        systemMonitor.setMediaVolume(fraction)
                    },
                    onMediaPlayPause = {
                        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        localPlaying = !localPlaying
                    },
                    onMediaNext = {
                        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                    },
                    onMediaPrev = {
                        systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    }
                )
            }
        }
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

    private fun handleModuleAction(type: QuickModuleType, systemMonitor: SystemMonitor) {
        when (type) {
            QuickModuleType.WIFI -> systemMonitor.openWifiPanel()
            QuickModuleType.CELLULAR -> {
                systemMonitor.openSystemSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)
                dismissSmoothly()
            }
            QuickModuleType.BLUETOOTH -> {
                systemMonitor.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
                dismissSmoothly()
            }
            QuickModuleType.FLASHLIGHT -> systemMonitor.toggleFlashlight()
            QuickModuleType.DND -> systemMonitor.toggleDndReal()
            QuickModuleType.ROTATION -> systemMonitor.toggleAutoRotateReal()
            QuickModuleType.AIRPLANE -> {
                systemMonitor.openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                dismissSmoothly()
            }
            QuickModuleType.HOTSPOT -> {
                systemMonitor.openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS)
                dismissSmoothly()
            }
            QuickModuleType.LOCATION -> {
                systemMonitor.openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                dismissSmoothly()
            }
            QuickModuleType.BATTERY_SAVER -> {
                systemMonitor.openSystemSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                dismissSmoothly()
            }
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
        }
    }

    private fun dismissSmoothly() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
