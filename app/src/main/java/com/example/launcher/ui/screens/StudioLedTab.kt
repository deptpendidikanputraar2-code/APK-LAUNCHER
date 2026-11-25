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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.launcher.model.BlackBerryLedConfig
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LedBlinkPattern
import com.example.launcher.model.LedColorPreset
import com.example.launcher.model.LedIndicatorTrigger
import com.example.launcher.model.LedLampConfig
import com.example.launcher.model.LedPlacement
import com.example.launcher.ui.components.BlackBerryLedDiode
import com.example.launcher.ui.components.BlackBerryLedEvaluator
import com.example.launcher.ui.components.SamsungCameraRingLed
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroAmber
import com.example.ui.theme.VitroCoral
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroMint
import com.example.ui.theme.VitroObsidian
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val DurationPresetsSeconds = listOf(
    0 to "Terus",
    5 to "5 dtk",
    15 to "15 dtk",
    30 to "30 dtk",
    60 to "1 mnt",
    180 to "3 mnt",
    300 to "5 mnt",
    600 to "10 mnt"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlackBerryLedStudioTab(
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    onUpdate: ((LauncherConfig) -> LauncherConfig) -> Unit,
    onAddLedAppRule: (String, Long) -> Unit,
    onUpdateLedAppColor: (String, Long) -> Unit,
    onRemoveLedAppRule: (String) -> Unit,
    onTestLed: (Int) -> Unit
) {
    val led = config.ledConfig
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = LED 1 App, 1 = LED 2 Indicator, 2 = Posisi
    var showAppPicker by remember { mutableStateOf(false) }

    fun updateLed(block: (BlackBerryLedConfig) -> BlackBerryLedConfig) {
        onUpdate { it.copy(ledConfig = block(it.ledConfig)) }
    }

    // Live animation clock for the real-time BlackBerry LED preview card inside Studio
    var previewNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(led.masterEnabled, led.appLed.enabled, led.indicatorLed.enabled) {
        if (!led.masterEnabled) return@LaunchedEffect
        while (true) {
            previewNow = System.currentTimeMillis()
            delay(36L)
        }
    }

    val previewIntensity1 = if (led.masterEnabled && led.appLed.enabled) {
        BlackBerryLedEvaluator.computeIntensity(led.appLed.pattern, led.appLed.blinkIntervalMs, previewNow)
    } else 0f

    val previewIntensity2 = if (led.masterEnabled && led.indicatorLed.enabled) {
        BlackBerryLedEvaluator.computeIntensity(led.indicatorLed.pattern, led.indicatorLed.blinkIntervalMs, previewNow)
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioSectionTitle("Lampu LED Notifikasi Ganda ala BlackBerry")

        StudioToggleRow(
            label = "Aktifkan Lampu LED BlackBerry",
            checked = led.masterEnabled,
            onCheckedChange = { c -> updateLed { it.copy(masterEnabled = c) } }
        )

        // Live Realistic BlackBerry Hardware Bezel Preview Card
        VitroGlassContainer(
            cornerRadius = 24.dp,
            tintColor = Color(0xFF040711),
            glassOpacity = 0.86f,
            borderColor = VitroCyanAccent.copy(alpha = 0.42f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_led_live_preview_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (led.placement == LedPlacement.CAMERA_RING_A56) {
                                "Pratinjau Cincin Kamera A56"
                            } else {
                                "Pratinjau Live LED BlackBerry"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "Posisi: ${led.placement.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VitroTextSecondary
                        )
                    }
                    if (led.placement == LedPlacement.CAMERA_RING_A56) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFF020409))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SamsungCameraRingLed(
                                led1Active = led.masterEnabled && led.appLed.enabled,
                                led1Color = Color(led.appLed.colorHex),
                                led1Intensity = previewIntensity1,
                                led1Glow = led.appLed.glowIntensity,
                                led2Active = led.masterEnabled && led.indicatorLed.enabled,
                                led2Color = Color(led.indicatorLed.colorHex),
                                led2Intensity = previewIntensity2,
                                led2Glow = led.indicatorLed.glowIntensity,
                                ringDiameterDp = led.cameraRingDiameterDp.coerceIn(22f, 44f).dp,
                                ringStrokeDp = led.cameraRingStrokeDp.coerceIn(1.5f, 6f).dp,
                                showCameraCutoutPreview = true,
                                contentDescription = "Pratinjau LED Melingkar Kamera A56"
                            )
                        }
                    } else {
                        // Simulated sleek hardware bezel strip housing both LEDs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFF020409))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(led.spacingDp.coerceIn(6f, 22f).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BlackBerryLedDiode(
                                    color = Color(led.appLed.colorHex),
                                    intensity = previewIntensity1,
                                    sizeDp = led.appLed.sizeDp.dp,
                                    glowIntensity = led.appLed.glowIntensity,
                                    showDormantLens = true,
                                    contentDescription = "Pratinjau LED 1 Aplikasi"
                                )
                                BlackBerryLedDiode(
                                    color = Color(led.indicatorLed.colorHex),
                                    intensity = previewIntensity2,
                                    sizeDp = led.indicatorLed.sizeDp.dp,
                                    glowIntensity = led.indicatorLed.glowIntensity,
                                    showDormantLens = true,
                                    contentDescription = "Pratinjau LED 2 Indikator"
                                )
                            }
                        }
                    }
                }

                // Quick live test triggers (actually flashes on Home/Lock screen too!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VitroGlassContainer(
                        cornerRadius = 14.dp,
                        tintColor = Color(led.appLed.colorHex),
                        glassOpacity = 0.24f,
                        borderColor = Color(led.appLed.colorHex).copy(alpha = 0.65f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_test_led_1_button")
                            .bouncyClickable { onTestLed(1) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uji LED 1 (Aplikasi)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                    VitroGlassContainer(
                        cornerRadius = 14.dp,
                        tintColor = Color(led.indicatorLed.colorHex),
                        glassOpacity = 0.24f,
                        borderColor = Color(led.indicatorLed.colorHex).copy(alpha = 0.65f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_test_led_2_button")
                            .bouncyClickable { onTestLed(2) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uji LED 2 (Indikator)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                    }
                }
            }
        }

        if (led.masterEnabled) {
            // Section Switcher: LED 1 / LED 2 / Posisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedSectionPill(
                    label = "LED 1: Aplikasi",
                    icon = Icons.Rounded.NotificationsActive,
                    selected = selectedSection == 0,
                    accent = Color(led.appLed.colorHex),
                    modifier = Modifier.weight(1f).testTag("studio_led_subtab_1"),
                    onClick = { selectedSection = 0 }
                )
                LedSectionPill(
                    label = "LED 2: Indikator",
                    icon = Icons.Rounded.Bolt,
                    selected = selectedSection == 1,
                    accent = Color(led.indicatorLed.colorHex),
                    modifier = Modifier.weight(1f).testTag("studio_led_subtab_2"),
                    onClick = { selectedSection = 1 }
                )
                LedSectionPill(
                    label = "Posisi",
                    icon = Icons.Rounded.Tune,
                    selected = selectedSection == 2,
                    accent = VitroCyanAccent,
                    modifier = Modifier.weight(0.85f).testTag("studio_led_subtab_pos"),
                    onClick = { selectedSection = 2 }
                )
            }

            when (selectedSection) {
                // ======================================================
                // LED 1: NOTIFIKASI APLIKASI TERTENTU
                // ======================================================
                0 -> {
                    StudioSectionTitle("LED 1 • Lampu Notifikasi Aplikasi Tertentu")
                    StudioToggleRow(
                        label = "Aktifkan Lampu LED 1",
                        checked = led.appLed.enabled,
                        onCheckedChange = { c -> updateLed { it.copy(appLed = it.appLed.copy(enabled = c)) } }
                    )

                    if (led.appLed.enabled) {
                        LedLampCustomizationControls(
                            lamp = led.appLed,
                            colorTitle = "Warna Default LED 1",
                            durationHint = "Berapa lama LED 1 menyala saat notifikasi aplikasi masuk",
                            onLampChange = { updated -> updateLed { it.copy(appLed = updated) } }
                        )

                        StudioSectionTitle("Pilih Aplikasi Tertentu & Warna Khusus")
                        Text(
                            text = "Pilih aplikasi khusus (misalnya WhatsApp hijau, Instagram pink, Telegram biru). Ketuk titik warna di setiap aplikasi untuk mengganti warna LED-nya!",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitroTextSecondary
                        )

                        StudioToggleRow(
                            label = "Nyala untuk semua aplikasi jika tidak masuk daftar",
                            checked = led.appLedAllAppsFallback,
                            onCheckedChange = { c -> updateLed { it.copy(appLedAllAppsFallback = c) } }
                        )

                        Button(
                            onClick = { showAppPicker = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VitroCyanAccent,
                                contentColor = VitroObsidian
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_led_add_app_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Aplikasi untuk LED 1", style = MaterialTheme.typography.labelLarge)
                        }

                        if (led.appRules.isEmpty()) {
                            VitroGlassContainer(
                                cornerRadius = 18.dp,
                                glassOpacity = 0.14f,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (led.appLedAllAppsFallback) {
                                        "Belum ada aplikasi khusus dipilih — LED 1 otomatis menyala untuk semua notifikasi masuk dengan warna default."
                                    } else {
                                        "Belum ada aplikasi dipilih. Ketuk tombol di atas untuk memilih aplikasi yang akan menyalakan LED 1."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VitroTextSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                led.appRules.forEach { rule ->
                                    val app = installedApps.firstOrNull { it.packageName == rule.packageName }
                                    val appLabel = app?.label ?: rule.packageName.substringAfterLast('.')
                                    var expandedPalette by remember(rule.packageName) { mutableStateOf(false) }

                                    VitroGlassContainer(
                                        cornerRadius = 18.dp,
                                        tintColor = Color(rule.colorHex),
                                        glassOpacity = 0.16f,
                                        borderColor = Color(rule.colorHex).copy(alpha = 0.50f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                            contentDescription = appLabel,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = appLabel,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = VitroTextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val colorName = LedColorPreset.entries
                                                            .firstOrNull { it.hex == rule.colorHex }?.label ?: "Kustom"
                                                        Text(
                                                            text = "LED: $colorName • Ketuk titik warna untuk ubah",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VitroTextSecondary
                                                        )
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(rule.colorHex))
                                                            .border(2.dp, Color.White, CircleShape)
                                                            .clickable { expandedPalette = !expandedPalette }
                                                    )
                                                    IconButton(
                                                        onClick = { onRemoveLedAppRule(rule.packageName) },
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.DeleteOutline,
                                                            contentDescription = "Hapus",
                                                            tint = VitroCoral,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (expandedPalette) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    LedColorPreset.entries.forEach { preset ->
                                                        val isPicked = rule.colorHex == preset.hex
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(preset.hex))
                                                                .border(
                                                                    width = if (isPicked) 2.5.dp else 1.dp,
                                                                    color = if (isPicked) Color.White else Color.White.copy(alpha = 0.35f),
                                                                    shape = CircleShape
                                                                )
                                                                .clickable {
                                                                    onUpdateLedAppColor(rule.packageName, preset.hex)
                                                                    expandedPalette = false
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isPicked) {
                                                                Icon(
                                                                    Icons.Rounded.Check,
                                                                    contentDescription = preset.label,
                                                                    tint = VitroObsidian,
                                                                    modifier = Modifier.size(15.dp)
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
                        }
                    }
                }

                // ======================================================
                // LED 2: LAMPU INDIKATOR SISTEM HP
                // ======================================================
                1 -> {
                    StudioSectionTitle("LED 2 • Lampu Indikator Status HP")
                    StudioToggleRow(
                        label = "Aktifkan Lampu LED 2",
                        checked = led.indicatorLed.enabled,
                        onCheckedChange = { c -> updateLed { it.copy(indicatorLed = it.indicatorLed.copy(enabled = c)) } }
                    )

                    if (led.indicatorLed.enabled) {
                        StudioSectionTitle("Pemicu Indikator LED 2")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LedIndicatorTrigger.entries.forEach { trigger ->
                                val selected = led.indicatorTrigger == trigger
                                VitroGlassContainer(
                                    cornerRadius = 18.dp,
                                    tintColor = if (selected) Color(led.indicatorLed.colorHex) else Color.White,
                                    glassOpacity = if (selected) 0.24f else 0.12f,
                                    borderColor = if (selected) Color(led.indicatorLed.colorHex).copy(alpha = 0.78f)
                                    else Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bouncyClickable { updateLed { it.copy(indicatorTrigger = trigger) } }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(trigger.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                                            Text(trigger.subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
                                        }
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, contentDescription = "Aktif", tint = VitroMint)
                                        }
                                    }
                                }
                            }
                        }

                        if (led.indicatorTrigger == LedIndicatorTrigger.SMART_AUTO) {
                            StudioToggleRow(
                                label = "Warna Otomatis Status (Cas=Amber, Penuh=Hijau, Lemah=Merah)",
                                checked = led.indicatorUseAutoStatusColor,
                                onCheckedChange = { c -> updateLed { it.copy(indicatorUseAutoStatusColor = c) } }
                            )
                        }

                        LedLampCustomizationControls(
                            lamp = led.indicatorLed,
                            colorTitle = "Warna Lampu LED 2",
                            durationHint = "Berapa lama LED 2 menyala setelah status HP berubah",
                            onLampChange = { updated -> updateLed { it.copy(indicatorLed = updated) } }
                        )
                    }
                }

                // ======================================================
                // POSISI & PENGATURAN GLOBAL KEDUA LAMPU LED
                // ======================================================
                2 -> {
                    StudioSectionTitle("Posisi 2 Lampu LED di Layar")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LedPlacement.entries.forEach { placement ->
                            val selected = led.placement == placement
                            VitroGlassContainer(
                                cornerRadius = 18.dp,
                                tintColor = if (selected) VitroCyanAccent else Color.White,
                                glassOpacity = if (selected) 0.24f else 0.12f,
                                borderColor = if (selected) VitroCyanAccent else Color.White.copy(alpha = 0.22f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bouncyClickable { updateLed { it.copy(placement = placement) } }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(placement.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                                        Text(placement.subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
                                    }
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Terpilih", tint = VitroCyanAccent)
                                    }
                                }
                            }
                        }
                    }

                    if (led.placement == LedPlacement.CAMERA_RING_A56) {
                        StudioSectionTitle("Kalibrasi Lingkaran Kamera (Samsung A56)")
                        StudioSliderRow(
                            label = "Diameter Lingkaran Kamera",
                            valueText = "${led.cameraRingDiameterDp.roundToInt()} dp",
                            value = led.cameraRingDiameterDp,
                            range = 18f..56f,
                            onValueChange = { v -> updateLed { it.copy(cameraRingDiameterDp = v) } }
                        )
                        StudioSliderRow(
                            label = "Ketebalan Cincin Cahaya LED",
                            valueText = String.format("%.1f dp", led.cameraRingStrokeDp),
                            value = led.cameraRingStrokeDp,
                            range = 1.5f..7.0f,
                            onValueChange = { v -> updateLed { it.copy(cameraRingStrokeDp = v) } }
                        )
                        StudioSliderRow(
                            label = "Posisi Vertikal Y (Pas Lubang Kamera)",
                            valueText = "${led.topOffsetDp.roundToInt()} dp",
                            value = led.topOffsetDp,
                            range = 0f..60f,
                            onValueChange = { v -> updateLed { it.copy(topOffsetDp = v) } }
                        )
                        StudioSliderRow(
                            label = "Geser Horizontal X Kamera",
                            valueText = "${led.horizontalOffsetDp.roundToInt()} dp",
                            value = led.horizontalOffsetDp.coerceIn(-60f, 60f),
                            range = -60f..60f,
                            onValueChange = { v -> updateLed { it.copy(horizontalOffsetDp = v) } }
                        )
                    } else {
                        StudioSliderRow(
                            label = "Jarak dari Tepi Atas (Posisi Y)",
                            valueText = "${led.topOffsetDp.roundToInt()} dp",
                            value = led.topOffsetDp,
                            range = 2f..80f,
                            onValueChange = { v -> updateLed { it.copy(topOffsetDp = v) } }
                        )
                        StudioSliderRow(
                            label = "Jarak dari Tepi Kiri/Kanan (Posisi X)",
                            valueText = "${led.horizontalOffsetDp.roundToInt()} dp",
                            value = led.horizontalOffsetDp.coerceIn(4f, 120f),
                            range = 4f..120f,
                            onValueChange = { v -> updateLed { it.copy(horizontalOffsetDp = v) } }
                        )
                        StudioSliderRow(
                            label = "Jarak Antar Lampu LED 1 & LED 2",
                            valueText = "${led.spacingDp.roundToInt()} dp",
                            value = led.spacingDp,
                            range = 4f..34f,
                            onValueChange = { v -> updateLed { it.copy(spacingDp = v) } }
                        )
                    }

                    StudioSectionTitle("Tampilan Lintas Layar")
                    StudioToggleRow(
                        label = "Tampilkan Lampu LED di Layar Kunci",
                        checked = led.showOnLockScreen,
                        onCheckedChange = { c -> updateLed { it.copy(showOnLockScreen = c) } }
                    )
                    StudioToggleRow(
                        label = "Tampilkan Lampu LED di Atas Aplikasi Lain (System-Wide)",
                        checked = led.showOverOtherApps,
                        onCheckedChange = { c -> updateLed { it.copy(showOverOtherApps = c) } }
                    )
                }
            }
        }
    }

    if (showAppPicker) {
        SearchableAppPickerDialog(
            title = "Pilih Aplikasi untuk LED 1",
            apps = installedApps,
            selectedPackages = led.appRules.map { it.packageName }.toSet(),
            onPickApp = { app ->
                val smartColor = guessDefaultLedColorForApp(app.packageName, led.appLed.colorHex)
                onAddLedAppRule(app.packageName, smartColor)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LedLampCustomizationControls(
    lamp: LedLampConfig,
    colorTitle: String,
    durationHint: String,
    onLampChange: (LedLampConfig) -> Unit
) {
    // 1. Color Swatches
    StudioSectionTitle(colorTitle)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LedColorPreset.entries.forEach { preset ->
            val selected = lamp.colorHex == preset.hex
            VitroGlassContainer(
                cornerRadius = 16.dp,
                tintColor = Color(preset.hex),
                glassOpacity = if (selected) 0.34f else 0.14f,
                borderColor = if (selected) Color(preset.hex) else Color.White.copy(alpha = 0.22f),
                modifier = Modifier.bouncyClickable {
                    onLampChange(lamp.copy(colorHex = preset.hex))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(preset.hex))
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroTextPrimary
                    )
                }
            }
        }
    }

    // 2. Blink Pattern Selector (BlackBerry Authentic Patterns)
    StudioSectionTitle("Pola Kedip ala BlackBerry")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        LedBlinkPattern.entries.forEach { pattern ->
            val selected = lamp.pattern == pattern
            VitroGlassContainer(
                cornerRadius = 16.dp,
                tintColor = if (selected) Color(lamp.colorHex) else Color.White,
                glassOpacity = if (selected) 0.24f else 0.12f,
                borderColor = if (selected) Color(lamp.colorHex).copy(alpha = 0.78f)
                else Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { onLampChange(lamp.copy(pattern = pattern)) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pattern.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                        Text(pattern.subtitle, style = MaterialTheme.typography.bodySmall, color = VitroTextSecondary)
                    }
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = "Terpilih", tint = Color(lamp.colorHex))
                    }
                }
            }
        }
    }

    // 3. Blink Interval / Speed Slider (hidden if SOLID)
    if (lamp.pattern != LedBlinkPattern.SOLID) {
        val intervalSec = String.format("%.1f dtk", lamp.blinkIntervalMs / 1000f)
        StudioSliderRow(
            label = "Kecepatan / Jeda Kedipan",
            valueText = intervalSec,
            value = lamp.blinkIntervalMs.toFloat(),
            range = 400f..4500f,
            onValueChange = { v -> onLampChange(lamp.copy(blinkIntervalMs = v.roundToInt())) }
        )
    }

    // 4. Duration Selector (Quick Chips + Fine Slider)
    StudioSectionTitle("Durasi Lama Menyala")
    Text(
        text = durationHint,
        style = MaterialTheme.typography.bodySmall,
        color = VitroTextSecondary
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DurationPresetsSeconds.forEach { (sec, label) ->
            StudioSmallChip(
                text = label,
                selected = lamp.durationSeconds == sec,
                onClick = { onLampChange(lamp.copy(durationSeconds = sec)) }
            )
        }
    }
    val durText = when {
        lamp.durationSeconds == 0 -> "Terus sampai dibuka"
        lamp.durationSeconds < 60 -> "${lamp.durationSeconds} detik"
        else -> "${lamp.durationSeconds / 60} mnt ${lamp.durationSeconds % 60} dtk"
    }
    StudioSliderRow(
        label = "Atur Durasi Bebas (0 = Terus Menyala)",
        valueText = durText,
        value = lamp.durationSeconds.toFloat(),
        range = 0f..600f,
        onValueChange = { v ->
            val rounded = if (v < 5f) 0 else (v / 5f).roundToInt() * 5
            onLampChange(lamp.copy(durationSeconds = rounded))
        }
    )

    // 5. Size & Glow Intensity
    StudioSliderRow(
        label = "Ukuran Lampu LED",
        valueText = "${lamp.sizeDp.roundToInt()} dp",
        value = lamp.sizeDp,
        range = 6f..18f,
        onValueChange = { v -> onLampChange(lamp.copy(sizeDp = v)) }
    )
    StudioSliderRow(
        label = "Kecerahan Pendar Cahaya (Glow)",
        valueText = "${(lamp.glowIntensity * 100).roundToInt()}%",
        value = lamp.glowIntensity,
        range = 0.25f..1.0f,
        onValueChange = { v -> onLampChange(lamp.copy(glowIntensity = v)) }
    )
}

@Composable
private fun LedSectionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VitroGlassContainer(
        cornerRadius = 16.dp,
        tintColor = if (selected) accent else Color.White,
        glassOpacity = if (selected) 0.28f else 0.14f,
        borderColor = if (selected) accent else Color.White.copy(alpha = 0.24f),
        modifier = modifier.bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VitroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun guessDefaultLedColorForApp(packageName: String, fallback: Long): Long {
    val lower = packageName.lowercase()
    return when {
        lower.contains("whatsapp") || lower.contains("line") -> LedColorPreset.BBM_GREEN.hex
        lower.contains("instagram") || lower.contains("tiktok") -> LedColorPreset.HOT_PINK.hex
        lower.contains("telegram") || lower.contains("twitter") || lower.contains("facebook") -> LedColorPreset.CYAN_PULSE.hex
        lower.contains("gmail") || lower.contains("youtube") || lower.contains("mail") -> LedColorPreset.BB_RED.hex
        lower.contains("shopee") || lower.contains("grab") -> LedColorPreset.FLAME_ORANGE.hex
        lower.contains("discord") || lower.contains("viber") -> LedColorPreset.VIOLET_NEON.hex
        lower.contains("snapchat") -> LedColorPreset.AMBER_GOLD.hex
        else -> fallback
    }
}
