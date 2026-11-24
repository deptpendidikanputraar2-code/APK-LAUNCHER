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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.launcher.data.LauncherPreferencesRepository
import com.example.launcher.data.SystemMonitor
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.system.NotificationHub
import com.example.launcher.system.SystemSyncChecker
import com.example.launcher.ui.components.VitroDynamicIsland
import com.example.launcher.ui.screens.LockScreenOverlay
import com.example.launcher.viewmodel.LauncherUiOverlays
import com.example.ui.theme.VitroLauncherTheme
import kotlinx.coroutines.launch

/**
 * Dedicated full-screen native Lock Screen replacement Activity.
 * Automatically wakes over the screen when the user turns on their phone (if enabled),
 * featuring Adaptive Clock DIY, Lock Screen Glass Widgets, Dynamic Island, live notifications,
 * and PIN/Password security.
 */
class VitroLockScreenActivity : ComponentActivity() {

    private var hideStatusBarPref: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

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
            val notifications by NotificationHub.notifications.collectAsStateWithLifecycle()
            val nowPlaying by NotificationHub.nowPlaying.collectAsStateWithLifecycle()
            val notifEnabled = SystemSyncChecker.isNotificationListenerEnabled(applicationContext)

            LaunchedEffect(config.hideSystemStatusBar) {
                hideStatusBarPref = config.hideSystemStatusBar
                applyStatusBarVisibility(config.hideSystemStatusBar)
            }

            LaunchedEffect(config.separateLockWallpaper, config.wallpaperConfig.preset, config.lockWallpaperConfig.preset) {
                if (config.effectiveLockWallpaper().preset == WallpaperPreset.SYSTEM_LIVE) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                }
            }

            fun saveUpdated(transform: (LauncherConfig) -> LauncherConfig) {
                val updated = transform(config)
                lifecycleScope.launch { prefsRepo.saveConfig(updated) }
            }

            VitroLauncherTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LockScreenOverlay(
                        visible = true,
                        config = config,
                        telemetry = telemetry,
                        overlays = LauncherUiOverlays(isLockScreenActive = true),
                        nowPlaying = nowPlaying,
                        notifications = notifications,
                        isNotificationAccessEnabled = notifEnabled,
                        onUnlock = {
                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        },
                        onVerifyPasscode = { raw ->
                            SystemSyncChecker.verifyPasscode(raw, config.lockPasscodeHash)
                        },
                        onToggleFlashlight = { systemMonitor.toggleFlashlight() },
                        onToggleMedia = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                        onTriggerLockPopup = {},
                        onOpenNotification = { notif ->
                            NotificationHub.openNotification(applicationContext, notif)
                            finish()
                        },
                        onDismissNotification = { key -> NotificationHub.dismissNotification(key) },
                        onClearAllNotifications = { NotificationHub.clearAllDismissible() },
                        onEnableNotificationAccess = { SystemSyncChecker.openNotificationListenerSettings(applicationContext) },
                        onOpenStudioFromLock = { finish() },
                        onUpdateLockClockLayout = { scale, vOff ->
                            saveUpdated {
                                it.copy(
                                    lockClockScale = scale.coerceIn(0.55f, 1.85f),
                                    lockClockVerticalOffsetDp = vOff.coerceIn(-60f, 160f)
                                )
                            }
                        },
                        onUpdateLockClockStyle = { style -> saveUpdated { it.copy(lockClockStyle = style) } },
                        onUpdateLockClockColor = { color -> saveUpdated { it.copy(lockClockColor = color) } },
                        onUpdateLockClockWeight = { weight -> saveUpdated { it.copy(lockClockWeight = weight.coerceIn(300, 900)) } },
                        onAddLockWidget = { wt ->
                            saveUpdated { cur ->
                                val item = HomeWidgetPlacement(
                                    id = "lw_${wt.name.lowercase()}_${System.currentTimeMillis()}",
                                    pageIndex = 0,
                                    builtInType = wt,
                                    label = wt.label,
                                    widthPercent = 0.48f,
                                    heightDp = 112
                                )
                                cur.copy(lockWidgets = cur.lockWidgets + item)
                            }
                        },
                        onUpdateLockWidgetLayout = { id, wPct, hDp, xOff, yOff ->
                            saveUpdated { cur ->
                                cur.copy(
                                    lockWidgets = cur.lockWidgets.map { w ->
                                        if (w.id == id) w.copy(
                                            widthPercent = wPct.coerceIn(0.44f, 1.0f),
                                            heightDp = hDp.coerceIn(80, 320),
                                            xOffsetDp = xOff.coerceIn(-100f, 100f),
                                            yOffsetDp = yOff.coerceIn(-80f, 160f)
                                        ) else w
                                    }
                                )
                            }
                        },
                        onRemoveLockWidget = { id ->
                            saveUpdated { cur -> cur.copy(lockWidgets = cur.lockWidgets.filterNot { it.id == id }) }
                        }
                    )

                    if (config.dynamicIsland.enabled && config.dynamicIsland.showOnLockScreen) {
                        VitroDynamicIsland(
                            islandConfig = config.dynamicIsland,
                            telemetry = telemetry,
                            overlays = LauncherUiOverlays(isLockScreenActive = true),
                            notifications = notifications,
                            nowPlaying = nowPlaying,
                            onMediaPlayPause = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                            onMediaNext = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
                            onMediaPrev = { systemMonitor.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                            onToggleFlashlight = { systemMonitor.toggleFlashlight() },
                            onOpenQuickPanel = {},
                            onOpenNotification = { notif ->
                                NotificationHub.openNotification(applicationContext, notif)
                                finish()
                            },
                            onDismissNotification = { key -> NotificationHub.dismissNotification(key) },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
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
}
