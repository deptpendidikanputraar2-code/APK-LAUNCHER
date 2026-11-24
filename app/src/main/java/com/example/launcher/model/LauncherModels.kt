package com.example.launcher.model

import android.app.PendingIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class DockLayoutMode(val label: String, val subtitle: String) {
    FLOATING_BOTTOM("Bawah Melayang", "Kapsul kaca di bawah layar"),
    FULL_EDGE_BOTTOM("Bawah Penuh", "Menempel penuh ke tepi bawah"),
    LEFT_RAIL("Rel Kiri", "Dock vertikal di sisi kiri"),
    RIGHT_RAIL("Rel Kanan", "Dock vertikal di sisi kanan")
}

enum class DockColorPreset(
    val label: String,
    val baseColor: Color,
    val borderColor: Color
) {
    FROSTED_CLEAR("Kaca Bening", Color(0xFFFFFFFF), Color(0x66FFFFFF)),
    MIDNIGHT_OBSIDIAN("Obsidian Gelap", Color(0xFF0B132B), Color(0x4438BDF8)),
    SAPPHIRE_GLASS("Kaca Safir", Color(0xFF1D4ED8), Color(0x6693C5FD)),
    EMERALD_MIST("Kabut Zamrud", Color(0xFF047857), Color(0x666EE7B7)),
    ROSE_QUARTZ("Kuarsa Mawar", Color(0xFFBE123C), Color(0x66FDA4AF)),
    AMBER_GLOW("Sinar Amber", Color(0xFFB45309), Color(0x66FDE68A))
}

enum class WidgetStackMode(val label: String, val description: String) {
    SMART_STACK("Tumpuk Pintar", "Geser vertikal untuk membalik widget dalam 1 kartu"),
    CARD_DECK("Tumpuk Kartu", "Kartu mengintip bertumpuk, ketuk untuk menggilir"),
    GRID_SEPARATE("Daftar Terpisah", "Tampilkan semua widget aktif secara terpisah")
}

enum class WidgetType(val label: String, val subtitle: String) {
    CLOCK_WEATHER("Jam & Cuaca", "Jam real-time & tanggal perangkat"),
    BATTERY_SYSTEM("Baterai & Suhu", "Status daya, pengisian & suhu nyata HP"),
    FOCUS_MEDIA("Media & Fokus", "Kontrol pemutar musik HP & timer fokus"),
    CALENDAR_MINI("Kalender Mini", "Hari & bulan tersinkron otomatis"),
    QUICK_NOTE("Catatan Cepat", "Catatan singkat yang selalu tampil"),
    LIVE_NOTIFICATIONS("Notifikasi HP", "Daftar notifikasi masuk secara real-time"),
    STORAGE_RAM("RAM & Penyimpanan", "Pantau memori & penyimpanan HP nyata"),
    APP_SHORTCUTS("Pintasan Cepat", "Akses 4 aplikasi teratas dalam 1 ketukan")
}

data class HomeWidgetPlacement(
    val id: String,
    val pageIndex: Int = 0,
    val builtInType: WidgetType? = null,
    val appWidgetId: Int? = null,
    val label: String = "",
    val heightDp: Int = 148
)

enum class QuickPanelOrientation(val label: String, val description: String) {
    VERTICAL_IOS("Vertikal (iPhone Glass)", "Grid vertikal ala pusat kontrol iOS"),
    HORIZONTAL_STRIP("Horizontal (Lanskap)", "Tata letak melebar mudah dijangkau jempol")
}

enum class EdgePanelTriggerZone(val label: String, val subtitle: String) {
    TOP_RIGHT("Sudut Kanan Atas", "Geser turun dari sudut kanan atas (ala iPhone)"),
    TOP_LEFT("Sudut Kiri Atas", "Geser turun dari sudut kiri atas layar"),
    TOP_FULL("Seluruh Tepi Atas", "Geser turun dari tepi atas mana saja")
}

enum class QuickModuleType(val label: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Data Seluler"),
    BLUETOOTH("Bluetooth"),
    FLASHLIGHT("Senter"),
    DND("Jangan Ganggu"),
    ROTATION("Rotasi"),
    AIRPLANE("Mode Pesawat"),
    HOTSPOT("Hotspot"),
    LOCATION("Lokasi"),
    BATTERY_SAVER("Hemat Daya"),
    SCREENSHOT("Tangkapan Layar"),
    CAMERA("Kamera"),
    LOCK_SCREEN("Kunci Layar")
}

data class QuickModuleConfig(
    val type: QuickModuleType,
    val enabled: Boolean = true,
    val widthPercent: Float = 0.48f, // 0.23f .. 1.0f freely customizable
    val heightDp: Int = 78           // 56 .. 220 dp freely customizable
)

enum class DynamicIslandStyle(
    val label: String,
    val containerColor: Color,
    val accentColor: Color
) {
    OBSIDIAN("Obsidian Hitam", Color(0xFF050812), Color(0xFF38BDF8)),
    FROSTED_GLASS("Kaca Bening", Color(0xFF1A2540), Color(0xFFFFFFFF)),
    AURORA_GLOW("Aurora Safir", Color(0xFF0A1633), Color(0xFF22D3EE)),
    SAPPHIRE("Biru Safir", Color(0xFF102A6B), Color(0xFF60A5FA)),
    EMERALD("Zamrud Neon", Color(0xFF062C22), Color(0xFF34D399)),
    ROSE_QUARTZ("Kuarsa Mawar", Color(0xFF340C1D), Color(0xFFFB7185))
}

data class DynamicIslandConfig(
    val enabled: Boolean = true,
    val widthDp: Float = 158f,
    val heightDp: Float = 34f,
    val topOffsetDp: Float = 8f,
    val horizontalOffsetDp: Float = 0f,
    val cornerRadiusDp: Float = 24f,
    val style: DynamicIslandStyle = DynamicIslandStyle.OBSIDIAN,
    val borderGlow: Float = 0.65f,
    val showOnLockScreen: Boolean = true,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showNotifications: Boolean = true,
    val showMedia: Boolean = true,
    val showFocusTimer: Boolean = true
)

enum class SidebarItemType(val defaultLabel: String) {
    HOME("Beranda"),
    WIDGETS("Widget"),
    QUICK_PANEL("Pusat Kontrol"),
    APPS("Semua Aplikasi"),
    NOTIFICATIONS("Notifikasi"),
    FLASHLIGHT("Senter"),
    LOCK_SCREEN("Layar Kunci"),
    LOCK_PHONE("Kunci HP"),
    STUDIO("Studio Vitro"),
    APP_SHORTCUT("Aplikasi")
}

data class SidebarItemConfig(
    val id: String,
    val type: SidebarItemType,
    val packageName: String? = null,
    val enabled: Boolean = true
)

enum class StatusIndicatorPosition(val label: String, val description: String) {
    TOP_STATUS_BAR("Status Bar Atas", "Baris atas klasik bebas tumpang tindih"),
    DYNAMIC_PILL("Pulau Kaca Atas", "Kapsul kaca mengambang di tengah atas"),
    INSIDE_DOCK("Terpadu di Dock", "Indikator ringkas menyatu dengan Dock Bar"),
    BOTTOM_CAPSULE("Kapsul Sudut Bawah", "Lencana mengambang di sudut kanan bawah"),
    HIDDEN("Tersembunyi", "Tampilan layar penuh tanpa indikator")
}

enum class StatusSlotAlignment(val label: String) {
    CLASSIC_SPLIT("Jam Kiri • Sinyal Kanan"),
    CENTERED_PILL("Rata Tengah Terpadu"),
    REVERSED_SPLIT("Sinyal Kiri • Jam Kanan")
}

enum class BatteryIconStyle(val label: String) {
    CAPSULE("Kapsul"),
    RING("Lingkaran"),
    TEXT_ONLY("Angka Saja")
}

enum class PopupStyle(val label: String, val description: String) {
    DYNAMIC_ISLAND("Kapsul Dinamis", "Melebar halus dari kapsul kaca di atas"),
    FLOATING_GLASS_CARD("Kartu Kaca Melayang", "Kartu frosted elegan dengan garis tepi berkilau"),
    BOTTOM_GLASS_SHEET("Lembar Bawah", "Muncul lembut dari area bawah layar"),
    STACKED_DECK("Tumpuk Notifikasi", "Kartu bertumpuk bergaya iOS yang bisa diperluas"),
    COMPACT_BUBBLE("Gelembung Ringkas", "Pil minimalis tidak mengganggu layar")
}

enum class PageTransitionStyle(val label: String, val description: String) {
    FLUID_GLASS("Kaca Mengalir", "Paralaks lembut dengan skala & transparansi"),
    CUBE_3D("Kubus 3D", "Rotasi perspektif 3D antar halaman"),
    DEPTH_STACK("Tumpukan Kedalaman", "Halaman baru muncul dari lapisan belakang"),
    SMOOTH_FAN("Kipas Halus", "Ayunan melengkung halus mengikuti jari"),
    ELASTIC_SLIDE("Geser Elastis", "Respons cepat dengan efek pegas ringan")
}

enum class TransitionSpeed(val label: String, val stiffness: Float, val dampingRatio: Float) {
    SNAPPY("Cepat", 620f, 0.82f),
    BALANCED("Seimbang", 380f, 0.76f),
    CINEMATIC("Sinematik", 210f, 0.85f)
}

enum class WallpaperPreset(val label: String, val subtitle: String) {
    SYSTEM_LIVE("Wallpaper Asli HP", "Sinkron langsung dengan wallpaper bawaan / live HP Anda"),
    AURORA("Aurora Safir", "Latar aurora kaca gelap beresolusi tinggi"),
    PRISM("Kristal Prisma", "Refraksi cahaya kristal lembut untuk frosted glass"),
    DUSK("Senja Alpin", "Gradasi senja atmosferik tenang"),
    OBSIDIAN_MESH("Obsidian Kaca", "Mesh gelap minimalis hemat baterai AMOLED")
}

enum class SidebarSide(val label: String) {
    LEFT("Sisi Kiri"),
    RIGHT("Sisi Kanan")
}

enum class LockSecurityMode(val label: String, val subtitle: String) {
    NONE("Tanpa Sandi", "Geser ke atas langsung membuka layar"),
    PIN("PIN Angka (4–6 Digit)", "Papan angka kaca ala iOS untuk membuka layar kunci"),
    PASSWORD("Sandi Teks / Alfanumerik", "Kata sandi huruf & angka kustom")
}

enum class HomeNavTab(val label: String) {
    HOME("Beranda"),
    APPS("Aplikasi"),
    PANEL("Panel"),
    LOCK("Kunci"),
    STUDIO("Studio")
}

fun defaultQuickModules(): List<QuickModuleConfig> = listOf(
    QuickModuleConfig(QuickModuleType.WIFI, true, 0.48f, 78),
    QuickModuleConfig(QuickModuleType.CELLULAR, true, 0.48f, 78),
    QuickModuleConfig(QuickModuleType.BLUETOOTH, true, 0.31f, 78),
    QuickModuleConfig(QuickModuleType.FLASHLIGHT, true, 0.31f, 78),
    QuickModuleConfig(QuickModuleType.DND, true, 0.31f, 78),
    QuickModuleConfig(QuickModuleType.ROTATION, true, 0.48f, 74),
    QuickModuleConfig(QuickModuleType.LOCK_SCREEN, true, 0.48f, 74),
    QuickModuleConfig(QuickModuleType.SCREENSHOT, true, 0.48f, 74),
    QuickModuleConfig(QuickModuleType.CAMERA, true, 0.48f, 74),
    QuickModuleConfig(QuickModuleType.AIRPLANE, false, 0.31f, 74),
    QuickModuleConfig(QuickModuleType.HOTSPOT, false, 0.48f, 74),
    QuickModuleConfig(QuickModuleType.LOCATION, false, 0.31f, 74),
    QuickModuleConfig(QuickModuleType.BATTERY_SAVER, false, 0.48f, 74)
)

fun defaultSidebarItems(): List<SidebarItemConfig> = listOf(
    SidebarItemConfig("home", SidebarItemType.HOME, null, true),
    SidebarItemConfig("widgets", SidebarItemType.WIDGETS, null, true),
    SidebarItemConfig("panel", SidebarItemType.QUICK_PANEL, null, true),
    SidebarItemConfig("apps", SidebarItemType.APPS, null, true),
    SidebarItemConfig("notif", SidebarItemType.NOTIFICATIONS, null, true),
    SidebarItemConfig("flashlight", SidebarItemType.FLASHLIGHT, null, false),
    SidebarItemConfig("lock_screen", SidebarItemType.LOCK_SCREEN, null, true),
    SidebarItemConfig("lock_phone", SidebarItemType.LOCK_PHONE, null, false),
    SidebarItemConfig("studio", SidebarItemType.STUDIO, null, true)
)

fun defaultHomeWidgetPlacements(): List<HomeWidgetPlacement> = listOf(
    HomeWidgetPlacement("w_clock_0", pageIndex = 0, builtInType = WidgetType.CLOCK_WEATHER, heightDp = 128),
    HomeWidgetPlacement("w_notif_0", pageIndex = 0, builtInType = WidgetType.LIVE_NOTIFICATIONS, heightDp = 160),
    HomeWidgetPlacement("w_battery_1", pageIndex = 1, builtInType = WidgetType.BATTERY_SYSTEM, heightDp = 124),
    HomeWidgetPlacement("w_storage_1", pageIndex = 1, builtInType = WidgetType.STORAGE_RAM, heightDp = 132),
    HomeWidgetPlacement("w_calendar_2", pageIndex = 2, builtInType = WidgetType.CALENDAR_MINI, heightDp = 124)
)

data class LauncherConfig(
    // 1. Dock Bar
    val dockLayout: DockLayoutMode = DockLayoutMode.FLOATING_BOTTOM,
    val dockColor: DockColorPreset = DockColorPreset.FROSTED_CLEAR,
    val dockOpacity: Float = 0.24f,
    val dockCornerRadius: Float = 28f,
    val dockMaxIcons: Int = 5,
    val showDockLabels: Boolean = false,
    val pinnedDockPackages: List<String> = emptyList(),

    // 2. Widgets (Built-in Glass + Real Android AppWidgets)
    val widgetStackMode: WidgetStackMode = WidgetStackMode.SMART_STACK,
    val enabledWidgets: List<WidgetType> = listOf(
        WidgetType.CLOCK_WEATHER,
        WidgetType.BATTERY_SYSTEM,
        WidgetType.FOCUS_MEDIA,
        WidgetType.CALENDAR_MINI,
        WidgetType.LIVE_NOTIFICATIONS,
        WidgetType.STORAGE_RAM,
        WidgetType.QUICK_NOTE
    ),
    val homeWidgets: List<HomeWidgetPlacement> = defaultHomeWidgetPlacements(),
    val widgetGlassOpacity: Float = 0.22f,
    val quickNoteText: String = "Ketuk 2x layar beranda untuk mengunci HP, atau buka Sidebar untuk atur tampilan.",

    // 3. Quick Panel (Pure iPhone Glass Control Center - Zero Settings Inside!)
    val quickPanelOrientation: QuickPanelOrientation = QuickPanelOrientation.VERTICAL_IOS,
    val quickPanelGlassOpacity: Float = 0.26f,
    val quickModules: List<QuickModuleConfig> = defaultQuickModules(),
    val showBrightnessSlider: Boolean = true,
    val showVolumeSlider: Boolean = true,
    val showMediaControl: Boolean = true,
    val sliderHeightDp: Int = 152,
    val replaceSystemControlPanel: Boolean = true,
    val edgeTriggerZone: EdgePanelTriggerZone = EdgePanelTriggerZone.TOP_RIGHT,

    // 4. Dynamic Island (Fully Customizable)
    val dynamicIsland: DynamicIslandConfig = DynamicIslandConfig(),

    // 5. Status Indicators (Hide native status bar to eliminate overlap!)
    val hideSystemStatusBar: Boolean = true,
    val statusPosition: StatusIndicatorPosition = StatusIndicatorPosition.TOP_STATUS_BAR,
    val statusAlignment: StatusSlotAlignment = StatusSlotAlignment.CLASSIC_SPLIT,
    val batteryStyle: BatteryIconStyle = BatteryIconStyle.CAPSULE,
    val showClockIndicator: Boolean = true,
    val showSignalIndicator: Boolean = true,
    val showWifiIndicator: Boolean = true,
    val showBatteryPercent: Boolean = true,

    // 6. Pop-Up Styles (Home & Lock Screen)
    val homePopupStyle: PopupStyle = PopupStyle.DYNAMIC_ISLAND,
    val lockPopupStyle: PopupStyle = PopupStyle.STACKED_DECK,
    val popupCornerRadius: Float = 26f,
    val popupGlassOpacity: Float = 0.30f,

    // 7. Pages, Double-Tap Lock & Lock Screen Security Passcode
    val homePageCount: Int = 3,
    val homeGridColumns: Int = 4,
    val homeGridRows: Int = 5,
    val homePageScale: Float = 1.0f,
    val lockPageCount: Int = 3,
    val lockClockScale: Float = 1.0f,
    val lockPageScale: Float = 1.0f,
    val doubleTapToLock: Boolean = true,
    val lockSecurityMode: LockSecurityMode = LockSecurityMode.NONE,
    val lockPasscodeHash: String = "",
    val lockPasscodeLength: Int = 4,

    // 8. Page Transitions
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.FLUID_GLASS,
    val transitionSpeed: TransitionSpeed = TransitionSpeed.BALANCED,

    // 9. Customizable Glassy Sidebar v7
    val showGlassySidebar: Boolean = true,
    val sidebarSide: SidebarSide = SidebarSide.LEFT,
    val sidebarWidthDp: Float = 228f,
    val sidebarGlassOpacity: Float = 0.84f,
    val sidebarItems: List<SidebarItemConfig> = defaultSidebarItems(),
    val ambientGlowIntensity: Float = 0.80f,

    // 10. Visual Theme
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.AURORA
)

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?
)

data class LiveNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMillis: Long,
    val isOngoing: Boolean = false,
    val isMedia: Boolean = false,
    val appIcon: ImageBitmap? = null,
    val contentIntent: PendingIntent? = null
)

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val appName: String = "",
    val isPlaying: Boolean = false,
    val hasActiveSession: Boolean = false
)

data class SystemSyncStatus(
    val isDefaultLauncher: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isWriteSettingsGranted: Boolean = false,
    val isDndPolicyGranted: Boolean = false
)

data class DeviceTelemetry(
    val timeText: String = "09:41",
    val dateText: String = "Senin, 23 Nov",
    val monthText: String = "November 2026",
    val dayNumber: String = "23",
    val batteryPercent: Int = 88,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 31.5f,
    val isWifiConnected: Boolean = true,
    val isCellularConnected: Boolean = true,
    val networkLabel: String = "Wi-Fi",
    val signalBars: Int = 4,
    val volumeFraction: Float = 0.65f,
    val brightnessFraction: Float = 0.72f,
    val isFlashlightOn: Boolean = false,
    val isDndEnabled: Boolean = false,
    val isAutoRotateEnabled: Boolean = true,
    val isBluetoothOn: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val isLocationEnabled: Boolean = true,
    val ramUsedGb: Float = 3.2f,
    val ramTotalGb: Float = 8.0f,
    val storageUsedGb: Float = 54.0f,
    val storageTotalGb: Float = 128.0f
)

data class PopupMessage(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val body: String,
    val badge: String,
    val targetApp: InstalledApp? = null,
    val liveNotification: LiveNotification? = null,
    val isLockScreenContext: Boolean = false
)
