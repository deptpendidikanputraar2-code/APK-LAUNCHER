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
import com.example.launcher.model.DockColorPreset
import com.example.launcher.model.DockLayoutMode
import com.example.launcher.model.LauncherConfig
import com.example.launcher.model.ModuleSize
import com.example.launcher.model.PageTransitionStyle
import com.example.launcher.model.PopupStyle
import com.example.launcher.model.QuickModuleConfig
import com.example.launcher.model.QuickModuleType
import com.example.launcher.model.QuickPanelOrientation
import com.example.launcher.model.SidebarSide
import com.example.launcher.model.StatusIndicatorPosition
import com.example.launcher.model.StatusSlotAlignment
import com.example.launcher.model.TransitionSpeed
import com.example.launcher.model.WallpaperPreset
import com.example.launcher.model.WidgetStackMode
import com.example.launcher.model.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

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
        val WIDGET_OPACITY = floatPreferencesKey("widget_opacity")
        val QUICK_NOTE = stringPreferencesKey("quick_note")

        val QP_ORIENTATION = stringPreferencesKey("qp_orientation")
        val QP_OPACITY = floatPreferencesKey("qp_opacity")
        val QP_MODULES = stringPreferencesKey("qp_modules")
        val QP_BRIGHTNESS = booleanPreferencesKey("qp_brightness")
        val QP_VOLUME = booleanPreferencesKey("qp_volume")
        val QP_MEDIA = booleanPreferencesKey("qp_media")

        val STATUS_POSITION = stringPreferencesKey("status_position")
        val STATUS_ALIGNMENT = stringPreferencesKey("status_alignment")
        val BATTERY_STYLE = stringPreferencesKey("battery_style")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_SIGNAL = booleanPreferencesKey("show_signal")
        val SHOW_WIFI = booleanPreferencesKey("show_wifi")
        val SHOW_BATTERY_PCT = booleanPreferencesKey("show_battery_pct")

        val HOME_POPUP_STYLE = stringPreferencesKey("home_popup_style")
        val LOCK_POPUP_STYLE = stringPreferencesKey("lock_popup_style")
        val POPUP_CORNER = floatPreferencesKey("popup_corner")
        val POPUP_OPACITY = floatPreferencesKey("popup_opacity")

        val HOME_PAGE_COUNT = intPreferencesKey("home_page_count")
        val HOME_COLS = intPreferencesKey("home_cols")
        val HOME_ROWS = intPreferencesKey("home_rows")
        val HOME_PAGE_SCALE = floatPreferencesKey("home_page_scale")
        val LOCK_PAGE_COUNT = intPreferencesKey("lock_page_count")
        val LOCK_CLOCK_SCALE = floatPreferencesKey("lock_clock_scale")
        val LOCK_PAGE_SCALE = floatPreferencesKey("lock_page_scale")

        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val TRANSITION_SPEED = stringPreferencesKey("transition_speed")
        val SHOW_GLASSY_SIDEBAR = booleanPreferencesKey("show_glassy_sidebar")
        val SIDEBAR_SIDE = stringPreferencesKey("sidebar_side")
        val AMBIENT_GLOW = floatPreferencesKey("ambient_glow_intensity")
        val SHOW_BOTTOM_TAB_BAR = booleanPreferencesKey("show_glass_bottom_tab_bar")
        val WALLPAPER = stringPreferencesKey("wallpaper")
    }

    val configFlow: Flow<LauncherConfig> = context.launcherDataStore.data
        .catch { e ->
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw e
        }
        .map { prefs ->
            val defaults = LauncherConfig()
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
                widgetGlassOpacity = prefs[Keys.WIDGET_OPACITY] ?: defaults.widgetGlassOpacity,
                quickNoteText = prefs[Keys.QUICK_NOTE] ?: defaults.quickNoteText,

                quickPanelOrientation = prefs[Keys.QP_ORIENTATION].toEnumOrDefault(defaults.quickPanelOrientation),
                quickPanelGlassOpacity = prefs[Keys.QP_OPACITY] ?: defaults.quickPanelGlassOpacity,
                quickModules = prefs[Keys.QP_MODULES]?.let { decodeModules(it) } ?: defaults.quickModules,
                showBrightnessSlider = prefs[Keys.QP_BRIGHTNESS] ?: defaults.showBrightnessSlider,
                showVolumeSlider = prefs[Keys.QP_VOLUME] ?: defaults.showVolumeSlider,
                showMediaControl = prefs[Keys.QP_MEDIA] ?: defaults.showMediaControl,

                statusPosition = prefs[Keys.STATUS_POSITION].toEnumOrDefault(defaults.statusPosition),
                statusAlignment = prefs[Keys.STATUS_ALIGNMENT].toEnumOrDefault(defaults.statusAlignment),
                batteryStyle = prefs[Keys.BATTERY_STYLE].toEnumOrDefault(defaults.batteryStyle),
                showClockIndicator = prefs[Keys.SHOW_CLOCK] ?: defaults.showClockIndicator,
                showSignalIndicator = prefs[Keys.SHOW_SIGNAL] ?: defaults.showSignalIndicator,
                showWifiIndicator = prefs[Keys.SHOW_WIFI] ?: defaults.showWifiIndicator,
                showBatteryPercent = prefs[Keys.SHOW_BATTERY_PCT] ?: defaults.showBatteryPercent,

                homePopupStyle = prefs[Keys.HOME_POPUP_STYLE].toEnumOrDefault(defaults.homePopupStyle),
                lockPopupStyle = prefs[Keys.LOCK_POPUP_STYLE].toEnumOrDefault(defaults.lockPopupStyle),
                popupCornerRadius = prefs[Keys.POPUP_CORNER] ?: defaults.popupCornerRadius,
                popupGlassOpacity = prefs[Keys.POPUP_OPACITY] ?: defaults.popupGlassOpacity,

                homePageCount = (prefs[Keys.HOME_PAGE_COUNT] ?: defaults.homePageCount).coerceIn(1, 7),
                homeGridColumns = (prefs[Keys.HOME_COLS] ?: defaults.homeGridColumns).coerceIn(3, 6),
                homeGridRows = (prefs[Keys.HOME_ROWS] ?: defaults.homeGridRows).coerceIn(4, 7),
                homePageScale = (prefs[Keys.HOME_PAGE_SCALE] ?: defaults.homePageScale).coerceIn(0.85f, 1.15f),
                lockPageCount = (prefs[Keys.LOCK_PAGE_COUNT] ?: defaults.lockPageCount).coerceIn(1, 4),
                lockClockScale = (prefs[Keys.LOCK_CLOCK_SCALE] ?: defaults.lockClockScale).coerceIn(0.8f, 1.25f),
                lockPageScale = (prefs[Keys.LOCK_PAGE_SCALE] ?: defaults.lockPageScale).coerceIn(0.85f, 1.12f),

                pageTransitionStyle = prefs[Keys.TRANSITION_STYLE].toEnumOrDefault(defaults.pageTransitionStyle),
                transitionSpeed = prefs[Keys.TRANSITION_SPEED].toEnumOrDefault(defaults.transitionSpeed),
                showGlassySidebar = prefs[Keys.SHOW_GLASSY_SIDEBAR] ?: defaults.showGlassySidebar,
                sidebarSide = prefs[Keys.SIDEBAR_SIDE].toEnumOrDefault(defaults.sidebarSide),
                ambientGlowIntensity = (prefs[Keys.AMBIENT_GLOW] ?: defaults.ambientGlowIntensity).coerceIn(0f, 1f),
                showGlassBottomTabBar = prefs[Keys.SHOW_BOTTOM_TAB_BAR] ?: defaults.showGlassBottomTabBar,
                wallpaperPreset = prefs[Keys.WALLPAPER].toEnumOrDefault(defaults.wallpaperPreset)
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
            prefs[Keys.WIDGET_OPACITY] = config.widgetGlassOpacity
            prefs[Keys.QUICK_NOTE] = config.quickNoteText

            prefs[Keys.QP_ORIENTATION] = config.quickPanelOrientation.name
            prefs[Keys.QP_OPACITY] = config.quickPanelGlassOpacity
            prefs[Keys.QP_MODULES] = encodeModules(config.quickModules)
            prefs[Keys.QP_BRIGHTNESS] = config.showBrightnessSlider
            prefs[Keys.QP_VOLUME] = config.showVolumeSlider
            prefs[Keys.QP_MEDIA] = config.showMediaControl

            prefs[Keys.STATUS_POSITION] = config.statusPosition.name
            prefs[Keys.STATUS_ALIGNMENT] = config.statusAlignment.name
            prefs[Keys.BATTERY_STYLE] = config.batteryStyle.name
            prefs[Keys.SHOW_CLOCK] = config.showClockIndicator
            prefs[Keys.SHOW_SIGNAL] = config.showSignalIndicator
            prefs[Keys.SHOW_WIFI] = config.showWifiIndicator
            prefs[Keys.SHOW_BATTERY_PCT] = config.showBatteryPercent

            prefs[Keys.HOME_POPUP_STYLE] = config.homePopupStyle.name
            prefs[Keys.LOCK_POPUP_STYLE] = config.lockPopupStyle.name
            prefs[Keys.POPUP_CORNER] = config.popupCornerRadius
            prefs[Keys.POPUP_OPACITY] = config.popupGlassOpacity

            prefs[Keys.HOME_PAGE_COUNT] = config.homePageCount
            prefs[Keys.HOME_COLS] = config.homeGridColumns
            prefs[Keys.HOME_ROWS] = config.homeGridRows
            prefs[Keys.HOME_PAGE_SCALE] = config.homePageScale
            prefs[Keys.LOCK_PAGE_COUNT] = config.lockPageCount
            prefs[Keys.LOCK_CLOCK_SCALE] = config.lockClockScale
            prefs[Keys.LOCK_PAGE_SCALE] = config.lockPageScale

            prefs[Keys.TRANSITION_STYLE] = config.pageTransitionStyle.name
            prefs[Keys.TRANSITION_SPEED] = config.transitionSpeed.name
            prefs[Keys.SHOW_GLASSY_SIDEBAR] = config.showGlassySidebar
            prefs[Keys.SIDEBAR_SIDE] = config.sidebarSide.name
            prefs[Keys.AMBIENT_GLOW] = config.ambientGlowIntensity
            prefs[Keys.SHOW_BOTTOM_TAB_BAR] = config.showGlassBottomTabBar
            prefs[Keys.WALLPAPER] = config.wallpaperPreset.name
        }
    }

    private fun encodeModules(modules: List<QuickModuleConfig>): String =
        modules.joinToString("|") { "${it.type.name}:${it.enabled}:${it.size.name}" }

    private fun decodeModules(raw: String): List<QuickModuleConfig>? {
        val parsed = raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 3) return@mapNotNull null
            val type = runCatching { QuickModuleType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val enabled = parts[1].toBooleanStrictOrNull() ?: true
            val size = runCatching { ModuleSize.valueOf(parts[2]) }.getOrNull() ?: ModuleSize.SMALL_1X1
            QuickModuleConfig(type, enabled, size)
        }
        return parsed.takeIf { it.isNotEmpty() }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
