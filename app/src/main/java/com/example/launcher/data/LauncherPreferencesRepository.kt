package com.example.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.launcher.model.BatteryIconStyle
import com.example.launcher.model.CustomNotificationSound
import com.example.launcher.model.DockColorPreset
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.DynamicIslandConfig
import com.example.launcher.model.DynamicIslandStyle
import com.example.launcher.model.EdgePanelTriggerZone
import com.example.launcher.model.HomeWidgetPlacement
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.LockClockColor
import com.example.launcher.model.LockClockStyle
import com.example.launcher.model.LockSecurityMode
import com.example.launcher.model.PageTransitionStyle
import com.example.launcher.model.PopupStyle
import com.example.launcher.model.QuickModuleConfig
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.model.SearchBarColorPreset
import com.example.launcher.model.SearchBarConfig
import com.example.launcher.model.SearchBarPosition
import com.example.launcher.model.SearchBarShape
import com.example.launcher.model.SidebarItemConfig
import com.example.launcher.model.SidebarItemType
import com.example.launcher.model.SidebarSide
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.StatusSlotAlignment
import com.example.launcher.model.TransitionSpeed
import com.example.launcher.model.WallpaperColorTone
import com.example.launcher.model.WallpaperConfig
import com.example.launcher.model.WallpaperGlassStyle
import com.example.launcher.model.WallpaperParticleOverlay
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import com.example.launcher.model.defaultHomeWidgetPlacements
import com.example.launcher.model.defaultLockWidgets
import com.example.launcher.model.defaultQuickModules
import com.example.launcher.model.defaultSidebarItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vitro_launcher_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val DOCK_LAYOUT = stringPreferencesKey("dock_layout")
        val DOCK_COLOR = stringPreferencesKey("dock_color")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val DOCK_CORNER = floatPreferencesKey("dock_corner")
        val DOCK_MAX_ICONS = intPreferencesKey("dock_max_icons")
        val SHOW_DOCK_LABELS = booleanPreferencesKey("show_dock_labels")
        val PINNED_DOCK = stringPreferencesKey("pinned_dock_packages")

        val WIDGET_STACK_MODE = stringPreferencesKey("widget_stack_mode")
        val ENABLED_WIDGETS = stringPreferencesKey("enabled_widgets")
        val HOME_WIDGETS_V2 = stringPreferencesKey("home_widgets_v2")
        val LOCK_WIDGETS_V1 = stringPreferencesKey("lock_widgets_v1")
        val WIDGET_OPACITY = floatPreferencesKey("widget_opacity")
        val QUICK_NOTE = stringPreferencesKey("quick_note")

        val SB_ENABLED = booleanPreferencesKey("sb_enabled")
        val SB_POS = stringPreferencesKey("sb_pos")
        val SB_SHAPE = stringPreferencesKey("sb_shape")
        val SB_COLOR = stringPreferencesKey("sb_color")
        val SB_OPACITY = floatPreferencesKey("sb_opacity")
        val SB_WIDTH = floatPreferencesKey("sb_width")
        val SB_HEIGHT = intPreferencesKey("sb_height")
        val SB_X = floatPreferencesKey("sb_x")
        val SB_Y = floatPreferencesKey("sb_y")

        val QP_ORIENTATION = stringPreferencesKey("qp_orientation")
        val QP_OPACITY = floatPreferencesKey("qp_opacity")
        val QP_MODULES_V2 = stringPreferencesKey("qp_modules_v2")
        val QP_BRIGHTNESS = booleanPreferencesKey("qp_brightness")
        val QP_VOLUME = booleanPreferencesKey("qp_volume")
        val QP_MEDIA = booleanPreferencesKey("qp_media")
        val QP_SLIDER_HEIGHT = intPreferencesKey("qp_slider_height")
        val QP_REPLACE_SYSTEM = booleanPreferencesKey("qp_replace_system")
        val QP_EDGE_ZONE = stringPreferencesKey("qp_edge_zone")

        val DI_ENABLED = booleanPreferencesKey("di_enabled")
        val DI_WIDTH = floatPreferencesKey("di_width")
        val DI_HEIGHT = floatPreferencesKey("di_height")
        val DI_TOP_OFFSET = floatPreferencesKey("di_top_offset")
        val DI_HORIZ_OFFSET = floatPreferencesKey("di_horiz_offset")
        val DI_RADIUS = floatPreferencesKey("di_radius")
        val DI_STYLE = stringPreferencesKey("di_style")
        val DI_GLOW = floatPreferencesKey("di_glow")
        val DI_LOCK_SCREEN = booleanPreferencesKey("di_lock_screen")
        val DI_CLOCK = booleanPreferencesKey("di_clock")
        val DI_BATTERY = booleanPreferencesKey("di_battery")
        val DI_NOTIF = booleanPreferencesKey("di_notif")
        val DI_MEDIA = booleanPreferencesKey("di_media")
        val DI_FOCUS = booleanPreferencesKey("di_focus")
        val DI_AUTO_NOTIF = booleanPreferencesKey("di_auto_notif")
        val DI_AUTO_SYS = booleanPreferencesKey("di_auto_sys")
        val DI_DUAL_SPLIT = booleanPreferencesKey("di_dual_split")

        val HIDE_SYSTEM_STATUS_BAR = booleanPreferencesKey("hide_system_status_bar")
        val STATUS_POSITION = stringPreferencesKey("status_position")
        val STATUS_ALIGNMENT = stringPreferencesKey("status_alignment")
        val BATTERY_STYLE = stringPreferencesKey("battery_style")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_SIGNAL = booleanPreferencesKey("show_signal")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_BATTERY_PCT = booleanPreferencesKey("show_battery_pct")

        val REPLACE_SYSTEM_NOTIF = booleanPreferencesKey("replace_system_notif")
        val SHOW_SYS_WIDE_ISLAND = booleanPreferencesKey("show_sys_wide_island")
        val ACTIVE_NOTIF_SOUND = stringPreferencesKey("active_notif_sound")
        val CUSTOM_NOTIF_SOUNDS = stringPreferencesKey("custom_notif_sounds")
        val HOME_POPUP_STYLE = stringPreferencesKey("home_popup_style")
        val LOCK_POPUP_STYLE = stringPreferencesKey("lock_popup_style")
        val POPUP_CORNER = floatPreferencesKey("popup_corner")
        val POPUP_OPACITY = floatPreferencesKey("popup_opacity")

        val HOME_PAGE_COUNT = intPreferencesKey("home_page_count")
        val HOME_COLS = intPreferencesKey("home_cols")
        val HOME_ROWS = intPreferencesKey("home_rows")
        val HOME_PAGE_SCALE = floatPreferencesKey("home_page_scale")
        val LOCK_PAGE_COUNT = intPreferencesKey("lock_page_count")
        val LOCK_CLOCK_STYLE = stringPreferencesKey("lock_clock_style")
        val LOCK_CLOCK_COLOR = stringPreferencesKey("lock_clock_color")
        val LOCK_CLOCK_SCALE = floatPreferencesKey("lock_clock_scale")
        val LOCK_CLOCK_V_OFFSET = floatPreferencesKey("lock_clock_v_offset")
        val LOCK_CLOCK_WEIGHT = intPreferencesKey("lock_clock_weight")
        val LOCK_PAGE_SCALE = floatPreferencesKey("lock_page_scale")
        val REPLACE_SYSTEM_LOCK = booleanPreferencesKey("replace_system_lock")
        val DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        val LOCK_SECURITY_MODE = stringPreferencesKey("lock_security_mode")
        val LOCK_PASSCODE_HASH = stringPreferencesKey("lock_passcode_hash")
        val LOCK_PASSCODE_LENGTH = intPreferencesKey("lock_passcode_length")

        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val TRANSITION_SPEED = stringPreferencesKey("transition_speed")
        val SHOW_GLASSY_SIDEBAR = booleanPreferencesKey("show_glassy_sidebar")
        val SIDEBAR_SIDE = stringPreferencesKey("sidebar_side")
        val SIDEBAR_WIDTH = floatPreferencesKey("sidebar_width")
        val SIDEBAR_OPACITY = floatPreferencesKey("sidebar_opacity")
        val SIDEBAR_ITEMS = stringPreferencesKey("sidebar_items")
        val AMBIENT_GLOW = floatPreferencesKey("ambient_glow_intensity")
        val WALLPAPER = stringPreferencesKey("wallpaper")
        val WP_HOME_V2 = stringPreferencesKey("wp_home_v2")
        val WP_SEPARATE_LOCK = booleanPreferencesKey("wp_separate_lock")
        val WP_LOCK_V2 = stringPreferencesKey("wp_lock_v2")
    }

    val configFlow: Flow<LauncherConfig> = context.launcherDataStore.data
        .catch { e ->
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw e
        }
        .map { prefs ->
            val defaults = LauncherConfig()
            val diDef = defaults.dynamicIsland
            val sbDef = defaults.searchBar
            LauncherConfig(
                dockLayout = prefs[Keys.DOCK_LAYOUT].toEnumOrDefault(defaults.dockLayout),
                dockColor = prefs[Keys.DOCK_COLOR].toEnumOrDefault(defaults.dockColor),
                dockOpacity = prefs[Keys.DOCK_OPACITY] ?: defaults.dockOpacity,
                dockCornerRadius = prefs[Keys.DOCK_CORNER] ?: defaults.dockCornerRadius,
                dockMaxIcons = (prefs[Keys.DOCK_MAX_ICONS] ?: defaults.dockMaxIcons).coerceIn(3, 6),
                showDockLabels = prefs[Keys.SHOW_DOCK_LABELS] ?: defaults.showDockLabels,
                pinnedDockPackages = prefs[Keys.PINNED_DOCK]
                    ?.split("|")
                    ?.filter { it.isNotBlank() }
                    ?: defaults.pinnedDockPackages,

                widgetStackMode = prefs[Keys.WIDGET_STACK_MODE].toEnumOrDefault(defaults.widgetStackMode),
                enabledWidgets = prefs[Keys.ENABLED_WIDGETS]?.let { raw ->
                    raw.split("|").mapNotNull { runCatching { WidgetType.valueOf(it) }.getOrNull() }
                        .ifEmpty { defaults.enabledWidgets }
                } ?: defaults.enabledWidgets,
                homeWidgets = prefs[Keys.HOME_WIDGETS_V2]?.let { decodeHomeWidgets(it) }
                    ?: defaultHomeWidgetPlacements(),
                lockWidgets = prefs[Keys.LOCK_WIDGETS_V1]?.let { decodeHomeWidgets(it) }
                    ?: defaultLockWidgets(),
                widgetGlassOpacity = prefs[Keys.WIDGET_OPACITY] ?: defaults.widgetGlassOpacity,
                quickNoteText = prefs[Keys.QUICK_NOTE] ?: defaults.quickNoteText,

                searchBar = SearchBarConfig(
                    enabledOnHome = prefs[Keys.SB_ENABLED] ?: sbDef.enabledOnHome,
                    position = prefs[Keys.SB_POS].toEnumOrDefault(sbDef.position),
                    shape = prefs[Keys.SB_SHAPE].toEnumOrDefault(sbDef.shape),
                    colorPreset = prefs[Keys.SB_COLOR].toEnumOrDefault(sbDef.colorPreset),
                    opacity = (prefs[Keys.SB_OPACITY] ?: sbDef.opacity).coerceIn(0.08f, 0.85f),
                    widthPercent = (prefs[Keys.SB_WIDTH] ?: sbDef.widthPercent).coerceIn(0.50f, 1.0f),
                    heightDp = (prefs[Keys.SB_HEIGHT] ?: sbDef.heightDp).coerceIn(40, 68),
                    xOffsetDp = (prefs[Keys.SB_X] ?: sbDef.xOffsetDp).coerceIn(-120f, 120f),
                    yOffsetDp = (prefs[Keys.SB_Y] ?: sbDef.yOffsetDp).coerceIn(-120f, 360f)
                ),

                quickPanelOrientation = prefs[Keys.QP_ORIENTATION].toEnumOrDefault(defaults.quickPanelOrientation),
                quickPanelGlassOpacity = prefs[Keys.QP_OPACITY] ?: defaults.quickPanelGlassOpacity,
                quickModules = prefs[Keys.QP_MODULES_V2]?.let { decodeModulesV2(it) }
                    ?: defaultQuickModules(),
                showBrightnessSlider = prefs[Keys.QP_BRIGHTNESS] ?: defaults.showBrightnessSlider,
                showVolumeSlider = prefs[Keys.QP_VOLUME] ?: defaults.showVolumeSlider,
                showMediaControl = prefs[Keys.QP_MEDIA] ?: defaults.showMediaControl,
                sliderHeightDp = (prefs[Keys.QP_SLIDER_HEIGHT] ?: defaults.sliderHeightDp).coerceIn(100, 220),
                replaceSystemControlPanel = prefs[Keys.QP_REPLACE_SYSTEM] ?: defaults.replaceSystemControlPanel,
                edgeTriggerZone = prefs[Keys.QP_EDGE_ZONE].toEnumOrDefault(defaults.edgeTriggerZone),

                dynamicIsland = DynamicIslandConfig(
                    enabled = prefs[Keys.DI_ENABLED] ?: diDef.enabled,
                    widthDp = (prefs[Keys.DI_WIDTH] ?: diDef.widthDp).coerceIn(108f, 280f),
                    heightDp = (prefs[Keys.DI_HEIGHT] ?: diDef.heightDp).coerceIn(26f, 54f),
                    topOffsetDp = (prefs[Keys.DI_TOP_OFFSET] ?: diDef.topOffsetDp).coerceIn(0f, 56f),
                    horizontalOffsetDp = (prefs[Keys.DI_HORIZ_OFFSET] ?: diDef.horizontalOffsetDp).coerceIn(-90f, 90f),
                    cornerRadiusDp = (prefs[Keys.DI_RADIUS] ?: diDef.cornerRadiusDp).coerceIn(12f, 32f),
                    style = prefs[Keys.DI_STYLE].toEnumOrDefault(diDef.style),
                    borderGlow = (prefs[Keys.DI_GLOW] ?: diDef.borderGlow).coerceIn(0f, 1f),
                    showOnLockScreen = prefs[Keys.DI_LOCK_SCREEN] ?: diDef.showOnLockScreen,
                    showClock = prefs[Keys.DI_CLOCK] ?: diDef.showClock,
                    showBattery = prefs[Keys.DI_BATTERY] ?: diDef.showBattery,
                    showNotifications = prefs[Keys.DI_NOTIF] ?: diDef.showNotifications,
                    showMedia = prefs[Keys.DI_MEDIA] ?: diDef.showMedia,
                    showFocusTimer = prefs[Keys.DI_FOCUS] ?: diDef.showFocusTimer,
                    autoExpandOnNotification = prefs[Keys.DI_AUTO_NOTIF] ?: diDef.autoExpandOnNotification,
                    autoExpandOnSystemStatus = prefs[Keys.DI_AUTO_SYS] ?: diDef.autoExpandOnSystemStatus,
                    showDualSplitBubble = prefs[Keys.DI_DUAL_SPLIT] ?: diDef.showDualSplitBubble
                ),

                hideSystemStatusBar = prefs[Keys.HIDE_SYSTEM_STATUS_BAR] ?: defaults.hideSystemStatusBar,
                statusPosition = prefs[Keys.STATUS_POSITION].toEnumOrDefault(defaults.statusPosition),
                statusAlignment = prefs[Keys.STATUS_ALIGNMENT].toEnumOrDefault(defaults.statusAlignment),
                batteryStyle = prefs[Keys.BATTERY_STYLE].toEnumOrDefault(defaults.batteryStyle),
                showClockIndicator = prefs[Keys.SHOW_CLOCK] ?: defaults.showClockIndicator,
                showSignalIndicator = prefs[Keys.SHOW_SIGNAL] ?: defaults.showSignalIndicator,
                showWifiIndicator = prefs[Keys.SHOW_WIFI] ?: defaults.showWifiIndicator,
                showBatteryPercent = prefs[Keys.SHOW_BATTERY_PCT] ?: defaults.showBatteryPercent,

                replaceSystemNotifications = prefs[Keys.REPLACE_SYSTEM_NOTIF] ?: defaults.replaceSystemNotifications,
                showSystemWideDynamicIsland = prefs[Keys.SHOW_SYS_WIDE_ISLAND] ?: defaults.showSystemWideDynamicIsland,
                activeNotifSoundId = prefs[Keys.ACTIVE_NOTIF_SOUND] ?: defaults.activeNotifSoundId,
                customNotifSounds = prefs[Keys.CUSTOM_NOTIF_SOUNDS]?.let { decodeCustomSounds(it) }
                    ?: emptyList(),
                homePopupStyle = prefs[Keys.HOME_POPUP_STYLE].toEnumOrDefault(defaults.homePopupStyle),
                lockPopupStyle = prefs[Keys.LOCK_POPUP_STYLE].toEnumOrDefault(defaults.lockPopupStyle),
                popupCornerRadius = prefs[Keys.POPUP_CORNER] ?: defaults.popupCornerRadius,
                popupGlassOpacity = prefs[Keys.POPUP_OPACITY] ?: defaults.popupGlassOpacity,

                homePageCount = (prefs[Keys.HOME_PAGE_COUNT] ?: defaults.homePageCount).coerceIn(1, 7),
                homeGridColumns = (prefs[Keys.HOME_COLS] ?: defaults.homeGridColumns).coerceIn(3, 6),
                homeGridRows = (prefs[Keys.HOME_ROWS] ?: defaults.homeGridRows).coerceIn(4, 7),
                homePageScale = (prefs[Keys.HOME_PAGE_SCALE] ?: defaults.homePageScale).coerceIn(0.85f, 1.15f),
                lockPageCount = (prefs[Keys.LOCK_PAGE_COUNT] ?: defaults.lockPageCount).coerceIn(1, 4),
                lockClockStyle = prefs[Keys.LOCK_CLOCK_STYLE].toEnumOrDefault(defaults.lockClockStyle),
                lockClockColor = prefs[Keys.LOCK_CLOCK_COLOR].toEnumOrDefault(defaults.lockClockColor),
                lockClockScale = (prefs[Keys.LOCK_CLOCK_SCALE] ?: defaults.lockClockScale).coerceIn(0.55f, 1.85f),
                lockClockVerticalOffsetDp = (prefs[Keys.LOCK_CLOCK_V_OFFSET] ?: defaults.lockClockVerticalOffsetDp).coerceIn(-60f, 160f),
                lockClockWeight = (prefs[Keys.LOCK_CLOCK_WEIGHT] ?: defaults.lockClockWeight).coerceIn(300, 900),
                lockPageScale = (prefs[Keys.LOCK_PAGE_SCALE] ?: defaults.lockPageScale).coerceIn(0.85f, 1.12f),
                replaceSystemLockScreen = prefs[Keys.REPLACE_SYSTEM_LOCK] ?: defaults.replaceSystemLockScreen,
                doubleTapToLock = prefs[Keys.DOUBLE_TAP_LOCK] ?: defaults.doubleTapToLock,
                lockSecurityMode = prefs[Keys.LOCK_SECURITY_MODE].toEnumOrDefault(defaults.lockSecurityMode),
                lockPasscodeHash = prefs[Keys.LOCK_PASSCODE_HASH] ?: defaults.lockPasscodeHash,
                lockPasscodeLength = (prefs[Keys.LOCK_PASSCODE_LENGTH] ?: defaults.lockPasscodeLength).coerceIn(4, 6),

                pageTransitionStyle = prefs[Keys.TRANSITION_STYLE].toEnumOrDefault(defaults.pageTransitionStyle),
                transitionSpeed = prefs[Keys.TRANSITION_SPEED].toEnumOrDefault(defaults.transitionSpeed),
                showGlassySidebar = prefs[Keys.SHOW_GLASSY_SIDEBAR] ?: defaults.showGlassySidebar,
                sidebarSide = prefs[Keys.SIDEBAR_SIDE].toEnumOrDefault(defaults.sidebarSide),
                sidebarWidthDp = (prefs[Keys.SIDEBAR_WIDTH] ?: defaults.sidebarWidthDp).coerceIn(180f, 270f),
                sidebarGlassOpacity = (prefs[Keys.SIDEBAR_OPACITY] ?: defaults.sidebarGlassOpacity).coerceIn(0.45f, 0.96f),
                sidebarItems = prefs[Keys.SIDEBAR_ITEMS]?.let { decodeSidebarItems(it) }
                    ?: defaultSidebarItems(),
                ambientGlowIntensity = (prefs[Keys.AMBIENT_GLOW] ?: defaults.ambientGlowIntensity).coerceIn(0f, 1f),
                wallpaperPreset = prefs[Keys.WALLPAPER].toEnumOrDefault(defaults.wallpaperPreset),
                wallpaperConfig = prefs[Keys.WP_HOME_V2]?.let { decodeWallpaperConfig(it, defaults.wallpaperConfig) }
                    ?: defaults.wallpaperConfig.copy(
                        preset = prefs[Keys.WALLPAPER].toEnumOrDefault(defaults.wallpaperPreset)
                    ),
                separateLockWallpaper = prefs[Keys.WP_SEPARATE_LOCK] ?: defaults.separateLockWallpaper,
                lockWallpaperConfig = prefs[Keys.WP_LOCK_V2]?.let { decodeWallpaperConfig(it, defaults.lockWallpaperConfig) }
                    ?: defaults.lockWallpaperConfig
            )
        }

    suspend fun saveConfig(config: LauncherConfig) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.DOCK_LAYOUT] = config.dockLayout.name
            prefs[Keys.DOCK_COLOR] = config.dockColor.name
            prefs[Keys.DOCK_OPACITY] = config.dockOpacity
            prefs[Keys.DOCK_CORNER] = config.dockCornerRadius
            prefs[Keys.DOCK_MAX_ICONS] = config.dockMaxIcons
            prefs[Keys.SHOW_DOCK_LABELS] = config.showDockLabels
            prefs[Keys.PINNED_DOCK] = config.pinnedDockPackages.joinToString("|")

            prefs[Keys.WIDGET_STACK_MODE] = config.widgetStackMode.name
            prefs[Keys.ENABLED_WIDGETS] = config.enabledWidgets.joinToString("|") { it.name }
            prefs[Keys.HOME_WIDGETS_V2] = encodeHomeWidgets(config.homeWidgets)
            prefs[Keys.LOCK_WIDGETS_V1] = encodeHomeWidgets(config.lockWidgets)
            prefs[Keys.WIDGET_OPACITY] = config.widgetGlassOpacity
            prefs[Keys.QUICK_NOTE] = config.quickNoteText

            val sb = config.searchBar
            prefs[Keys.SB_ENABLED] = sb.enabledOnHome
            prefs[Keys.SB_POS] = sb.position.name
            prefs[Keys.SB_SHAPE] = sb.shape.name
            prefs[Keys.SB_COLOR] = sb.colorPreset.name
            prefs[Keys.SB_OPACITY] = sb.opacity
            prefs[Keys.SB_WIDTH] = sb.widthPercent
            prefs[Keys.SB_HEIGHT] = sb.heightDp
            prefs[Keys.SB_X] = sb.xOffsetDp
            prefs[Keys.SB_Y] = sb.yOffsetDp

            prefs[Keys.QP_ORIENTATION] = config.quickPanelOrientation.name
            prefs[Keys.QP_OPACITY] = config.quickPanelGlassOpacity
            prefs[Keys.QP_MODULES_V2] = encodeModulesV2(config.quickModules)
            prefs[Keys.QP_BRIGHTNESS] = config.showBrightnessSlider
            prefs[Keys.QP_VOLUME] = config.showVolumeSlider
            prefs[Keys.QP_MEDIA] = config.showMediaControl
            prefs[Keys.QP_SLIDER_HEIGHT] = config.sliderHeightDp
            prefs[Keys.QP_REPLACE_SYSTEM] = config.replaceSystemControlPanel
            prefs[Keys.QP_EDGE_ZONE] = config.edgeTriggerZone.name

            val di = config.dynamicIsland
            prefs[Keys.DI_ENABLED] = di.enabled
            prefs[Keys.DI_WIDTH] = di.widthDp
            prefs[Keys.DI_HEIGHT] = di.heightDp
            prefs[Keys.DI_TOP_OFFSET] = di.topOffsetDp
            prefs[Keys.DI_HORIZ_OFFSET] = di.horizontalOffsetDp
            prefs[Keys.DI_RADIUS] = di.cornerRadiusDp
            prefs[Keys.DI_STYLE] = di.style.name
            prefs[Keys.DI_GLOW] = di.borderGlow
            prefs[Keys.DI_LOCK_SCREEN] = di.showOnLockScreen
            prefs[Keys.DI_CLOCK] = di.showClock
            prefs[Keys.DI_BATTERY] = di.showBattery
            prefs[Keys.DI_NOTIF] = di.showNotifications
            prefs[Keys.DI_MEDIA] = di.showMedia
            prefs[Keys.DI_FOCUS] = di.showFocusTimer
            prefs[Keys.DI_AUTO_NOTIF] = di.autoExpandOnNotification
            prefs[Keys.DI_AUTO_SYS] = di.autoExpandOnSystemStatus
            prefs[Keys.DI_DUAL_SPLIT] = di.showDualSplitBubble

            prefs[Keys.HIDE_SYSTEM_STATUS_BAR] = config.hideSystemStatusBar
            prefs[Keys.STATUS_POSITION] = config.statusPosition.name
            prefs[Keys.STATUS_ALIGNMENT] = config.statusAlignment.name
            prefs[Keys.BATTERY_STYLE] = config.batteryStyle.name
            prefs[Keys.SHOW_CLOCK] = config.showClockIndicator
            prefs[Keys.SHOW_SIGNAL] = config.showSignalIndicator
            prefs[Keys.SHOW_WIFI] = config.showWifiIndicator
            prefs[Keys.SHOW_BATTERY_PCT] = config.showBatteryPercent

            prefs[Keys.REPLACE_SYSTEM_NOTIF] = config.replaceSystemNotifications
            prefs[Keys.SHOW_SYS_WIDE_ISLAND] = config.showSystemWideDynamicIsland
            prefs[Keys.ACTIVE_NOTIF_SOUND] = config.activeNotifSoundId
            prefs[Keys.CUSTOM_NOTIF_SOUNDS] = encodeCustomSounds(config.customNotifSounds)
            prefs[Keys.HOME_POPUP_STYLE] = config.homePopupStyle.name
            prefs[Keys.LOCK_POPUP_STYLE] = config.lockPopupStyle.name
            prefs[Keys.POPUP_CORNER] = config.popupCornerRadius
            prefs[Keys.POPUP_OPACITY] = config.popupGlassOpacity

            prefs[Keys.HOME_PAGE_COUNT] = config.homePageCount
            prefs[Keys.HOME_COLS] = config.homeGridColumns
            prefs[Keys.HOME_ROWS] = config.homeGridRows
            prefs[Keys.HOME_PAGE_SCALE] = config.homePageScale
            prefs[Keys.LOCK_PAGE_COUNT] = config.lockPageCount
            prefs[Keys.LOCK_CLOCK_STYLE] = config.lockClockStyle.name
            prefs[Keys.LOCK_CLOCK_COLOR] = config.lockClockColor.name
            prefs[Keys.LOCK_CLOCK_SCALE] = config.lockClockScale
            prefs[Keys.LOCK_CLOCK_V_OFFSET] = config.lockClockVerticalOffsetDp
            prefs[Keys.LOCK_CLOCK_WEIGHT] = config.lockClockWeight
            prefs[Keys.LOCK_PAGE_SCALE] = config.lockPageScale
            prefs[Keys.REPLACE_SYSTEM_LOCK] = config.replaceSystemLockScreen
            prefs[Keys.DOUBLE_TAP_LOCK] = config.doubleTapToLock
            prefs[Keys.LOCK_SECURITY_MODE] = config.lockSecurityMode.name
            prefs[Keys.LOCK_PASSCODE_HASH] = config.lockPasscodeHash
            prefs[Keys.LOCK_PASSCODE_LENGTH] = config.lockPasscodeLength

            prefs[Keys.TRANSITION_STYLE] = config.pageTransitionStyle.name
            prefs[Keys.TRANSITION_SPEED] = config.transitionSpeed.name
            prefs[Keys.SHOW_GLASSY_SIDEBAR] = config.showGlassySidebar
            prefs[Keys.SIDEBAR_SIDE] = config.sidebarSide.name
            prefs[Keys.SIDEBAR_WIDTH] = config.sidebarWidthDp
            prefs[Keys.SIDEBAR_OPACITY] = config.sidebarGlassOpacity
            prefs[Keys.SIDEBAR_ITEMS] = encodeSidebarItems(config.sidebarItems)
            prefs[Keys.AMBIENT_GLOW] = config.ambientGlowIntensity
            prefs[Keys.WALLPAPER] = config.wallpaperConfig.preset.name
            prefs[Keys.WP_HOME_V2] = encodeWallpaperConfig(config.wallpaperConfig)
            prefs[Keys.WP_SEPARATE_LOCK] = config.separateLockWallpaper
            prefs[Keys.WP_LOCK_V2] = encodeWallpaperConfig(config.lockWallpaperConfig)
        }
    }

    private fun encodeModulesV2(modules: List<QuickModuleConfig>): String =
        modules.joinToString("|") {
            "${it.type.name}:${it.enabled}:${"%.2f".format(Locale.US, it.widthPercent)}:${it.heightDp}"
        }

    private fun decodeModulesV2(raw: String): List<QuickModuleConfig>? {
        val parsed = raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 4) return@mapNotNull null
            val type = runCatching { QuickModuleType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val enabled = parts[1].toBooleanStrictOrNull() ?: true
            val width = parts[2].toFloatOrNull()?.coerceIn(0.23f, 1.0f) ?: 0.48f
            val height = parts[3].toIntOrNull()?.coerceIn(56, 220) ?: 78
            QuickModuleConfig(type, enabled, width, height)
        }
        if (parsed.isEmpty()) return null
        val existingTypes = parsed.map { it.type }.toSet()
        val missing = defaultQuickModules().filterNot { it.type in existingTypes }
        return parsed + missing
    }

    private fun encodeSidebarItems(items: List<SidebarItemConfig>): String =
        items.joinToString("|") {
            val safeId = it.id.replace("|", "_").replace(":", "_")
            val safePkg = it.packageName.orEmpty().replace("|", "").replace(":", "")
            "$safeId:${it.type.name}:$safePkg:${it.enabled}"
        }

    private fun decodeSidebarItems(raw: String): List<SidebarItemConfig>? {
        val parsed = raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 4) return@mapNotNull null
            val id = parts[0].ifBlank { return@mapNotNull null }
            val type = runCatching { SidebarItemType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val pkg = parts[2].ifBlank { null }
            val enabled = parts[3].toBooleanStrictOrNull() ?: true
            SidebarItemConfig(id, type, pkg, enabled)
        }
        return parsed.takeIf { it.isNotEmpty() }
    }

    private fun encodeHomeWidgets(widgets: List<HomeWidgetPlacement>): String =
        widgets.joinToString("|") { w ->
            val safeId = w.id.replace("|", "_").replace("~", "_")
            val builtIn = w.builtInType?.name.orEmpty()
            val awId = w.appWidgetId?.toString().orEmpty()
            val safeLabel = w.label.replace("|", " ").replace("~", " ")
            val widthStr = "%.2f".format(Locale.US, w.widthPercent)
            val xStr = "%.1f".format(Locale.US, w.xOffsetDp)
            val yStr = "%.1f".format(Locale.US, w.yOffsetDp)
            "$safeId~${w.pageIndex}~$builtIn~$awId~$safeLabel~${w.heightDp}~$widthStr~$xStr~$yStr"
        }

    private fun decodeHomeWidgets(raw: String): List<HomeWidgetPlacement>? {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split("~")
            if (parts.size < 6) return@mapNotNull null
            val id = parts[0].ifBlank { return@mapNotNull null }
            val pageIdx = parts[1].toIntOrNull()?.coerceIn(0, 6) ?: 0
            val builtIn = parts[2].takeIf { it.isNotBlank() }?.let {
                runCatching { WidgetType.valueOf(it) }.getOrNull()
            }
            val appWidgetId = parts[3].toIntOrNull()
            if (builtIn == null && appWidgetId == null) return@mapNotNull null
            val label = parts[4]
            val heightDp = parts[5].toIntOrNull()?.coerceIn(72, 420) ?: 148
            val widthPct = parts.getOrNull(6)?.toFloatOrNull()?.coerceIn(0.44f, 1.0f) ?: 1.0f
            val xOff = parts.getOrNull(7)?.toFloatOrNull()?.coerceIn(-120f, 120f) ?: 0f
            val yOff = parts.getOrNull(8)?.toFloatOrNull()?.coerceIn(-120f, 240f) ?: 0f
            HomeWidgetPlacement(id, pageIdx, builtIn, appWidgetId, label, widthPct, heightDp, xOff, yOff)
        }
    }

    private fun encodeCustomSounds(sounds: List<CustomNotificationSound>): String =
        sounds.joinToString("|") { s ->
            val safeId = s.id.replace("|", "_").replace("~", "_")
            val safeName = s.name.replace("|", " ").replace("~", " ")
            val safeUri = s.uriString.replace("|", "%7C").replace("~", "%7E")
            "$safeId~$safeName~$safeUri"
        }

    private fun decodeCustomSounds(raw: String): List<CustomNotificationSound> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split("~")
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].ifBlank { return@mapNotNull null }
            val name = parts[1].ifBlank { "Suara Kustom" }
            val uri = parts[2].ifBlank { return@mapNotNull null }
            CustomNotificationSound(id, name, uri)
        }
    }

    private fun encodeWallpaperConfig(wp: WallpaperConfig): String {
        val safePath = wp.customImagePath.replace("|", "%7C").replace("~", "%7E")
        return listOf(
            wp.preset.name,
            safePath,
            wp.glassStyle.name,
            "%.2f".format(Locale.US, wp.glassIntensity),
            "%.1f".format(Locale.US, wp.blurRadiusDp),
            "%.2f".format(Locale.US, wp.dimAlpha),
            wp.particleOverlay.name,
            wp.colorTone.name,
            wp.parallaxEnabled.toString(),
            "%.2f".format(Locale.US, wp.parallaxStrength),
            "%.2f".format(Locale.US, wp.liveSpeed)
        ).joinToString("~")
    }

    private fun decodeWallpaperConfig(raw: String, fallback: WallpaperConfig): WallpaperConfig {
        val parts = raw.split("~")
        if (parts.size < 11) return fallback
        return WallpaperConfig(
            preset = parts[0].toEnumOrDefault(fallback.preset),
            customImagePath = parts[1].replace("%7C", "|").replace("%7E", "~"),
            glassStyle = parts[2].toEnumOrDefault(fallback.glassStyle),
            glassIntensity = parts[3].toFloatOrNull()?.coerceIn(0f, 1f) ?: fallback.glassIntensity,
            blurRadiusDp = parts[4].toFloatOrNull()?.coerceIn(0f, 24f) ?: fallback.blurRadiusDp,
            dimAlpha = parts[5].toFloatOrNull()?.coerceIn(0f, 0.70f) ?: fallback.dimAlpha,
            particleOverlay = parts[6].toEnumOrDefault(fallback.particleOverlay),
            colorTone = parts[7].toEnumOrDefault(fallback.colorTone),
            parallaxEnabled = parts[8].toBooleanStrictOrNull() ?: fallback.parallaxEnabled,
            parallaxStrength = parts[9].toFloatOrNull()?.coerceIn(0f, 1f) ?: fallback.parallaxStrength,
            liveSpeed = parts[10].toFloatOrNull()?.coerceIn(0.3f, 2.0f) ?: fallback.liveSpeed
        )
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
