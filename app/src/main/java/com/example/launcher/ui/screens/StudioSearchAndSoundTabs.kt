package com.example.launcher.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcher.model.BuiltInNotifSound
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LockClockColor
import com.example.launcher.model.LockClockStyle
import com.example.launcher.model.SearchBarColorPreset
import com.example.launcher.model.SearchBarPosition
import com.example.launcher.model.SearchBarShape
import com.example.launcher.ui.components.VitroCustomizableSearchBar
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlin.math.roundToInt

/**
 * Dedicated Studio tab to customize the Search Bar across ALL menus (Home Screen, App Drawer & Studio).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchBarStudioTab(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    val sb = config.searchBar
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Preview of Search Bar
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pratinjau Bilah Pencarian (Berlaku di Semua Menu)",
                    style = MaterialTheme.typography.labelMedium,
                    color = VitroCyanAccent,
                    fontWeight = FontWeight.Bold
                )
                VitroCustomizableSearchBar(
                    searchConfig = sb,
                    query = "",
                    readOnly = true,
                    placeholder = "Cari aplikasi, widget, pengaturan...",
                    modifier = Modifier.fillMaxWidth(sb.widthPercent.coerceIn(0.55f, 1.0f))
                )
            }
        }

        // Toggle Home Screen Search Bar
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tampilkan Pencarian di Beranda", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                    Text("Tekan lama pencarian di Beranda untuk mode DIY geser & ubah ukuran bebas", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                }
                Switch(
                    checked = sb.enabledOnHome,
                    onCheckedChange = { checked ->
                        onUpdate { it.copy(searchBar = it.searchBar.copy(enabledOnHome = checked)) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VitroCyanAccent)
                )
            }
        }

        // Position selector
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Posisi Pencarian di Beranda", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBarPosition.entries.forEach { pos ->
                        val selected = sb.position == pos
                        StudioChipPill(
                            label = pos.label,
                            selected = selected,
                            onClick = { onUpdate { it.copy(searchBar = it.searchBar.copy(position = pos)) } }
                        )
                    }
                }
            }
        }

        // Shape selector
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bentuk Bilah Pencarian", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBarShape.entries.forEach { shape ->
                        val selected = sb.shape == shape
                        StudioChipPill(
                            label = shape.label,
                            selected = selected,
                            onClick = { onUpdate { it.copy(searchBar = it.searchBar.copy(shape = shape)) } }
                        )
                    }
                }
            }
        }

        // Color preset selector
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Warna Bilah Pencarian", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBarColorPreset.entries.forEach { preset ->
                        val selected = sb.colorPreset == preset
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) VitroCyanAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.16f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onUpdate { it.copy(searchBar = it.searchBar.copy(colorPreset = preset)) } }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(preset.bgColor)
                                    .border(1.dp, preset.borderColor, CircleShape)
                            )
                            Text(preset.label, style = MaterialTheme.typography.labelSmall, color = VitroTextPrimary)
                        }
                    }
                }
            }
        }

        // Sliders: Opacity, Width, Height
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ukuran & Transparansi", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                CompactStudioSlider(
                    label = "Transparansi Kaca",
                    valueText = "${(sb.opacity * 100).roundToInt()}%",
                    value = sb.opacity,
                    range = 0.08f..0.85f,
                    onChange = { v -> onUpdate { it.copy(searchBar = it.searchBar.copy(opacity = v)) } }
                )
                CompactStudioSlider(
                    label = "Lebar Pencarian",
                    valueText = "${(sb.widthPercent * 100).roundToInt()}%",
                    value = sb.widthPercent,
                    range = 0.50f..1.0f,
                    onChange = { v -> onUpdate { it.copy(searchBar = it.searchBar.copy(widthPercent = v)) } }
                )
                CompactStudioSlider(
                    label = "Tinggi Pencarian",
                    valueText = "${sb.heightDp} dp",
                    value = sb.heightDp.toFloat(),
                    range = 40f..68f,
                    onChange = { v -> onUpdate { it.copy(searchBar = it.searchBar.copy(heightDp = v.roundToInt())) } }
                )
            }
        }
    }
}

/**
 * Aesthetic Notification Sound Picker + Native Notification Replacement settings.
 * Allows previewing 6 studio-grade built-in glass chimes AND adding unlimited custom audio sounds from phone.
 */
@Composable
fun NotificationSoundStudioSection(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onSelectSound: (String) -> Unit,
    onPreviewSound: (String) -> Unit,
    onRequestAddCustomSound: () -> Unit,
    onRemoveCustomSound: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Native Notification & System-Wide Dynamic Island Replacement toggles
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.18f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pengganti Notifikasi Bawaan HP",
                    style = MaterialTheme.typography.titleSmall,
                    color = VitroCyanAccent,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gantikan Notifikasi & Suara HP", style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                        Text("Notifikasi masuk memutar suara estetik Vitro & tampil di Dynamic Island", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                    }
                    Switch(
                        checked = config.replaceSystemNotifications,
                        onCheckedChange = { checked -> onUpdate { it.copy(replaceSystemNotifications = checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VitroCyanAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dynamic Island Notifikasi Lintas Aplikasi", style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                        Text("Tampilkan kapsul notifikasi iPhone 18 Pro di atas aplikasi lain", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                    }
                    Switch(
                        checked = config.showSystemWideDynamicIsland,
                        onCheckedChange = { checked -> onUpdate { it.copy(showSystemWideDynamicIsland = checked) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VitroCyanAccent)
                    )
                }
            }
        }

        // Aesthetic Built-in + Custom Sound Picker
        VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.16f, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Suara Notifikasi Estetik", style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                        Text("Ketuk untuk memilih & dengarkan pratinjau langsung", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                    }
                    Button(
                        onClick = onRequestAddCustomSound,
                        colors = ButtonDefaults.buttonColors(containerColor = VitroCyanAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF040814), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Audio", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF040814))
                    }
                }

                // Built-in aesthetic chimes
                BuiltInNotifSound.entries.forEach { sound ->
                    val selected = config.activeNotifSoundId == sound.id
                    SoundChoiceRow(
                        title = sound.label,
                        subtitle = sound.subtitle,
                        selected = selected,
                        isCustom = false,
                        onSelect = {
                            onSelectSound(sound.id)
                            onPreviewSound(sound.id)
                        },
                        onPreview = { onPreviewSound(sound.id) },
                        onDelete = null
                    )
                }

                // User-added custom audio files
                if (config.customNotifSounds.isNotEmpty()) {
                    Text(
                        text = "Suara Kustom Dari HP Anda",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroMint,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    config.customNotifSounds.forEach { custom ->
                        val selected = config.activeNotifSoundId == custom.id
                        SoundChoiceRow(
                            title = custom.name,
                            subtitle = "Audio kustom perangkat",
                            selected = selected,
                            isCustom = true,
                            onSelect = {
                                onSelectSound(custom.id)
                                onPreviewSound(custom.id)
                            },
                            onPreview = { onPreviewSound(custom.id) },
                            onDelete = { onRemoveCustomSound(custom.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    isCustom: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) VitroCyanAccent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else if (isCustom) Icons.Rounded.MusicNote else Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = if (selected) VitroCyanAccent else VitroTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar Pratinjau", tint = VitroCyanAccent, modifier = Modifier.size(20.dp))
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Hapus Suara", tint = VitroCoral, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Compact Adaptive Clock & Lock Screen Replacement card shown in Studio -> Halaman & Sandi.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveClockStudioCard(
    config: LauncherConfig,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit
) {
    VitroGlassContainer(cornerRadius = 22.dp, glassOpacity = 0.18f, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Pengganti Layar Kunci & Adaptive Clock",
                style = MaterialTheme.typography.titleSmall,
                color = VitroCyanAccent,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gantikan Layar Kunci Bawaan HP", style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                    Text("Otomatis tampilkan Layar Kunci Vitro saat layar HP dinyalakan", style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                }
                Switch(
                    checked = config.replaceSystemLockScreen,
                    onCheckedChange = { checked -> onUpdate { it.copy(replaceSystemLockScreen = checked) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VitroCyanAccent)
                )
            }

            Text("Model Jam Layar Kunci (Bisa diubah langsung di Layar Kunci)", style = MaterialTheme.typography.labelMedium, color = VitroTextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockClockStyle.entries.forEach { style ->
                    StudioChipPill(
                        label = style.label,
                        selected = config.lockClockStyle == style,
                        onClick = { onUpdate { it.copy(lockClockStyle = style) } }
                    )
                }
            }

            Text("Warna Jam", style = MaterialTheme.typography.labelMedium, color = VitroTextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockClockColor.entries.forEach { color ->
                    StudioChipPill(
                        label = color.label,
                        selected = config.lockClockColor == color,
                        onClick = { onUpdate { it.copy(lockClockColor = color) } }
                    )
                }
            }

            CompactStudioSlider(
                label = "Ukuran Jam Layar Kunci",
                valueText = "${(config.lockClockScale * 100).roundToInt()}%",
                value = config.lockClockScale,
                range = 0.55f..1.85f,
                onChange = { s -> onUpdate { it.copy(lockClockScale = s) } }
            )
        }
    }
}

@Composable
private fun StudioChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) VitroCyanAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) VitroCyanAccent else VitroTextPrimary
        )
    }
}

@Composable
private fun CompactStudioSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = VitroCyanAccent, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VitroCyanAccent),
            modifier = Modifier.height(30.dp)
        )
    }
}
