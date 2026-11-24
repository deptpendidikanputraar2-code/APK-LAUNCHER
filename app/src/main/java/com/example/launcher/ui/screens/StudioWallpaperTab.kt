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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.WallpaperCategory
import com.example.launcher.model.WallpaperColorTone
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WallpaperGlassStyle
import com.example.launcher.model.WallpaperParticleOverlay
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.system.SystemWallpaperTarget
import com.example.launcher.ui.components.VitroAdvancedWallpaperBackdrop
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperStudioTab(
    config: LauncherConfig,
    wallpaperFeedback: String?,
    onUpdateHomeWallpaper: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onUpdateLockWallpaper: ((WallpaperConfig) -> WallpaperConfig) -> Unit,
    onSetSeparateLockWallpaper: (Boolean) -> Unit,
    onUpdateAmbientGlow: (Float) -> Unit,
    onPickGalleryPhoto: (isLockScreen: Boolean) -> Unit,
    onApplyToSystemWallpaper: (WallpaperConfig, SystemWallpaperTarget) -> Unit
) {
    var editingLockScreen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(WallpaperCategory.ALL) }

    val activeWallpaper = if (editingLockScreen && config.separateLockWallpaper) {
        config.lockWallpaperConfig
    } else if (editingLockScreen) {
        config.lockWallpaperConfig
    } else {
        config.wallpaperConfig
    }

    fun updateTarget(transform: (WallpaperConfig) -> WallpaperConfig) {
        if (editingLockScreen) {
            if (!config.separateLockWallpaper) onSetSeparateLockWallpaper(true)
            onUpdateLockWallpaper(transform)
        } else {
            onUpdateHomeWallpaper(transform)
        }
    }

    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == WallpaperCategory.ALL) {
            WallpaperPreset.entries
        } else {
            WallpaperPreset.entries.filter { it.category == selectedCategory }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Target Screen Selector (Beranda vs Layar Kunci)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WallpaperTargetChip(
                label = "Wallpaper Beranda",
                icon = Icons.Rounded.Home,
                selected = !editingLockScreen,
                onClick = { editingLockScreen = false },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wp_target_home")
            )
            WallpaperTargetChip(
                label = "Wallpaper Layar Kunci",
                icon = Icons.Rounded.Lock,
                selected = editingLockScreen,
                onClick = {
                    editingLockScreen = true
                    if (!config.separateLockWallpaper) onSetSeparateLockWallpaper(true)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wp_target_lock")
            )
        }

        StudioToggleRow(
            label = "Gunakan Wallpaper Berbeda untuk Layar Kunci",
            checked = config.separateLockWallpaper,
            onCheckedChange = { enabled ->
                onSetSeparateLockWallpaper(enabled)
                if (!enabled) editingLockScreen = false
            }
        )

        // Live Real-Time Wallpaper Preview Frame
        VitroGlassContainer(
            cornerRadius = 26.dp,
            glassOpacity = 0.16f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.2.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                ) {
                    VitroAdvancedWallpaperBackdrop(
                        wallpaperConfig = activeWallpaper,
                        glowIntensity = config.ambientGlowIntensity,
                        enableSensorParallax = false,
                        isMiniPreview = false,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top live badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA040814))
                            .border(1.dp, VitroCyanAccent.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = VitroCyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (editingLockScreen) "Pratinjau Layar Kunci" else "Pratinjau Beranda",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Mini mock clock & glass dock overlay so user immediately sees icon/text contrast
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "09:41",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${activeWallpaper.preset.label} • ${activeWallpaper.glassStyle.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    if (!editingLockScreen) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(VitroCyanAccent, VitroIndigo, VitroMint, VitroAmber).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(c.copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                }

                // Quick Actions: Gallery Photo Picker + Shuffle Aesthetic Combo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPickGalleryPhoto(editingLockScreen) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VitroCyanAccent,
                            contentColor = VitroObsidian
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("wp_pick_gallery_button")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Foto Galeri HP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    VitroGlassContainer(
                        cornerRadius = 16.dp,
                        tintColor = VitroIndigo,
                        glassOpacity = 0.32f,
                        borderColor = VitroCyanAccent.copy(alpha = 0.55f),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .bouncyClickable {
                                val pool = WallpaperPreset.entries.filter {
                                    it != WallpaperPreset.SYSTEM_LIVE && it != WallpaperPreset.CUSTOM_PHOTO
                                }
                                val randomPreset = pool.random()
                                val randomGlass = WallpaperGlassStyle.entries.random()
                                val randomParticle = WallpaperParticleOverlay.entries.random()
                                val randomTone = WallpaperColorTone.entries.random()
                                updateTarget {
                                    it.copy(
                                        preset = randomPreset,
                                        glassStyle = randomGlass,
                                        particleOverlay = randomParticle,
                                        colorTone = randomTone,
                                        glassIntensity = 0.42f
                                    )
                                }
                            }
                            .testTag("wp_shuffle_combo_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Acak Gaya", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }

        // Category Filter Pills
        StudioSectionTitle("Galeri Wallpaper Live, Glass & HD")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperCategory.entries.forEach { cat ->
                StudioSmallChip(
                    text = cat.label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        // 2-Column Visual Wallpaper Preset Grid with Live Mini Previews
        val rows = remember(filteredPresets) { filteredPresets.chunked(2) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = activeWallpaper.preset == preset
                        WallpaperPresetTileCard(
                            preset = preset,
                            customImagePath = activeWallpaper.customImagePath,
                            selected = isSelected,
                            onClick = {
                                if (preset == WallpaperPreset.CUSTOM_PHOTO && activeWallpaper.customImagePath.isBlank()) {
                                    onPickGalleryPhoto(editingLockScreen)
                                } else {
                                    updateTarget { it.copy(preset = preset) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Modern Glass Lens Effects (works on Gallery photos, HD, AND Live!)
        StudioSectionTitle("Efek Lensa Kaca (Glass Overlay)")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperGlassStyle.entries.forEach { style ->
                StudioRadioCard(
                    title = style.label,
                    subtitle = style.subtitle,
                    selected = activeWallpaper.glassStyle == style,
                    onClick = { updateTarget { it.copy(glassStyle = style) } }
                )
            }
        }

        if (activeWallpaper.glassStyle != WallpaperGlassStyle.NONE) {
            StudioSliderRow(
                label = "Intensitas Efek Kaca",
                valueText = "${(activeWallpaper.glassIntensity * 100).roundToInt()}%",
                value = activeWallpaper.glassIntensity,
                range = 0.05f..1.0f,
                onValueChange = { v -> updateTarget { it.copy(glassIntensity = v) } }
            )
        }

        // Live Particle / Bokeh Overlay Selector
        StudioSectionTitle("Efek Partikel & Bokeh Live")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperParticleOverlay.entries.forEach { overlay ->
                StudioSmallChip(
                    text = overlay.label,
                    selected = activeWallpaper.particleOverlay == overlay,
                    onClick = { updateTarget { it.copy(particleOverlay = overlay) } }
                )
            }
        }

        // Color Tone Atmosphere Grading
        StudioSectionTitle("Filter Warna & Atmosfer")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperColorTone.entries.forEach { tone ->
                val isSelected = activeWallpaper.colorTone == tone
                VitroGlassContainer(
                    cornerRadius = 50.dp,
                    tintColor = if (isSelected) VitroCyanAccent else Color.White,
                    glassOpacity = if (isSelected) 0.34f else 0.14f,
                    borderColor = if (isSelected) VitroCyanAccent else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.bouncyClickable { updateTarget { it.copy(colorTone = tone) } }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tone.tint == Color.Transparent) {
                                        Brush.linearGradient(listOf(VitroCyanAccent, VitroIndigo, VitroAmber))
                                    } else {
                                        Brush.linearGradient(listOf(tone.tint, tone.tint.copy(alpha = 0.7f)))
                                    }
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        )
                        Text(tone.label, style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                    }
                }
            }
        }

        // Precision Controls: Blur, Dimming, Glow, Live Speed & 3D Gyro Parallax
        StudioSectionTitle("Kontrol Kedalaman, Blur & Gerak 3D")
        StudioSliderRow(
            label = "Blur Latar (Efek Kedalaman Kaca)",
            valueText = "${activeWallpaper.blurRadiusDp.roundToInt()} dp",
            value = activeWallpaper.blurRadiusDp,
            range = 0f..24f,
            onValueChange = { v -> updateTarget { it.copy(blurRadiusDp = v) } }
        )

        StudioSliderRow(
            label = "Kegelapan Latar (Kontras Ikon & Jam)",
            valueText = "${(activeWallpaper.dimAlpha * 100).roundToInt()}%",
            value = activeWallpaper.dimAlpha,
            range = 0f..0.70f,
            onValueChange = { v -> updateTarget { it.copy(dimAlpha = v) } }
        )

        StudioSliderRow(
            label = "Intensitas Efek Pendar (Ambient Glow)",
            valueText = "${(config.ambientGlowIntensity * 100).roundToInt()}%",
            value = config.ambientGlowIntensity,
            range = 0f..1f,
            onValueChange = onUpdateAmbientGlow
        )

        if (activeWallpaper.preset.isLive ||
            activeWallpaper.particleOverlay != WallpaperParticleOverlay.NONE ||
            activeWallpaper.glassStyle == WallpaperGlassStyle.LIQUID_SHIMMER
        ) {
            StudioSliderRow(
                label = "Kecepatan Animasi Live",
                valueText = "${"%.1f".format(activeWallpaper.liveSpeed)}x",
                value = activeWallpaper.liveSpeed,
                range = 0.3f..2.0f,
                onValueChange = { v -> updateTarget { it.copy(liveSpeed = v) } }
            )
        }

        StudioToggleRow(
            label = "Paralaks 3D Giroskop (Bergerak Saat HP Dimiringkan)",
            checked = activeWallpaper.parallaxEnabled,
            onCheckedChange = { c -> updateTarget { it.copy(parallaxEnabled = c) } }
        )

        if (activeWallpaper.parallaxEnabled) {
            StudioSliderRow(
                label = "Kekuatan Gerak Paralaks 3D",
                valueText = "${(activeWallpaper.parallaxStrength * 100).roundToInt()}%",
                value = activeWallpaper.parallaxStrength,
                range = 0.1f..1.0f,
                onValueChange = { v -> updateTarget { it.copy(parallaxStrength = v) } }
            )
        }

        // Apply directly to Android System WallpaperManager
        StudioSectionTitle("Sinkron ke Wallpaper Sistem Android")
        VitroGlassContainer(
            cornerRadius = 20.dp,
            glassOpacity = 0.16f,
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
                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = VitroMint, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Terapkan wallpaper terpilih beserta efek kaca & filter ke sistem HP asli Anda:",
                        style = MaterialTheme.typography.bodySmall,
                        color = VitroTextSecondary
                    )
                }

                if (!wallpaperFeedback.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VitroMint.copy(alpha = 0.22f))
                            .border(1.dp, VitroMint.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = wallpaperFeedback,
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroMint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemWallpaperTarget.entries.forEach { target ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VitroMint.copy(alpha = 0.20f))
                                .border(1.dp, VitroMint.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                .clickable { onApplyToSystemWallpaper(activeWallpaper, target) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("wp_apply_system_${target.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (target) {
                                    SystemWallpaperTarget.HOME -> "Beranda HP"
                                    SystemWallpaperTarget.LOCK -> "Kunci HP"
                                    SystemWallpaperTarget.BOTH -> "Keduanya"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = VitroTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperTargetChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VitroGlassContainer(
        cornerRadius = 18.dp,
        tintColor = if (selected) VitroCyanAccent else Color.White,
        glassOpacity = if (selected) 0.34f else 0.14f,
        borderColor = if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.25f),
        modifier = modifier.bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else VitroCyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WallpaperPresetTileCard(
    preset: WallpaperPreset,
    customImagePath: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewCfg = remember(preset, customImagePath) {
        WallpaperConfig(
            preset = preset,
            customImagePath = customImagePath,
            glassStyle = WallpaperGlassStyle.NONE,
            particleOverlay = WallpaperParticleOverlay.NONE,
            dimAlpha = 0.08f,
            parallaxEnabled = false
        )
    }

    Box(
        modifier = modifier
            .height(146.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp)
            )
            .bouncyClickable(onClick = onClick)
            .testTag("wp_preset_${preset.name.lowercase()}")
    ) {
        if (preset == WallpaperPreset.SYSTEM_LIVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B1120))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = VitroCyanAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(34.dp)
                )
            }
        } else if (preset == WallpaperPreset.CUSTOM_PHOTO && customImagePath.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0E2A47), Color(0xFF1E1B4B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = VitroCyanAccent,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ketuk Pilih Foto", style = MaterialTheme.typography.labelSmall, color = VitroTextPrimary)
                }
            }
        } else {
            VitroAdvancedWallpaperBackdrop(
                wallpaperConfig = previewCfg,
                glowIntensity = 0f,
                enableSensorParallax = false,
                isMiniPreview = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // LIVE / GLASS badge
        if (preset.isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC040814))
                    .border(1.dp, VitroCyanAccent.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (preset.category == WallpaperCategory.GLASS) "GLASS" else "LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VitroCyanAccent
                )
            }
        }

        // Selected check badge
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(VitroCyanAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Aktif", tint = VitroObsidian, modifier = Modifier.size(15.dp))
            }
        }

        // Bottom frosted title bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xDD040814))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = VitroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = VitroTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
