package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.model.InstalledApp
import com.example.launcher.model.LauncherConfig
import com.example.launcher.ui.components.VitroGlassContainer
import com.example.launcher.ui.components.bouncyClickable
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    config: LauncherConfig,
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onToggleDockPin: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pinMode by remember { mutableStateOf(false) }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(searchQuery.trim(), ignoreCase = true) ||
                it.packageName.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

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

        val safeTop = if (config.hideSystemStatusBar) {
            Modifier.displayCutoutPadding().padding(top = 6.dp)
        } else {
            Modifier.statusBarsPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF5050914))
                .then(safeTop)
                .navigationBarsPadding()
                .imePadding()
                .testTag("app_drawer_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_drawer_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = VitroTextPrimary
                        )
                        Text(
                            text = "${installedApps.size} aplikasi terpasang",
                            style = MaterialTheme.typography.labelMedium,
                            color = VitroCyanAccent
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitroGlassContainer(
                            cornerRadius = 50.dp,
                            tintColor = if (pinMode) VitroCyanAccent else Color.White,
                            glassOpacity = if (pinMode) 0.40f else 0.20f,
                            modifier = Modifier
                                .testTag("drawer_pin_mode_button")
                                .bouncyClickable { pinMode = !pinMode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = "Mode semat ke dock",
                                    tint = VitroTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pinMode) "Selesai" else "Atur Dock",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VitroTextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("drawer_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = VitroTextPrimary
                            )
                        }
                    }
                }

                if (pinMode) {
                    Text(
                        text = "Ketuk aplikasi mana saja untuk menyematkan atau melepasnya dari Dock Bar.",
                        style = MaterialTheme.typography.labelMedium,
                        color = VitroCyanAccent
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.homeGridColumns.coerceIn(3, 5)),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        val isPinned = config.pinnedDockPackages.contains(app.packageName)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .bouncyClickable {
                                    if (pinMode) onToggleDockPin(app.packageName)
                                    else onLaunchApp(app)
                                }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                    } else {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = VitroTextPrimary
                                        )
                                    }
                                }
                                if (isPinned) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(VitroCyanAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = "Disematkan di dock",
                                            tint = Color.Black,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = VitroTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom-anchored ergonomic Search Bar for effortless thumb reach
                com.example.launcher.ui.components.VitroCustomizableSearchBar(
                    searchConfig = config.searchBar,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_apps_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                        .testTag("drawer_search_input")
                )
            }
        }
    }
}
