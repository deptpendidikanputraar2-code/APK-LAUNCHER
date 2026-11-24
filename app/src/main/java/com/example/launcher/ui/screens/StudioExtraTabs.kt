package com.example.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.launcher.model.DynamicIslandStyle
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.SidebarItemType
import com.example.launcher.model.SidebarSide
import com.example.launcher.model.SystemSyncStatus
import com.example.launcher.model.WidgetType
import com.example.launcher.system.AvailableAndroidWidget
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicIslandStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    val di = config.dynamicIsland
    fun updateDi(block: (com.example.launcher.model.DynamicIslandConfig) -> com.example.launcher.model.DynamicIslandConfig) {
        onUpdate { it.copy(dynamicIsland = block(it.dynamicIsland)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioSectionTitle("Dynamic Island Interaktif")
        StudioToggleRow(
            label = "Aktifkan Dynamic Island",
            checked = di.enabled,
            onCheckedChange = { c -> updateDi { it.copy(enabled = c) } }
        )

        if (di.enabled) {
            StudioSectionTitle("Ukuran & Posisi Kamera (Bebas Atur)")
            StudioSliderRow(
                label = "Lebar Dynamic Island",
                valueText = "${di.widthDp.roundToInt()} dp",
                value = di.widthDp,
                range = 108f..280f,
                onValueChange = { v -> updateDi { it.copy(widthDp = v) } }
            )
            StudioSliderRow(
                label = "Tinggi Dynamic Island",
                valueText = "${di.heightDp.roundToInt()} dp",
                value = di.heightDp,
                range = 26f..54f,
                onValueChange = { v -> updateDi { it.copy(heightDp = v) } }
            )
            StudioSliderRow(
                label = "Jarak dari Atas (Posisi Y)",
                valueText = "${di.topOffsetDp.roundToInt()} dp",
                value = di.topOffsetDp,
                range = 0f..56f,
                onValueChange = { v -> updateDi { it.copy(topOffsetDp = v) } }
            )
            StudioSliderRow(
                label = "Geser Kiri / Kanan (Posisi X)",
                valueText = "${di.horizontalOffsetDp.roundToInt()} dp",
                value = di.horizontalOffsetDp,
                range = -90f..90f,
                onValueChange = { v -> updateDi { it.copy(horizontalOffsetDp = v) } }
            )
            StudioSliderRow(
                label = "Kelengkungan Sudut",
                valueText = "${di.cornerRadiusDp.roundToInt()} dp",
                value = di.cornerRadiusDp,
                range = 12f..32f,
                onValueChange = { v -> updateDi { it.copy(cornerRadiusDp = v) } }
            )
            StudioSliderRow(
                label = "Intensitas Pendar Tepi (Glow)",
                valueText = "${(di.borderGlow * 100).roundToInt()}%",
                value = di.borderGlow,
                range = 0f..1f,
                onValueChange = { v -> updateDi { it.copy(borderGlow = v) } }
            )

            StudioSectionTitle("Gaya & Warna Dynamic Island")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DynamicIslandStyle.entries.forEach { style ->
                    val selected = di.style == style
                    VitroGlassContainer(
                        cornerRadius = 18.dp,
                        tintColor = style.containerColor,
                        glassOpacity = if (selected) 0.72f else 0.42f,
                        borderColor = if (selected) style.accentColor else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.bouncyClickable {
                            updateDi { it.copy(style = style) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(style.accentColor)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Text(style.label, style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                        }
                    }
                }
            }

            StudioSectionTitle("Konten & Integrasi Dynamic Island")
            StudioToggleRow("Tampilkan di Layar Kunci", di.showOnLockScreen) { c -> updateDi { it.copy(showOnLockScreen = c) } }
            StudioToggleRow("Notifikasi HP Real-Time", di.showNotifications) { c -> updateDi { it.copy(showNotifications = c) } }
            StudioToggleRow("Kontrol Pemutar Media", di.showMedia) { c -> updateDi { it.copy(showMedia = c) } }
            StudioToggleRow("Status Baterai & Pengisian", di.showBattery) { c -> updateDi { it.copy(showBattery = c) } }
            StudioToggleRow("Jam Ringkas", di.showClock) { c -> updateDi { it.copy(showClock = c) } }
            StudioToggleRow("Timer Fokus Aktif", di.showFocusTimer) { c -> updateDi { it.copy(showFocusTimer = c) } }
            StudioToggleRow("Melebar Otomatis Saat Notifikasi Masuk", di.autoExpandOnNotification) { c -> updateDi { it.copy(autoExpandOnNotification = c) } }
            StudioToggleRow("Melebar Saat Status Sistem Berubah (Cas/Senter/Suara)", di.autoExpandOnSystemStatus) { c -> updateDi { it.copy(autoExpandOnSystemStatus = c) } }
            StudioToggleRow("Gelembung Ganda iPhone 18 Pro Max (2 Aktivitas)", di.showDualSplitBubble) { c -> updateDi { it.copy(showDualSplitBubble = c) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SidebarStudioTab(
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onToggleSidebarItem: (String) -> Unit,
    onMoveSidebarItem: (String, Boolean) -> Unit,
    onAddAppToSidebar: (String) -> Unit,
    onRemoveSidebarItem: (String) -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioSectionTitle("Pengaturan Sidebar Kaca v7")
        StudioToggleRow(
            label = "Tampilkan Pil Sidebar di Atas Dock",
            checked = config.showGlassySidebar,
            onCheckedChange = { c -> onUpdate { it.copy(showGlassySidebar = c) } }
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

            StudioSliderRow(
                label = "Lebar Sidebar Saat Dibuka",
                valueText = "${config.sidebarWidthDp.roundToInt()} dp",
                value = config.sidebarWidthDp,
                range = 180f..270f,
                onValueChange = { v -> onUpdate { it.copy(sidebarWidthDp = v) } }
            )

            StudioSliderRow(
                label = "Kepekatan Kaca Sidebar",
                valueText = "${(config.sidebarGlassOpacity * 100).roundToInt()}%",
                value = config.sidebarGlassOpacity,
                range = 0.45f..0.96f,
                onValueChange = { v -> onUpdate { it.copy(sidebarGlassOpacity = v) } }
            )

            StudioSectionTitle("Atur Isi & Urutan Menu Sidebar")
            Button(
                onClick = { showAppPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent, contentColor = VitroObsidian),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_add_sidebar_app_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Aplikasi HP ke Sidebar", style = MaterialTheme.typography.labelLarge)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                config.sidebarItems.forEach { item ->
                    val app = item.packageName?.let { pkg -> installedApps.firstOrNull { it.packageName == pkg } }
                    val displayLabel = if (item.type == SidebarItemType.APP_SHORTCUT) {
                        app?.label ?: item.packageName.orEmpty().substringAfterLast('.')
                    } else item.type.defaultLabel

                    VitroGlassContainer(
                        cornerRadius = 18.dp,
                        glassOpacity = if (item.enabled) 0.24f else 0.12f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (app?.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = displayLabel,
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column {
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VitroTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.type == SidebarItemType.APP_SHORTCUT) "Aplikasi Pintasan" else "Menu Sistem",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VitroTextSecondary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onMoveSidebarItem(item.id, true) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naik", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onMoveSidebarItem(item.id, false) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turun", tint = VitroTextSecondary, modifier = Modifier.size(18.dp))
                                }
                                if (item.type == SidebarItemType.APP_SHORTCUT) {
                                    IconButton(onClick = { onRemoveSidebarItem(item.id) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = VitroCoral, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { onToggleSidebarItem(item.id) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = VitroObsidian, checkedTrackColor = VitroCyanAccent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        SearchableAppPickerDialog(
            title = "Pilih Aplikasi untuk Sidebar",
            apps = installedApps,
            selectedPackages = config.sidebarItems.mapNotNull { it.packageName }.toSet(),
            onPickApp = { app ->
                onAddAppToSidebar(app.packageName)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
fun SystemSyncStudioTab(
    syncStatus: SystemSyncStatus,
    onRefreshStatus: () -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onOpenDndPolicy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioSectionTitle("Sinkronisasi Penuh dengan HP")
            VitroGlassContainer(
                cornerRadius = 50.dp,
                glassOpacity = 0.22f,
                modifier = Modifier.bouncyClickable(onClick = onRefreshStatus)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Segarkan", tint = VitroCyanAccent, modifier = Modifier.size(15.dp))
                    Text("Cek Ulang", style = MaterialTheme.typography.labelMedium, color = VitroTextPrimary)
                }
            }
        }

        Text(
            text = "Aktifkan izin di bawah agar notifikasi HP, ketuk 2x kunci layar, kecerahan sistem, dan penggantian Pusat Kontrol bawaan berjalan penuh di perangkat Anda.",
            style = MaterialTheme.typography.bodySmall,
            color = VitroTextSecondary
        )

        SyncPermissionRow(
            title = "1. Launcher Utama HP",
            subtitle = "Jadikan Vitro Launcher sebagai beranda default saat menekan tombol Home",
            granted = syncStatus.isDefaultLauncher,
            onClick = onOpenDefaultLauncher
        )
        SyncPermissionRow(
            title = "2. Akses Notifikasi HP Nyata",
            subtitle = "Menampilkan pesan WhatsApp, SMS, Media & aplikasi di Home, Lock Screen & Dynamic Island",
            granted = syncStatus.isNotificationListenerEnabled,
            onClick = onOpenNotificationAccess
        )
        SyncPermissionRow(
            title = "3. Ketuk 2x Kunci Layar & Screenshot",
            subtitle = "Aktifkan Layanan Aksesibilitas Vitro untuk mengunci HP nyata & tangkapan layar",
            granted = syncStatus.isAccessibilityEnabled,
            onClick = onOpenAccessibility
        )
        SyncPermissionRow(
            title = "4. Ganti Panel Kontrol Bawaan HP",
            subtitle = "Izin 'Tampil di atas aplikasi lain' agar geser tepi kanan atas membuka Pusat Kontrol Vitro di semua aplikasi",
            granted = syncStatus.isOverlayPermissionGranted,
            onClick = onOpenOverlayPermission
        )
        SyncPermissionRow(
            title = "5. Kecerahan & Rotasi Layar Sistem",
            subtitle = "Mengubah tingkat kecerahan & auto-rotasi HP secara nyata dari Pusat Kontrol",
            granted = syncStatus.isWriteSettingsGranted,
            onClick = onOpenWriteSettings
        )
        SyncPermissionRow(
            title = "6. Kontrol Jangan Ganggu (DND)",
            subtitle = "Aktifkan/matikan mode Jangan Ganggu HP langsung dari Pusat Kontrol",
            granted = syncStatus.isDndPolicyGranted,
            onClick = onOpenDndPolicy
        )
    }
}

@Composable
private fun SyncPermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 20.dp,
        tintColor = if (granted) VitroMint else VitroAmber,
        glassOpacity = if (granted) 0.18f else 0.16f,
        borderColor = if (granted) VitroMint.copy(alpha = 0.55f) else VitroAmber.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (granted) VitroMint else VitroAmber,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (granted) VitroMint.copy(alpha = 0.25f) else VitroAmber.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (granted) "Aktif" else "Aktifkan",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (granted) VitroMint else VitroAmber
                )
            }
        }
    }
}

@Composable
fun SearchableAppPickerDialog(
    title: String,
    apps: List<InstalledApp>,
    selectedPackages: Set<String>,
    onPickApp: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        VitroGlassContainer(
            cornerRadius = 28.dp,
            tintColor = Color(0xFF0A1228),
            glassOpacity = 0.94f,
            borderColor = VitroCyanAccent.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.76f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = VitroTextPrimary)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari aplikasi…", color = VitroTextSecondary) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = VitroCyanAccent) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VitroCyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                        focusedTextColor = VitroTextPrimary,
                        unfocusedTextColor = VitroTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        val alreadyAdded = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = if (alreadyAdded) 0.14f else 0.06f))
                                .clickable { onPickApp(app) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = app.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                }
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VitroTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (alreadyAdded) {
                                Icon(Icons.Rounded.Check, contentDescription = "Terpilih", tint = VitroMint)
                            } else {
                                Icon(Icons.Rounded.Add, contentDescription = "Tambah", tint = VitroCyanAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetAddPickerDialog(
    homePageCount: Int,
    androidWidgets: List<AvailableAndroidWidget>,
    onAddBuiltInWidget: (WidgetType, Int) -> Unit,
    onPickAndroidWidget: (AvailableAndroidWidget, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Vitro Glass, 1 = Real HP Android Widgets
    var targetPage by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val filteredAndroid = remember(androidWidgets, query) {
        if (query.isBlank()) androidWidgets
        else androidWidgets.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.widgetLabel.contains(query, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        VitroGlassContainer(
            cornerRadius = 28.dp,
            tintColor = Color(0xFF0A1228),
            glassOpacity = 0.95f,
            borderColor = VitroCyanAccent.copy(alpha = 0.48f),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tambah Widget ke Beranda", style = MaterialTheme.typography.titleMedium, color = VitroTextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = VitroTextPrimary)
                    }
                }

                // Target Page selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Halaman:", style = MaterialTheme.typography.labelLarge, color = VitroTextSecondary)
                    repeat(homePageCount.coerceIn(1, 7)) { idx ->
                        val selected = targetPage == idx
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.12f))
                                .clickable { targetPage = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) VitroObsidian else VitroTextPrimary
                            )
                        }
                    }
                }

                // Source Switcher: Vitro Glass vs Real Android HP Widgets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioSmallChip(
                        text = "Widget Kaca Vitro",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    StudioSmallChip(
                        text = "Widget Asli HP (${androidWidgets.size})",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(WidgetType.entries) { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        onAddBuiltInWidget(type, targetPage)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Widgets, contentDescription = null, tint = VitroCyanAccent)
                                    Column {
                                        Text(type.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                                        Text(type.subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
                                    }
                                }
                                Icon(Icons.Rounded.Add, contentDescription = "Tambah", tint = VitroCyanAccent)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari widget aplikasi HP…", color = VitroTextSecondary) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = VitroCyanAccent) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VitroCyanAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                            focusedTextColor = VitroTextPrimary,
                            unfocusedTextColor = VitroTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredAndroid.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada widget sistem Android yang ditemukan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VitroTextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAndroid, key = { it.providerInfo.provider.flattenToString() }) { widget ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            onPickAndroidWidget(widget, targetPage)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (widget.previewIcon != null) {
                                            Image(
                                                bitmap = widget.previewIcon,
                                                contentDescription = widget.widgetLabel,
                                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Widgets, contentDescription = null, tint = VitroMint)
                                        }
                                        Column {
                                            Text(
                                                text = widget.widgetLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = VitroTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = widget.appName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VitroTextSecondary
                                            )
                                        }
                                    }
                                    Icon(Icons.Rounded.Add, contentDescription = "Tambah", tint = VitroMint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
