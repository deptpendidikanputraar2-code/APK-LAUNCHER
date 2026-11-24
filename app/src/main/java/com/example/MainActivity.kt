package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.ui.components.QuickPanelOverlay
import com.example.launcher.ui.components.VitroPopupOverlay
import com.example.launcher.ui.components.VitroWallpaperBackdrop
import com.example.launcher.ui.screens.AppDrawerOverlay
import com.example.launcher.ui.screens.CustomizationStudioOverlay
import com.example.launcher.ui.screens.HomeScreen
import com.example.launcher.ui.screens.LockScreenOverlay
import com.example.launcher.viewmodel.LauncherViewModel
import com.example.ui.theme.VitroLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            VitroLauncherTheme {
                VitroLauncherRoot(
                    onApplyWindowBrightness = { brightness ->
                        val params = window.attributes
                        params.screenBrightness = brightness.coerceIn(0.08f, 1.0f)
                        window.attributes = params
                    }
                )
            }
        }
    }
}

@Composable
private fun VitroLauncherRoot(
    onApplyWindowBrightness: (Float) -> Unit,
    viewModel: LauncherViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val overlays by viewModel.overlays.collectAsStateWithLifecycle()

    LaunchedEffect(telemetry.brightnessFraction) {
        onApplyWindowBrightness(telemetry.brightnessFraction)
    }

    val activeNavTab = when {
        overlays.isStudioOpen -> com.example.launcher.model.HomeNavTab.STUDIO
        overlays.isLockScreenActive -> com.example.launcher.model.HomeNavTab.LOCK
        overlays.isQuickPanelOpen -> com.example.launcher.model.HomeNavTab.PANEL
        overlays.isAppDrawerOpen -> com.example.launcher.model.HomeNavTab.APPS
        else -> com.example.launcher.model.HomeNavTab.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: High-res frosted glass wallpaper backdrop with radiant ambient glow
        VitroWallpaperBackdrop(
            preset = config.wallpaperPreset,
            glowIntensity = config.ambientGlowIntensity
        )

        // Layer 2: Responsive Home Screen with Glassy Sidebar v7, Dock, Widgets & Interactive Glass Bottom Tab Bar
        HomeScreen(
            config = config,
            installedApps = installedApps,
            telemetry = telemetry,
            overlays = overlays,
            activeNavTab = activeNavTab,
            onLaunchApp = viewModel::launchApp,
            onLongPressApp = viewModel::showAppQuickPopup,
            onOpenQuickPanel = viewModel::openQuickPanel,
            onOpenAppDrawer = viewModel::openAppDrawer,
            onOpenStudio = viewModel::openStudio,
            onEnterLockScreen = viewModel::enterLockScreen,
            onTriggerHomePopup = { viewModel.triggerSamplePopup(isLockScreen = false) },
            onCycleWidgetStack = viewModel::cycleSmartStack,
            onToggleMedia = viewModel::toggleMediaPlayback,
            onToggleFocusTimer = viewModel::toggleFocusTimer,
            onResetFocusTimer = viewModel::resetFocusTimer
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

        // Layer 4: iPhone Glass Quick Panel Overlay
        QuickPanelOverlay(
            visible = overlays.isQuickPanelOpen,
            config = config,
            telemetry = telemetry,
            overlays = overlays,
            onDismiss = viewModel::closeQuickPanel,
            onModuleClick = viewModel::handleQuickModuleClick,
            onCycleModuleSize = viewModel::cycleQuickModuleSize,
            onToggleOrientation = {
                viewModel.updateConfig { current ->
                    val next = if (current.quickPanelOrientation == QuickPanelOrientation.VERTICAL_IOS)
                        QuickPanelOrientation.HORIZONTAL_STRIP
                    else QuickPanelOrientation.VERTICAL_IOS
                    current.copy(quickPanelOrientation = next)
                }
            },
            onBrightnessChange = viewModel::setBrightness,
            onVolumeChange = viewModel::setVolume,
            onToggleMedia = viewModel::toggleMediaPlayback,
            onOpenStudio = viewModel::openStudio
        )

        // Layer 5: Multi-page Interactive Lock Screen Mode
        LockScreenOverlay(
            visible = overlays.isLockScreenActive,
            config = config,
            telemetry = telemetry,
            onUnlock = viewModel::unlockScreen,
            onToggleFlashlight = { viewModel.handleQuickModuleClick(com.example.launcher.model.QuickModuleType.FLASHLIGHT) },
            onTriggerLockPopup = { viewModel.triggerSamplePopup(isLockScreen = true) },
            onOpenStudioFromLock = {
                viewModel.unlockScreen()
                viewModel.openStudio()
            }
        )

        // Layer 6: Customization Studio Overlay
        CustomizationStudioOverlay(
            visible = overlays.isStudioOpen,
            config = config,
            onDismiss = viewModel::closeStudio,
            onUpdateConfig = viewModel::updateConfig,
            onToggleWidget = viewModel::toggleWidgetEnabled,
            onMoveWidget = viewModel::moveWidgetOrder,
            onToggleQuickModule = viewModel::toggleQuickModule,
            onSetQuickModuleSize = viewModel::setQuickModuleSize,
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
            onSetDefaultLauncher = {
                runCatching {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            },
            onResetDefaults = viewModel::resetDefaults
        )

        // Layer 7: Topmost Styled Pop-Up Overlay (renders over Home Screen or Lock Screen)
        val pinnedTarget = overlays.activePopup?.targetApp?.packageName
        VitroPopupOverlay(
            popup = overlays.activePopup,
            config = config,
            isPinnedToDock = pinnedTarget != null && config.pinnedDockPackages.contains(pinnedTarget),
            onDismiss = viewModel::dismissPopup,
            onLaunchApp = viewModel::launchApp,
            onToggleDockPin = viewModel::toggleDockPin,
            onOpenAppInfo = viewModel::openAppInfo
        )
    }
}
