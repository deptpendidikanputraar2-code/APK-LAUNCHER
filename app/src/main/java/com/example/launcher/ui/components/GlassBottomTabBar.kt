package com.example.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VitroCyanAccent
import com.example.ui.theme.VitroIndigo
import com.example.ui.theme.VitroTextPrimary
import com.example.ui.theme.VitroTextSecondary

enum class BottomGlassTab {
    HOME,
    PAGES,
    APPS,
    SIDEBAR
}

@Composable
fun GlassBottomTabBar(
    selectedTab: BottomGlassTab,
    currentPage: Int,
    totalPages: Int,
    glowIntensity: Float,
    onSelectHome: () -> Unit,
    onCyclePage: () -> Unit,
    onOpenApps: () -> Unit,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { BottomGlassTab.entries }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        VitroGlassContainer(
            cornerRadius = 30.dp,
            tintColor = Color(0xFF0E1832),
            glassOpacity = 0.42f,
            borderColor = Color.White.copy(alpha = 0.44f),
            glowColor = VitroCyanAccent,
            glowIntensity = glowIntensity * 0.65f,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(60.dp)
                .testTag("glass_bottom_tab_bar")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val tabWidth = maxWidth / tabs.size
                val compactLabels = maxWidth < 320.dp

                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                    label = "glass_tab_indicator_offset"
                )

                // Sliding Radiant Glass Active Pill
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .drawBehind {
                            if (glowIntensity > 0.05f) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            VitroCyanAccent.copy(alpha = 0.36f * glowIntensity),
                                            VitroIndigo.copy(alpha = 0.14f * glowIntensity),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.maxDimension * 0.75f
                                    ),
                                    radius = size.maxDimension * 0.75f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                            }
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    VitroCyanAccent.copy(alpha = 0.30f),
                                    VitroIndigo.copy(alpha = 0.22f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    VitroCyanAccent.copy(alpha = 0.35f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassTabBarItem(
                        label = "Beranda",
                        icon = Icons.Rounded.Home,
                        selected = selectedTab == BottomGlassTab.HOME,
                        compact = compactLabels,
                        testTag = "bottom_tab_home",
                        onClick = onSelectHome,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GlassTabBarItem(
                        label = "Hal ${currentPage + 1}/$totalPages",
                        icon = Icons.Rounded.AutoAwesomeMosaic,
                        selected = selectedTab == BottomGlassTab.PAGES,
                        compact = compactLabels,
                        testTag = "bottom_tab_pages",
                        onClick = onCyclePage,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GlassTabBarItem(
                        label = "Aplikasi",
                        icon = Icons.Rounded.Apps,
                        selected = selectedTab == BottomGlassTab.APPS,
                        compact = compactLabels,
                        testTag = "bottom_tab_apps",
                        onClick = onOpenApps,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GlassTabBarItem(
                        label = "Sidebar",
                        icon = Icons.Rounded.ViewSidebar,
                        selected = selectedTab == BottomGlassTab.SIDEBAR,
                        compact = compactLabels,
                        testTag = "bottom_tab_sidebar",
                        onClick = onToggleSidebar,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTabBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    compact: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isHovered -> 1.07f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 500f),
        label = "tab_item_scale"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            selected -> VitroCyanAccent
            isHovered -> VitroTextPrimary
            else -> VitroTextSecondary
        },
        animationSpec = tween(180),
        label = "tab_icon_tint"
    )
    val hoverBloom by animateFloatAsState(
        targetValue = if (isHovered && !selected) 0.22f else 0f,
        animationSpec = tween(200),
        label = "tab_hover_bloom"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                if (hoverBloom > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VitroCyanAccent.copy(alpha = hoverBloom),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.65f
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            .clip(RoundedCornerShape(22.dp))
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = VitroCyanAccent),
                onClick = onClick
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(if (selected) 20.dp else 18.dp)
        )
        if (!compact) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) VitroTextPrimary else VitroTextSecondary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
