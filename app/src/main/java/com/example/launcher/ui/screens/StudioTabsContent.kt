package com.example.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.launcher.model.BatteryIconStyle
import com.example.launcher.model.DockColorPreset
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.ModuleSize
import com.example.launcher.model.PageTransitionStyle
import com.example.launcher.model.PopupStyle
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.model.SidebarSide
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.StatusSlotAlignment
import com.example.launcher.model.TransitionSpeed
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DockStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Tata Letak Dock Bar")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DockLayoutMode.entries.forEach { mode ->
                StudioRadioCard(
                    title = mode.label,
                    subtitle = mode.subtitle,
                    selected = config.dockLayout == mode,
                    onClick = { onUpdate { it.copy(dockLayout = mode) } }
                )
            }
        }

        StudioSectionTitle("Warna Kaca Dock")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DockColorPreset.entries.forEach { preset ->
                val selected = config.dockColor == preset
                VitroGlassContainer(
                    cornerRadius = 18.dp,
                    tintColor = preset.baseColor,
                    glassOpacity = if (selected) 0.45f else 0.24f,
                    borderColor = if (selected) VitroCyanAccent else preset.borderColor,
                    modifier = Modifier.bouncyClickable {
                        onUpdate { it.copy(dockColor = preset) }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(preset.baseColor)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (preset == DockColorPreset.FROSTED_CLEAR) Color.Black else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = VitroTextPrimary
                        )
                    }
                }
            }
        }

        StudioSliderRow(
            label = "Transparansi Kaca",
            valueText = "${(config.dockOpacity * 100).roundToInt()}%",
            value = config.dockOpacity,
            range = 0.10f..0.65f,
            onValueChange = { v -> onUpdate { it.copy(dockOpacity = v) } }
        )

        StudioSliderRow(
            label = "Kelengkungan Sudut",
            valueText = "${config.dockCornerRadius.roundToInt()} dp",
            value = config.dockCornerRadius,
            range = 8f..40f,
            onValueChange = { v -> onUpdate { it.copy(dockCornerRadius = v) } }
        )

        StudioSliderRow(
            label = "Kapasitas Ikon Dock",
            valueText = "${config.dockMaxIcons} ikon",
            value = config.dockMaxIcons.toFloat(),
            range = 3f..6f,
            steps = 2,
            onValueChange = { v -> onUpdate { it.copy(dockMaxIcons = v.roundToInt()) } }
        )

        StudioToggleRow(
            label = "Tampilkan Nama Aplikasi di Dock",
            checked = config.showDockLabels,
            onCheckedChange = { checked -> onUpdate { it.copy(showDockLabels = checked) } }
        )
    }
}

@Composable
fun WidgetStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onMoveWidget: (WidgetType, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Gaya Susunan Widget")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetStackMode.entries.forEach { mode ->
                StudioRadioCard(
                    title = mode.label,
                    subtitle = mode.description,
                    selected = config.widgetStackMode == mode,
                    onClick = { onUpdate { it.copy(widgetStackMode = mode) } }
                )
            }
        }

        StudioSliderRow(
            label = "Transparansi Kaca Widget",
            valueText = "${(config.widgetGlassOpacity * 100).roundToInt()}%",
            value = config.widgetGlassOpacity,
            range = 0.10f..0.55f,
            onValueChange = { v -> onUpdate { it.copy(widgetGlassOpacity = v) } }
        )

        StudioSectionTitle("Urutan & Daftar Widget Aktif")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val orderedTypes = config.enabledWidgets + WidgetType.entries.filterNot { it in config.enabledWidgets }
            orderedTypes.forEach { type ->
                val enabled = type in config.enabledWidgets
                VitroGlassContainer(
                    cornerRadius = 18.dp,
                    glassOpacity = if (enabled) 0.24f else 0.12f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                tint = if (enabled) VitroCyanAccent else VitroTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = VitroTextPrimary
                            )
                        }
                        if (enabled) {
                            IconButton(onClick = { onMoveWidget(type, true) }) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naikkan", tint = VitroTextSecondary)
                            }
                            IconButton(onClick = { onMoveWidget(type, false) }) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turunkan", tint = VitroTextSecondary)
                            }
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { onToggleWidget(type) },
                            colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
                        )
                    }
                }
            }
        }

        StudioSectionTitle("Isi Widget Catatan Cepat")
        OutlinedTextField(
            value = config.quickNoteText,
            onValueChange = { text -> onUpdate { it.copy(quickNoteText = text) } },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VitroCyanAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                focusedTextColor = VitroTextPrimary,
                unfocusedTextColor = VitroTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanelStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleModule: (QuickModuleType) -> Unit,
    onSetModuleSize: (QuickModuleType, ModuleSize) -> Unit,
    onPreviewQuickPanel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onPreviewQuickPanel,
            colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_preview_quick_panel_button")
        ) {
            Text("Buka Pratinjau Quick Panel Glass", style = MaterialTheme.typography.labelLarge)
        }

        StudioSectionTitle("Orientasi Quick Panel")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickPanelOrientation.entries.forEach { orient ->
                StudioRadioCard(
                    title = orient.label,
                    subtitle = orient.description,
                    selected = config.quickPanelOrientation == orient,
                    onClick = { onUpdate { it.copy(quickPanelOrientation = orient) } }
                )
            }
        }

        StudioSliderRow(
            label = "Intensitas Kaca Panel",
            valueText = "${(config.quickPanelGlassOpacity * 100).roundToInt()}%",
            value = config.quickPanelGlassOpacity,
            range = 0.14f..0.55f,
            onValueChange = { v -> onUpdate { it.copy(quickPanelGlassOpacity = v) } }
        )

        StudioToggleRow("Slider Kecerahan Kaca", config.showBrightnessSlider) { c ->
            onUpdate { it.copy(showBrightnessSlider = c) }
        }
        StudioToggleRow("Slider Volume Media", config.showVolumeSlider) { c ->
            onUpdate { it.copy(showVolumeSlider = c) }
        }
        StudioToggleRow("Kartu Pemutar Media", config.showMediaControl) { c ->
            onUpdate { it.copy(showMediaControl = c) }
        }

        StudioSectionTitle("Atur Modul & Ukuran (1×1 / Horizontal / Vertikal)")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            config.quickModules.forEach { mod ->
                VitroGlassContainer(
                    cornerRadius = 18.dp,
                    glassOpacity = if (mod.enabled) 0.22f else 0.10f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mod.type.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                            Switch(
                                checked = mod.enabled,
                                onCheckedChange = { onToggleModule(mod.type) },
                                colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
                            )
                        }
                        if (mod.enabled) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModuleSize.entries.forEach { sz ->
                                    StudioSmallChip(
                                        text = sz.label,
                                        selected = mod.size == sz,
                                        onClick = { onSetModuleSize(mod.type, sz) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusIndicatorStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Posisi Indikator Sinyal, Baterai & Jam")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusIndicatorPosition.entries.forEach { pos ->
                StudioRadioCard(
                    title = pos.label,
                    subtitle = pos.description,
                    selected = config.statusPosition == pos,
                    onClick = { onUpdate { it.copy(statusPosition = pos) } }
                )
            }
        }

        if (config.statusPosition == StatusIndicatorPosition.TOP_STATUS_BAR) {
            StudioSectionTitle("Tata Letak Slot Status Bar")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusSlotAlignment.entries.forEach { align ->
                    StudioSmallChip(
                        text = align.label,
                        selected = config.statusAlignment == align,
                        onClick = { onUpdate { it.copy(statusAlignment = align) } }
                    )
                }
            }
        }

        StudioSectionTitle("Gaya Ikon Baterai")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BatteryIconStyle.entries.forEach { style ->
                StudioSmallChip(
                    text = style.label,
                    selected = config.batteryStyle == style,
                    onClick = { onUpdate { it.copy(batteryStyle = style) } }
                )
            }
        }

        StudioSectionTitle("Elemen yang Ditampilkan")
        StudioToggleRow("Jam Digital", config.showClockIndicator) { c -> onUpdate { it.copy(showClockIndicator = c) } }
        StudioToggleRow("Indikator Sinyal Seluler", config.showSignalIndicator) { c -> onUpdate { it.copy(showSignalIndicator = c) } }
        StudioToggleRow("Indikator Wi-Fi", config.showWifiIndicator) { c -> onUpdate { it.copy(showWifiIndicator = c) } }
        StudioToggleRow("Angka Persentase Baterai", config.showBatteryPercent) { c -> onUpdate { it.copy(showBatteryPercent = c) } }
    }
}

@Composable
fun PopupStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onPreviewHomePopup: () -> Unit,
    onPreviewLockPopup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPreviewHomePopup,
                colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_test_home_popup_button")
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Uji Home", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onPreviewLockPopup,
                colors = ButtonDefaults.buttonColors(containerColor = VitroIndigo, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_test_lock_popup_button")
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Uji Lock", style = MaterialTheme.typography.labelLarge)
            }
        }

        StudioSectionTitle("Gaya Pop-Up Home Screen")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PopupStyle.entries.forEach { style ->
                StudioRadioCard(
                    title = style.label,
                    subtitle = style.description,
                    selected = config.homePopupStyle == style,
                    onClick = { onUpdate { it.copy(homePopupStyle = style) } }
                )
            }
        }

        StudioSectionTitle("Gaya Pop-Up Lock Screen")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PopupStyle.entries.forEach { style ->
                StudioRadioCard(
                    title = style.label,
                    subtitle = style.description,
                    selected = config.lockPopupStyle == style,
                    onClick = { onUpdate { it.copy(lockPopupStyle = style) } }
                )
            }
        }

        StudioSliderRow(
            label = "Kelengkungan Pop-Up",
            valueText = "${config.popupCornerRadius.roundToInt()} dp",
            value = config.popupCornerRadius,
            range = 12f..38f,
            onValueChange = { v -> onUpdate { it.copy(popupCornerRadius = v) } }
        )

        StudioSliderRow(
            label = "Transparansi Kaca Pop-Up",
            valueText = "${(config.popupGlassOpacity * 100).roundToInt()}%",
            value = config.popupGlassOpacity,
            range = 0.15f..0.60f,
            onValueChange = { v -> onUpdate { it.copy(popupGlassOpacity = v) } }
        )
    }
}

@Composable
fun PagesStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onPreviewLockScreen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Halaman & Ukuran Home Screen")
        StudioSliderRow(
            label = "Jumlah Halaman Home Screen",
            valueText = "${config.homePageCount} halaman",
            value = config.homePageCount.toFloat(),
            range = 1f..7f,
            steps = 5,
            onValueChange = { v -> onUpdate { it.copy(homePageCount = v.roundToInt()) } }
        )
        StudioSliderRow(
            label = "Jumlah Kolom Grid",
            valueText = "${config.homeGridColumns} kolom",
            value = config.homeGridColumns.toFloat(),
            range = 3f..6f,
            steps = 2,
            onValueChange = { v -> onUpdate { it.copy(homeGridColumns = v.roundToInt()) } }
        )
        StudioSliderRow(
            label = "Jumlah Baris Grid",
            valueText = "${config.homeGridRows} baris",
            value = config.homeGridRows.toFloat(),
            range = 4f..7f,
            steps = 2,
            onValueChange = { v -> onUpdate { it.copy(homeGridRows = v.roundToInt()) } }
        )
        StudioSliderRow(
            label = "Ukuran / Skala Halaman Home",
            valueText = "${(config.homePageScale * 100).roundToInt()}%",
            value = config.homePageScale,
            range = 0.85f..1.15f,
            onValueChange = { v -> onUpdate { it.copy(homePageScale = v) } }
        )

        StudioSectionTitle("Halaman & Ukuran Lock Screen")
        StudioSliderRow(
            label = "Jumlah Halaman Lock Screen",
            valueText = "${config.lockPageCount} halaman",
            value = config.lockPageCount.toFloat(),
            range = 1f..4f,
            steps = 2,
            onValueChange = { v -> onUpdate { it.copy(lockPageCount = v.roundToInt()) } }
        )
        StudioSliderRow(
            label = "Ukuran Jam Lock Screen",
            valueText = "${(config.lockClockScale * 100).roundToInt()}%",
            value = config.lockClockScale,
            range = 0.80f..1.25f,
            onValueChange = { v -> onUpdate { it.copy(lockClockScale = v) } }
        )
        StudioSliderRow(
            label = "Ukuran / Skala Halaman Lock Screen",
            valueText = "${(config.lockPageScale * 100).roundToInt()}%",
            value = config.lockPageScale,
            range = 0.85f..1.12f,
            onValueChange = { v -> onUpdate { it.copy(lockPageScale = v) } }
        )

        Button(
            onClick = onPreviewLockScreen,
            colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_preview_lock_screen_button")
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lihat Pratinjau Lock Screen Multi-Halaman", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransitionStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Animasi Transisi Antar Halaman")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PageTransitionStyle.entries.forEach { style ->
                StudioRadioCard(
                    title = style.label,
                    subtitle = style.description,
                    selected = config.pageTransitionStyle == style,
                    onClick = { onUpdate { it.copy(pageTransitionStyle = style) } }
                )
            }
        }

        StudioSectionTitle("Respons & Kecepatan Animasi")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransitionSpeed.entries.forEach { speed ->
                StudioSmallChip(
                    text = speed.label,
                    selected = config.transitionSpeed == speed,
                    onClick = { onUpdate { it.copy(transitionSpeed = speed) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeWallpaperStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionTitle("Sidebar Kaca v7 (Memanjang ke Atas)")
        StudioToggleRow(
            label = "Tampilkan Pil Sidebar di Atas Dock Bar",
            checked = config.showGlassySidebar,
            onCheckedChange = { checked -> onUpdate { it.copy(showGlassySidebar = checked) } }
        )
        if (config.showGlassySidebar) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SidebarSide.entries.forEach { side ->
                    StudioSmallChip(
                        text = side.label,
                        selected = config.sidebarSide == side,
                        onClick = { onUpdate { it.copy(sidebarSide = side) } }
                    )
                }
            }
        }
        StudioSliderRow(
            label = "Intensitas Efek Pendar (Glow)",
            valueText = "${(config.ambientGlowIntensity * 100).roundToInt()}%",
            value = config.ambientGlowIntensity,
            range = 0f..1f,
            onValueChange = { v -> onUpdate { it.copy(ambientGlowIntensity = v) } }
        )

        StudioSectionTitle("Wallpaper Kaca Bawaan")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperPreset.entries.forEach { preset ->
                StudioRadioCard(
                    title = preset.label,
                    subtitle = "Latar beresolusi tinggi optimal untuk efek frosted glass",
                    selected = config.wallpaperPreset == preset,
                    onClick = { onUpdate { it.copy(wallpaperPreset = preset) } }
                )
            }
        }

        StudioSectionTitle("Integrasi Sistem Android")
        Button(
            onClick = onSetDefaultLauncher,
            colors = ButtonDefaults.buttonColors(containerColor = VitroMint, contentColor = VitroObsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_set_default_launcher_button")
        ) {
            Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jadikan Launcher Utama Android", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = onResetDefaults,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_reset_defaults_button")
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = VitroTextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembalikan Pengaturan Awal", color = VitroTextPrimary)
        }
    }
}

@Composable
fun StudioSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = VitroCyanAccent
    )
}

@Composable
fun StudioRadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 18.dp,
        tintColor = if (selected) VitroCyanAccent else Color.White,
        glassOpacity = if (selected) 0.28f else 0.14f,
        borderColor = if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.24f),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VitroTextSecondary)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(VitroCyanAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Terpilih", tint = VitroObsidian, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StudioSmallChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 50.dp,
        tintColor = if (selected) VitroCyanAccent else Color.White,
        glassOpacity = if (selected) 0.36f else 0.15f,
        borderColor = if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.28f),
        modifier = Modifier.bouncyClickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = VitroTextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 18.dp,
        glassOpacity = 0.15f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                Text(valueText, style = MaterialTheme.typography.labelLarge, color = VitroCyanAccent)
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = VitroCyanAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                )
            )
        }
    }
}

@Composable
fun StudioToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 18.dp,
        glassOpacity = 0.15f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
            )
        }
    }
}
