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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.ModuleSize
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.WidgetType
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

private enum class StudioCategory(val label: String, val icon: ImageVector) {
    DOCK("Dock Bar", Icons.Rounded.Dock),
    WIDGETS("Widget", Icons.Rounded.DashboardCustomize),
    QUICK_PANEL("Quick Panel", Icons.Rounded.Tune),
    STATUS("Indikator", Icons.Rounded.SignalCellularAlt),
    POPUP("Pop-Up", Icons.Rounded.NotificationsActive),
    PAGES("Halaman", Icons.Rounded.ViewCarousel),
    TRANSITION("Transisi", Icons.Rounded.Animation),
    THEME("Tema", Icons.Rounded.Palette)
}

@Composable
fun CustomizationStudioOverlay(
    visible: Boolean,
    config: LauncherConfig,
    onDismiss: () -> Unit,
    onUpdateConfig: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onMoveWidget: (WidgetType, Boolean) -> Unit,
    onToggleQuickModule: (QuickModuleType) -> Unit,
    onSetQuickModuleSize: (QuickModuleType, ModuleSize) -> Unit,
    onPreviewQuickPanel: () -> Unit,
    onPreviewHomePopup: () -> Unit,
    onPreviewLockPopup: () -> Unit,
    onPreviewLockScreen: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onResetDefaults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(StudioCategory.DOCK) }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE0060B18))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("customization_studio_overlay"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
            ) {
                // Studio Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = VitroTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
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

                // Horizontal Glass Category Bar
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(StudioCategory.entries) { tab ->
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

                // Active Tab Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 28.dp)
                ) {
                    when (selectedTab) {
                        StudioCategory.DOCK -> DockStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.WIDGETS -> WidgetStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onToggleWidget = onToggleWidget,
                            onMoveWidget = onMoveWidget
                        )
                        StudioCategory.QUICK_PANEL -> QuickPanelStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onToggleModule = onToggleQuickModule,
                            onSetModuleSize = onSetQuickModuleSize,
                            onPreviewQuickPanel = onPreviewQuickPanel
                        )
                        StudioCategory.STATUS -> StatusIndicatorStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.POPUP -> PopupStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onPreviewHomePopup = onPreviewHomePopup,
                            onPreviewLockPopup = onPreviewLockPopup
                        )
                        StudioCategory.PAGES -> PagesStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onPreviewLockScreen = onPreviewLockScreen
                        )
                        StudioCategory.TRANSITION -> TransitionStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig
                        )
                        StudioCategory.THEME -> ThemeWallpaperStudioTab(
                            config = config,
                            onUpdate = onUpdateConfig,
                            onSetDefaultLauncher = onSetDefaultLauncher,
                            onResetDefaults = onResetDefaults
                        )
                    }
                }
            }
        }
    }
}
