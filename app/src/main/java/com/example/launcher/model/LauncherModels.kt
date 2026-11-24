package com.example.launcher.model

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
    GRID_SEPARATE("Grid Terpisah", "Tampilkan widget aktif berdampingan secara terpisah")
}

enum class WidgetType(val label: String) {
    CLOCK_WEATHER("Jam & Cuaca"),
    BATTERY_SYSTEM("Baterai & Sistem"),
    FOCUS_MEDIA("Media & Fokus"),
    CALENDAR_MINI("Kalender Mini"),
    QUICK_NOTE("Catatan Cepat")
}

enum class QuickPanelOrientation(val label: String, val description: String) {
    VERTICAL_IOS("Vertikal (iPhone Glass)", "Grid vertikal ala pusat kontrol iOS"),
    HORIZONTAL_STRIP("Horizontal (Lanskap)", "Tata letak melebar mudah dijangkau jempol")
}

enum class ModuleSize(val label: String, val colSpan: Int, val rowSpan: Int) {
    SMALL_1X1("1×1", 1, 1),
    WIDE_2X1("2×1 Horizontal", 2, 1),
    TALL_1X2("1×2 Vertikal", 1, 2)
}

enum class QuickModuleType(val label: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Data Seluler"),
    BLUETOOTH("Bluetooth"),
    FLASHLIGHT("Senter"),
    DND("Jangan Ganggu"),
    ROTATION("Rotasi"),
    LOCK_SCREEN("Layar Kunci"),
    STUDIO("Studio Vitro")
}

data class QuickModuleConfig(
    val type: QuickModuleType,
    val enabled: Boolean = true,
    val size: ModuleSize = ModuleSize.SMALL_1X1
)

enum class StatusIndicatorPosition(val label: String, val description: String) {
    TOP_STATUS_BAR("Status Bar Atas", "Baris atas klasik di dekat tepi atas"),
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

enum class WallpaperPreset(val label: String) {
    AURORA("Aurora Safir"),
    PRISM("Kristal Prisma"),
    DUSK("Senja Alpin"),
    OBSIDIAN_MESH("Obsidian Kaca")
}

enum class SidebarSide(val label: String) {
    LEFT("Sisi Kiri"),
    RIGHT("Sisi Kanan")
}

enum class HomeNavTab(val label: String) {
    HOME("Beranda"),
    APPS("Aplikasi"),
    PANEL("Panel"),
    LOCK("Kunci"),
    STUDIO("Studio")
}

data class LauncherConfig(
    // 1. Dock Bar
    val dockLayout: DockLayoutMode = DockLayoutMode.FLOATING_BOTTOM,
    val dockColor: DockColorPreset = DockColorPreset.FROSTED_CLEAR,
    val dockOpacity: Float = 0.24f,
    val dockCornerRadius: Float = 28f,
    val dockMaxIcons: Int = 5,
    val showDockLabels: Boolean = false,
    val pinnedDockPackages: List<String> = emptyList(),

    // 2. Widgets
    val widgetStackMode: WidgetStackMode = WidgetStackMode.SMART_STACK,
    val enabledWidgets: List<WidgetType> = listOf(
        WidgetType.CLOCK_WEATHER,
        WidgetType.BATTERY_SYSTEM,
        WidgetType.FOCUS_MEDIA,
        WidgetType.CALENDAR_MINI,
        WidgetType.QUICK_NOTE
    ),
    val widgetGlassOpacity: Float = 0.22f,
    val quickNoteText: String = "Atur dock, tumpuk widget, atau geser turun untuk Pusat Kontrol kaca.",

    // 3. Quick Panel (iPhone Glass)
    val quickPanelOrientation: QuickPanelOrientation = QuickPanelOrientation.VERTICAL_IOS,
    val quickPanelGlassOpacity: Float = 0.26f,
    val quickModules: List<QuickModuleConfig> = listOf(
        QuickModuleConfig(QuickModuleType.WIFI, true, ModuleSize.WIDE_2X1),
        QuickModuleConfig(QuickModuleType.CELLULAR, true, ModuleSize.SMALL_1X1),
        QuickModuleConfig(QuickModuleType.BLUETOOTH, true, ModuleSize.SMALL_1X1),
        QuickModuleConfig(QuickModuleType.FLASHLIGHT, true, ModuleSize.TALL_1X2),
        QuickModuleConfig(QuickModuleType.DND, true, ModuleSize.SMALL_1X1),
        QuickModuleConfig(QuickModuleType.ROTATION, true, ModuleSize.SMALL_1X1),
        QuickModuleConfig(QuickModuleType.LOCK_SCREEN, true, ModuleSize.WIDE_2X1),
        QuickModuleConfig(QuickModuleType.STUDIO, true, ModuleSize.WIDE_2X1)
    ),
    val showBrightnessSlider: Boolean = true,
    val showVolumeSlider: Boolean = true,
    val showMediaControl: Boolean = true,

    // 4. Status Indicators (Signal, Battery, Clock)
    val statusPosition: StatusIndicatorPosition = StatusIndicatorPosition.TOP_STATUS_BAR,
    val statusAlignment: StatusSlotAlignment = StatusSlotAlignment.CLASSIC_SPLIT,
    val batteryStyle: BatteryIconStyle = BatteryIconStyle.CAPSULE,
    val showClockIndicator: Boolean = true,
    val showSignalIndicator: Boolean = true,
    val showWifiIndicator: Boolean = true,
    val showBatteryPercent: Boolean = true,

    // 5. Pop-Up Styles (Home & Lock Screen)
    val homePopupStyle: PopupStyle = PopupStyle.DYNAMIC_ISLAND,
    val lockPopupStyle: PopupStyle = PopupStyle.STACKED_DECK,
    val popupCornerRadius: Float = 26f,
    val popupGlassOpacity: Float = 0.30f,

    // 6. Pages & Page Sizes (Home & Lock Screen)
    val homePageCount: Int = 3,
    val homeGridColumns: Int = 4,
    val homeGridRows: Int = 5,
    val homePageScale: Float = 1.0f, // 0.85f .. 1.15f
    val lockPageCount: Int = 3,
    val lockClockScale: Float = 1.0f, // 0.80f .. 1.25f
    val lockPageScale: Float = 1.0f, // 0.85f .. 1.12f

    // 7. Page Transitions
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.FLUID_GLASS,
    val transitionSpeed: TransitionSpeed = TransitionSpeed.BALANCED,

    // 8. Glassy Tab-Sidebar v7 (above Dock Bar), Ambient Glow & Optional Extra Bottom Tab Bar
    val showGlassySidebar: Boolean = true,
    val sidebarSide: SidebarSide = SidebarSide.LEFT,
    val ambientGlowIntensity: Float = 0.80f,
    val showGlassBottomTabBar: Boolean = false,

    // Visual Theme
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.AURORA
)

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?
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
    val signalBars: Int = 4,
    val volumeFraction: Float = 0.65f,
    val brightnessFraction: Float = 0.72f,
    val isFlashlightOn: Boolean = false,
    val isDndEnabled: Boolean = false,
    val isAutoRotateEnabled: Boolean = true,
    val isBluetoothVisualOn: Boolean = true
)

data class PopupMessage(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val body: String,
    val badge: String,
    val targetApp: InstalledApp? = null,
    val isLockScreenContext: Boolean = false
)
