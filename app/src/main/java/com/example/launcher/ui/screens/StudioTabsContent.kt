package com.example.launcher.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcher.model.BatteryIconStyle
import com.example.launcher.model.DockColorPreset
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.DockSideAlignment
import com.example.launcher.model.EdgePanelTriggerZone
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.PageTransitionStyle
import com.example.launcher.model.PopupStyle
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.StatusSlotAlignment
import com.example.launcher.model.TransitionSpeed
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
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
    installedApps: List<InstalledApp>,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onAddDockApp: (String) -> Unit,
    onRemoveDockApp: (String) -> Unit,
    onMoveDockApp: (String, Boolean) -> Unit
) {
    var showDockAppPicker by remember { mutableStateOf(false) }

    val activeDockApps = remember(installedApps, config.pinnedDockPackages, config.dockMaxIcons) {
        val pinned = config.pinnedDockPackages.mapNotNull { pkg ->
            installedApps.firstOrNull { it.packageName == pkg }
        }
        if (pinned.isNotEmpty()) pinned else installedApps.take(config.dockMaxIcons)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Custom Dock App Picker & Reorder
        StudioSectionTitle("Atur Isi Aplikasi Dock Bar")
        Button(
            onClick = { showDockAppPicker = true },
            colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_add_dock_app_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pilih / Tambah Aplikasi ke Dock Bar", style = MaterialTheme.typography.labelLarge)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeDockApps.forEachIndexed { idx, app ->
                VitroGlassContainer(
                    cornerRadius = 18.dp,
                    glassOpacity = 0.22f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Column {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VitroTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Slot #${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VitroCyanAccent
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onMoveDockApp(app.packageName, true) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geser kiri", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMoveDockApp(app.packageName, false) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Geser kanan", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onRemoveDockApp(app.packageName) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus dari Dock", tint = VitroCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

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

        if (config.dockLayout == DockLayoutMode.LEFT_RAIL || config.dockLayout == DockLayoutMode.RIGHT_RAIL) {
            StudioSectionTitle("Posisi Vertikal Dock Samping (Atas / Tengah / Bawah)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DockSideAlignment.entries.forEach { align ->
                    StudioSmallChip(
                        text = align.label,
                        selected = config.dockSideAlignment == align,
                        onClick = {
                            onUpdate {
                                it.copy(
                                    dockSideAlignment = align,
                                    dockSideVerticalOffsetDp = 0f
                                )
                            }
                        }
                    )
                }
            }
            StudioSliderRow(
                label = "Geser Vertikal Bebas (Atas / Bawah)",
                valueText = "${config.dockSideVerticalOffsetDp.roundToInt()} dp",
                value = config.dockSideVerticalOffsetDp,
                range = -240f..240f,
                onValueChange = { v -> onUpdate { it.copy(dockSideVerticalOffsetDp = v) } }
            )
            if (config.dockSideVerticalOffsetDp != 0f) {
                OutlinedButton(
                    onClick = { onUpdate { it.copy(dockSideVerticalOffsetDp = 0f) } },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Posisi Vertikal Dock Samping", color = VitroCyanAccent)
                }
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
                        Text(preset.label, style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
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

    if (showDockAppPicker) {
        SearchableAppPickerDialog(
            title = "Pilih Aplikasi untuk Dock Bar",
            apps = installedApps,
            selectedPackages = activeDockApps.map { it.packageName }.toSet(),
            onPickApp = { app ->
                onAddDockApp(app.packageName)
                showDockAppPicker = false
            },
            onDismiss = { showDockAppPicker = false }
        )
    }
}

@Composable
fun WidgetStudioTab(
    config: LauncherConfig,
    availableAndroidWidgets: List<AvailableAndroidWidget>,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onMoveWidget: (WidgetType, Boolean) -> Unit,
    onAddBuiltInWidget: (WidgetType, Int) -> Unit,
    onPickAndroidWidget: (AvailableAndroidWidget, Int) -> Unit,
    onRemoveHomeWidget: (String) -> Unit,
    onResizeHomeWidget: (String, Int) -> Unit
) {
    var showAddWidgetPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { showAddWidgetPicker = true },
            colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_add_widget_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tambah Widget (Kaca Vitro / Widget Asli HP)", style = MaterialTheme.typography.labelLarge)
        }

        // User-added widgets across all pages
        StudioSectionTitle("Widget Terpasang di Halaman (${config.homeWidgets.size})")
        if (config.homeWidgets.isEmpty()) {
            Text(
                text = "Belum ada widget tambahan. Ketuk tombol di atas untuk menambah widget kaca atau widget asli Android dari HP Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = VitroTextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                config.homeWidgets.forEach { placement ->
                    val title = placement.builtInType?.label ?: placement.label.ifBlank { "Widget Android" }
                    val isAndroid = placement.appWidgetId != null
                    VitroGlassContainer(
                        cornerRadius = 18.dp,
                        glassOpacity = 0.22f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                                    Text(
                                        text = "${if (isAndroid) "Widget Asli HP" else "Widget Kaca"} • Halaman ${placement.pageIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAndroid) VitroMint else VitroCyanAccent
                                    )
                                }
                                IconButton(onClick = { onRemoveHomeWidget(placement.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus widget", tint = VitroCoral)
                                }
                            }
                            if (isAndroid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tinggi Widget", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                                    Text("${placement.heightDp} dp", style = MaterialTheme.typography.labelSmall, color = VitroCyanAccent)
                                }
                                Slider(
                                    value = placement.heightDp.toFloat(),
                                    onValueChange = { v -> onResizeHomeWidget(placement.id, v.roundToInt()) },
                                    valueRange = 96f..400f,
                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroCyanAccent)
                                )
                            }
                        }
                    }
                }
            }
        }

        StudioSectionTitle("Gaya Susunan Smart Stack (Halaman 1)")
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

        StudioSectionTitle("Urutan Widget di Smart Stack")
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

    if (showAddWidgetPicker) {
        WidgetAddPickerDialog(
            homePageCount = config.homePageCount,
            androidWidgets = availableAndroidWidgets,
            onAddBuiltInWidget = onAddBuiltInWidget,
            onPickAndroidWidget = onPickAndroidWidget,
            onDismiss = { showAddWidgetPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanelStudioTab(
    config: LauncherConfig,
    isOverlayGranted: Boolean,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleModule: (QuickModuleType) -> Unit,
    onUpdateModuleSize: (QuickModuleType, Float, Int) -> Unit,
    onMoveModule: (QuickModuleType, Boolean) -> Unit,
    onPreviewQuickPanel: () -> Unit,
    onOpenOverlayPermission: () -> Unit
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
            Text("Buka Pratinjau Pusat Kontrol Kaca", style = MaterialTheme.typography.labelLarge)
        }

        StudioSectionTitle("Gantikan Panel Kontrol Bawaan HP")
        StudioToggleRow(
            label = "Aktifkan Pemicu Tepi di Semua Aplikasi",
            checked = config.replaceSystemControlPanel,
            onCheckedChange = { c ->
                onUpdate { it.copy(replaceSystemControlPanel = c) }
                if (c && !isOverlayGranted) onOpenOverlayPermission()
            }
        )
        if (!isOverlayGranted) {
            VitroGlassContainer(
                cornerRadius = 16.dp,
                tintColor = VitroAmber,
                glassOpacity = 0.20f,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(onClick = onOpenOverlayPermission)
            ) {
                Text(
                    text = "Izin 'Tampil di atas aplikasi lain' belum aktif. Ketuk di sini agar geser turun dari tepi atas langsung membuka Pusat Kontrol Vitro menggantikan panel bawaan HP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VitroTextPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EdgePanelTriggerZone.entries.forEach { zone ->
                StudioRadioCard(
                    title = zone.label,
                    subtitle = zone.subtitle,
                    selected = config.edgeTriggerZone == zone,
                    onClick = { onUpdate { it.copy(edgeTriggerZone = zone) } }
                )
            }
        }

        StudioSectionTitle("Orientasi & Gaya Pusat Kontrol")
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

        StudioSliderRow(
            label = "Tinggi Slider Kecerahan & Suara",
            valueText = "${config.sliderHeightDp} dp",
            value = config.sliderHeightDp.toFloat(),
            range = 100f..220f,
            onValueChange = { v -> onUpdate { it.copy(sliderHeightDp = v.roundToInt()) } }
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

        StudioSectionTitle("Atur Modul & Ukuran Bebas (Lebar & Tinggi)")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            config.quickModules.forEach { mod ->
                VitroGlassContainer(
                    cornerRadius = 20.dp,
                    glassOpacity = if (mod.enabled) 0.22f else 0.10f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mod.type.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onMoveModule(mod.type, true) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naik", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onMoveModule(mod.type, false) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turun", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                                }
                                Switch(
                                    checked = mod.enabled,
                                    onCheckedChange = { onToggleModule(mod.type) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
                                )
                            }
                        }

                        if (mod.enabled) {
                            // Free Width Slider (23%..100%)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lebar Modul", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                                Text("${(mod.widthPercent * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = VitroCyanAccent)
                            }
                            Slider(
                                value = mod.widthPercent,
                                onValueChange = { w -> onUpdateModuleSize(mod.type, w, mod.heightDp) },
                                valueRange = 0.23f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroCyanAccent)
                            )

                            // Free Height Slider (56..220dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tinggi Modul", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                                Text("${mod.heightDp} dp", style = MaterialTheme.typography.labelSmall, color = VitroMint)
                            }
                            Slider(
                                value = mod.heightDp.toFloat(),
                                onValueChange = { h -> onUpdateModuleSize(mod.type, mod.widthPercent, h.roundToInt()) },
                                valueRange = 56f..220f,
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroMint)
                            )

                            // Fast shape presets for convenience
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StudioSmallChip("1×1", mod.widthPercent < 0.38f && mod.heightDp < 96) {
                                    onUpdateModuleSize(mod.type, 0.31f, 74)
                                }
                                StudioSmallChip("2×1 Lebar", mod.widthPercent in 0.44f..0.56f && mod.heightDp < 96) {
                                    onUpdateModuleSize(mod.type, 0.48f, 78)
                                }
                                StudioSmallChip("1×2 Tinggi", mod.widthPercent < 0.56f && mod.heightDp >= 130) {
                                    onUpdateModuleSize(mod.type, 0.48f, 152)
                                }
                                StudioSmallChip("Penuh", mod.widthPercent >= 0.92f) {
                                    onUpdateModuleSize(mod.type, 1.0f, 84)
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
        StudioSectionTitle("Status Bar Sistem Android")
        StudioToggleRow(
            label = "Sembunyikan Status Bar Bawaan HP (Anti Tumpuk)",
            checked = config.hideSystemStatusBar,
            onCheckedChange = { c -> onUpdate { it.copy(hideSystemStatusBar = c) } }
        )

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

        if (config.statusPosition != StatusIndicatorPosition.INSIDE_DOCK) {
            StudioSectionTitle("Atur Tata Letak Bebas Indikator (DIY)")
            Text(
                text = "Anda juga bisa tahan lama layar Beranda untuk masuk Mode DIY dan seret indikator langsung dengan jari ke mana saja.",
                style = MaterialTheme.typography.bodySmall,
                color = VitroTextSecondary
            )
            StudioSliderRow(
                label = "Geser Kiri / Kanan (Posisi X)",
                valueText = "${config.statusOffsetXDp.roundToInt()} dp",
                value = config.statusOffsetXDp,
                range = -180f..180f,
                onValueChange = { v -> onUpdate { it.copy(statusOffsetXDp = v) } }
            )
            StudioSliderRow(
                label = "Geser Atas / Bawah (Posisi Y)",
                valueText = "${config.statusOffsetYDp.roundToInt()} dp",
                value = config.statusOffsetYDp,
                range = -40f..680f,
                onValueChange = { v -> onUpdate { it.copy(statusOffsetYDp = v) } }
            )
            StudioSliderRow(
                label = "Ukuran Skala Indikator",
                valueText = "${(config.statusScale * 100).roundToInt()}%",
                value = config.statusScale,
                range = 0.70f..1.50f,
                onValueChange = { v -> onUpdate { it.copy(statusScale = v) } }
            )
            if (config.statusPosition == StatusIndicatorPosition.TOP_STATUS_BAR) {
                StudioSliderRow(
                    label = "Lebar Area Baris Status",
                    valueText = "${(config.statusWidthPercent * 100).roundToInt()}%",
                    value = config.statusWidthPercent,
                    range = 0.45f..1.0f,
                    onValueChange = { v -> onUpdate { it.copy(statusWidthPercent = v) } }
                )
            }
            if (config.statusOffsetXDp != 0f || config.statusOffsetYDp != 0f || config.statusScale != 1.0f || config.statusWidthPercent != 1.0f) {
                OutlinedButton(
                    onClick = {
                        onUpdate {
                            it.copy(
                                statusOffsetXDp = 0f,
                                statusOffsetYDp = 0f,
                                statusScale = 1.0f,
                                statusWidthPercent = 1.0f
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Posisi & Ukuran Indikator", color = VitroCyanAccent)
                }
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
    onSelectSound: (String) -> Unit,
    onPreviewSound: (String) -> Unit,
    onRequestAddCustomSound: () -> Unit,
    onRemoveCustomSound: (String) -> Unit,
    onPreviewHomePopup: () -> Unit,
    onPreviewLockPopup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotificationSoundStudioSection(
            config = config,
            onUpdate = onUpdate,
            onSelectSound = onSelectSound,
            onPreviewSound = onPreviewSound,
            onRequestAddCustomSound = onRequestAddCustomSound,
            onRemoveCustomSound = onRemoveCustomSound
        )

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
    isAccessibilityGranted: Boolean,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onSetLockPasscode: (LockSecurityMode, String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onPreviewLockScreen: () -> Unit
) {
    var selectedModeDraft by remember(config.lockSecurityMode) { mutableStateOf(config.lockSecurityMode) }
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AdaptiveClockStudioCard(
            config = config,
            onUpdate = onUpdate
        )

        StudioSectionTitle("Gestur Kunci Layar Cepat")
        StudioToggleRow(
            label = "Ketuk 2 Kali di Beranda untuk Kunci HP",
            checked = config.doubleTapToLock,
            onCheckedChange = { c ->
                onUpdate { it.copy(doubleTapToLock = c) }
                if (c && !isAccessibilityGranted) onOpenAccessibilitySettings()
            }
        )
        if (config.doubleTapToLock && !isAccessibilityGranted) {
            VitroGlassContainer(
                cornerRadius = 16.dp,
                tintColor = VitroAmber,
                glassOpacity = 0.18f,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(onClick = onOpenAccessibilitySettings)
            ) {
                Text(
                    text = "Aktifkan Layanan Aksesibilitas Vitro (ketuk di sini) agar ketuk 2x benar-benar mematikan & mengunci layar HP Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VitroTextPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Lock Screen Passcode / PIN Creator
        StudioSectionTitle("Keamanan & Sandi Layar Kunci")
        VitroGlassContainer(
            cornerRadius = 20.dp,
            tintColor = if (config.lockSecurityMode != LockSecurityMode.NONE) VitroMint else Color.White,
            glassOpacity = 0.18f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (config.lockSecurityMode == LockSecurityMode.NONE) "Status: Tanpa Sandi"
                        else "Status: ${config.lockSecurityMode.label} Aktif",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (config.lockSecurityMode != LockSecurityMode.NONE) VitroMint else VitroTextPrimary
                    )
                }

                LockSecurityMode.entries.forEach { mode ->
                    StudioRadioCard(
                        title = mode.label,
                        subtitle = mode.subtitle,
                        selected = selectedModeDraft == mode,
                        onClick = {
                            selectedModeDraft = mode
                            newPasscode = ""
                            confirmPasscode = ""
                            statusFeedback = null
                            if (mode == LockSecurityMode.NONE) {
                                onSetLockPasscode(LockSecurityMode.NONE, "")
                                statusFeedback = "Sandi layar kunci dinonaktifkan."
                            }
                        }
                    )
                }

                if (selectedModeDraft != LockSecurityMode.NONE) {
                    val isPin = selectedModeDraft == LockSecurityMode.PIN
                    OutlinedTextField(
                        value = newPasscode,
                        onValueChange = { raw ->
                            val clean = if (isPin) raw.filter { it.isDigit() }.take(6) else raw.take(24)
                            newPasscode = clean
                            statusFeedback = null
                        },
                        label = { Text(if (isPin) "Buat PIN Baru (4–6 angka)" else "Buat Sandi Baru (min. 4 karakter)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VitroCyanAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = VitroTextPrimary,
                            unfocusedTextColor = VitroTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("studio_passcode_input")
                    )
                    OutlinedTextField(
                        value = confirmPasscode,
                        onValueChange = { raw ->
                            val clean = if (isPin) raw.filter { it.isDigit() }.take(6) else raw.take(24)
                            confirmPasscode = clean
                            statusFeedback = null
                        },
                        label = { Text(if (isPin) "Ulangi PIN" else "Ulangi Sandi") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VitroCyanAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = VitroTextPrimary,
                            unfocusedTextColor = VitroTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("studio_passcode_confirm")
                    )
                    Button(
                        onClick = {
                            when {
                                newPasscode.length < 4 -> statusFeedback = "Minimal 4 karakter/digit."
                                newPasscode != confirmPasscode -> statusFeedback = "Konfirmasi sandi tidak cocok."
                                else -> {
                                    onSetLockPasscode(selectedModeDraft, newPasscode)
                                    newPasscode = ""
                                    confirmPasscode = ""
                                    statusFeedback = "Sandi layar kunci berhasil disimpan!"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VitroMint, contentColor = VitroObsidian),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("studio_save_passcode_button")
                    ) {
                        Text("Simpan Sandi Layar Kunci", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (statusFeedback != null) {
                    Text(
                        text = statusFeedback!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (statusFeedback!!.contains("berhasil") || statusFeedback!!.contains("dinonaktifkan")) VitroMint else VitroCoral
                    )
                }
            }
        }

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
            Text("Uji Layar Kunci & Sandi Sekarang", style = MaterialTheme.typography.labelLarge)
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

@Composable
fun ThemeWallpaperStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onOpenWallpaperStudio: () -> Unit = {},
    onSetDefaultLauncher: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onOpenWallpaperStudio,
            colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_open_full_wallpaper_button")
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buka Studio Wallpaper Live, Glass & Galeri", style = MaterialTheme.typography.labelLarge)
        }

        StudioSliderRow(
            label = "Intensitas Efek Pendar (Glow)",
            valueText = "${(config.ambientGlowIntensity * 100).roundToInt()}%",
            value = config.ambientGlowIntensity,
            range = 0f..1f,
            onValueChange = { v -> onUpdate { it.copy(ambientGlowIntensity = v) } }
        )

        StudioSectionTitle("Pilihan Cepat Wallpaper")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperPreset.entries.take(8).forEach { preset ->
                StudioRadioCard(
                    title = preset.label,
                    subtitle = preset.subtitle,
                    selected = config.wallpaperConfig.preset == preset,
                    onClick = {
                        onUpdate { cur ->
                            cur.copy(
                                wallpaperPreset = preset,
                                wallpaperConfig = cur.wallpaperConfig.copy(preset = preset)
                            )
                        }
                    }
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
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
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = VitroTextPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
            )
        }
    }
}
