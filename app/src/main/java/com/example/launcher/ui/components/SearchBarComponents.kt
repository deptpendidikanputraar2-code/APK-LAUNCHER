package com.example.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.SearchBarConfig
import com.example.launcher.model.SearchBarShape
import com.example.launcher.model.WidgetType
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

/**
 * Unified customizable Search Bar rendered consistently across Home Screen, App Drawer & Studio Vitro.
 * Respects user-configured shape, color preset, glass opacity, height, and width.
 */
@Composable
fun VitroCustomizableSearchBar(
    searchConfig: SearchBarConfig,
    query: String,
    onQueryChange: ((String) -> Unit)? = null,
    placeholder: String = "Cari aplikasi, menu, widget...",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingActionIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val preset = searchConfig.colorPreset
    val shape = RoundedCornerShape(searchConfig.shape.cornerDp.dp)
    val isMinimalLine = searchConfig.shape == SearchBarShape.MINIMAL_LINE
    val bgAlpha = if (isMinimalLine) 0.06f else searchConfig.opacity.coerceIn(0.08f, 0.85f)

    Box(
        modifier = modifier
            .height(searchConfig.heightDp.coerceIn(40, 68).dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        preset.bgColor.copy(alpha = bgAlpha),
                        preset.bgColor.copy(alpha = (bgAlpha * 0.62f).coerceAtLeast(0.04f))
                    )
                )
            )
            .border(
                width = if (isMinimalLine) 1.4.dp else 1.dp,
                color = preset.borderColor,
                shape = shape
            )
            .then(
                if (readOnly && onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Cari",
                tint = preset.contentColor.copy(alpha = 0.88f),
                modifier = Modifier.size(19.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (readOnly || onQueryChange == null) {
                    Text(
                        text = query.ifBlank { placeholder },
                        color = preset.contentColor.copy(alpha = if (query.isBlank()) 0.72f else 0.96f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = preset.contentColor.copy(alpha = 0.62f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(VitroCyanAccent),
                        textStyle = TextStyle(
                            color = preset.contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!readOnly && onQueryChange != null && query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Bersihkan",
                        tint = preset.contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (trailingActionIcon != null) {
                trailingActionIcon()
            }
        }
    }
}

/**
 * Full-screen Universal Glass Search Overlay launched when tapping the Home Screen Search Bar.
 * Instantly searches installed HP apps, Vitro menu/settings shortcuts, and Vitro Glass Widgets.
 */
@Composable
fun UniversalSearchOverlay(
    visible: Boolean,
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenQuickPanel: () -> Unit,
    onAddBuiltInWidget: (WidgetType) -> Unit
) {
    var query by remember(visible) { mutableStateOf("") }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 6 },
        exit = fadeOut() + slideOutVertically { -it / 6 }
    ) {
        BackHandler(onBack = onDismiss)

        val safeTop = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 10.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        val trimmed = query.trim()
        val matchingApps = remember(installedApps, trimmed) {
            if (trimmed.isBlank()) installedApps.take(8)
            else installedApps.filter {
                it.label.contains(trimmed, ignoreCase = true) ||
                    it.packageName.contains(trimmed, ignoreCase = true)
            }.take(20)
        }

        val matchingWidgets = remember(trimmed) {
            if (trimmed.isBlank()) emptyList()
            else WidgetType.entries.filter {
                it.label.contains(trimmed, ignoreCase = true) ||
                    it.subtitle.contains(trimmed, ignoreCase = true)
            }
        }

        val quickShortcuts = listOf(
            "Studio Vitro (Pengaturan Lengkap)" to onOpenStudio,
            "Pusat Kontrol Vitro" to onOpenQuickPanel,
            "Atur Pencarian & DIY" to onOpenStudio,
            "Atur Dynamic Island & Suara Notifikasi" to onOpenStudio
        ).filter { trimmed.isBlank() || it.first.contains(trimmed, ignoreCase = true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF5040814))
                .then(safeTop)
                .navigationBarsPadding()
                .imePadding()
                .testTag("universal_search_overlay"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pencarian Cepat Vitro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VitroTextPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (matchingApps.isNotEmpty()) {
                        item {
                            Text(
                                text = if (trimmed.isBlank()) "Aplikasi Sering Diakses" else "Aplikasi HP (${matchingApps.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = VitroCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(matchingApps, key = { it.packageName }) { app ->
                            VitroGlassContainer(
                                cornerRadius = 18.dp,
                                glassOpacity = 0.16f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onLaunchApp(app)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Apps,
                                            contentDescription = null,
                                            tint = VitroCyanAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = VitroTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VitroTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchingWidgets.isNotEmpty()) {
                        item {
                            Text(
                                text = "Widget Kaca Vitro (Ketuk untuk Tambah ke Beranda)",
                                style = MaterialTheme.typography.labelMedium,
                                color = VitroCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matchingWidgets) { widget ->
                            VitroGlassContainer(
                                cornerRadius = 18.dp,
                                glassOpacity = 0.16f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddBuiltInWidget(widget)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DashboardCustomize,
                                        contentDescription = null,
                                        tint = VitroCyanAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(widget.label, style = MaterialTheme.typography.titleSmall, color = VitroTextPrimary)
                                        Text(widget.subtitle, style = MaterialTheme.typography.labelSmall, color = VitroTextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    if (quickShortcuts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pintasan Menu Vitro",
                                style = MaterialTheme.typography.labelMedium,
                                color = VitroCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(quickShortcuts) { (label, action) ->
                            VitroGlassContainer(
                                cornerRadius = 18.dp,
                                glassOpacity = 0.14f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        action()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = VitroCyanAccent, modifier = Modifier.size(20.dp))
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = VitroTextPrimary)
                                }
                            }
                        }
                    }
                }

                // Bottom-anchored ergonomic Search Bar + Close Button for effortless thumb reach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VitroCustomizableSearchBar(
                        searchConfig = config.searchBar,
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Cari aplikasi, widget kaca, menu Vitro...",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("universal_search_bottom_input")
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = VitroTextPrimary)
                    }
                }
            }
        }
    }
}
