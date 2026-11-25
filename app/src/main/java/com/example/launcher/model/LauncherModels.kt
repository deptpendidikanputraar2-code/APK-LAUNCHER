package com.example.launcher.model

import android.app.PendingIntent
import android.media.AudioManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class DockLayoutMode(val label: String, val subtitle: String) {
    FLOATING_BOTTOM("Bawah Melayang", "Kapsul kaca di bawah layar"),
    FULL_EDGE_BOTTOM("Bawah Penuh", "Menempel penuh ke tepi bawah"),
    LEFT_RAIL("Rel Kiri", "Dock vertikal melayang di sisi kiri"),
    RIGHT_RAIL("Rel Kanan", "Dock vertikal melayang di sisi kanan")
}

enum class DockSideAlignment(val label: String) {
    TOP("Atas"),
    CENTER("Tengah"),
    BOTTOM("Bawah")
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
    ANALOG_CLOCK("Jam Analog Kaca", "Jarum detik live dengan dial Liquid Glass"),
    WORLD_CLOCK("Jam Dunia", "Jakarta, Tokyo & London secara real-time"),
    WEATHER_PRO("Cuaca & Atmosfer", "Prakiraan harian, UV & kelembapan estetis"),
    BATTERY_SYSTEM("Baterai & Suhu", "Status daya, pengisian & suhu nyata HP"),
    FOCUS_MEDIA("Media & Fokus", "Kontrol pemutar musik HP & timer fokus"),
    MEDIA_WAVEFORM("Musik Pro Waveform", "Visualisator spektrum musik ala iPhone 18 Pro"),
    CALENDAR_MINI("Kalender Mini", "Hari & bulan tersinkron otomatis"),
    QUICK_NOTE("Catatan Cepat", "Catatan singkat yang bisa diedit langsung"),
    LIVE_NOTIFICATIONS("Notifikasi HP", "Daftar notifikasi masuk secara real-time"),
    STORAGE_RAM("RAM & Penyimpanan", "Pantau memori & penyimpanan HP nyata"),
    APP_SHORTCUTS("Pintasan Cepat", "Akses 4 aplikasi teratas dalam 1 ketukan"),
    DEVICE_RADAR("Radar Perangkat", "Ringkasan Wi-Fi, sinyal, baterai & status sistem"),
    DAILY_QUOTE("Kutipan Estetik", "Inspirasi harian dengan sentuhan kaca cair")
}

data class HomeWidgetPlacement(
    val id: String,
    val pageIndex: Int = 0,
    val builtInType: WidgetType? = null,
    val appWidgetId: Int? = null,
    val label: String = "",
    val widthPercent: Float = 1.0f, // 0.44f .. 1.0f freely DIY resizable
    val heightDp: Int = 148,        // 72 .. 420 dp freely DIY resizable
    val xOffsetDp: Float = 0f,      // DIY horizontal offset
    val yOffsetDp: Float = 0f       // DIY vertical offset
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
    RINGER_MODE("Mode Suara"),
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
    OBSIDIAN("iPhone 18 Obsidian", Color(0xFF03050A), Color(0xFF38BDF8)),
    LIQUID_PRISM("Liquid Prism Pro", Color(0xFF090F22), Color(0xFF818CF8)),
    FROSTED_GLASS("Kaca Bening", Color(0xFF1A2540), Color(0xFFFFFFFF)),
    AURORA_GLOW("Aurora Safir", Color(0xFF07152E), Color(0xFF22D3EE)),
    EMERALD("Zamrud Neon", Color(0xFF04261D), Color(0xFF34D399)),
    ROSE_QUARTZ("Kuarsa Mawar", Color(0xFF2D0A1A), Color(0xFFFB7185))
}

data class DynamicIslandConfig(
    val enabled: Boolean = true,
    val widthDp: Float = 166f,
    val heightDp: Float = 36f,
    val topOffsetDp: Float = 8f,
    val horizontalOffsetDp: Float = 0f,
    val cornerRadiusDp: Float = 24f,
    val style: DynamicIslandStyle = DynamicIslandStyle.OBSIDIAN,
    val borderGlow: Float = 0.80f,
    val showOnLockScreen: Boolean = true,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showNotifications: Boolean = true,
    val showMedia: Boolean = true,
    val showFocusTimer: Boolean = true,
    val autoExpandOnNotification: Boolean = true,
    val autoExpandOnSystemStatus: Boolean = true,
    val showDualSplitBubble: Boolean = true
)

enum class LedBlinkPattern(val label: String, val subtitle: String) {
    CLASSIC_BB("Kedip Klasik BB", "Kilatan tajam singkat ala LED BlackBerry asli"),
    DOUBLE_PULSE("Kedip Ganda 2x", "Dua kedipan cepat lalu jeda ala BBM"),
    HEARTBEAT("Denyut Jantung", "Kedipan ganda halus berirama organik"),
    BREATHING("Napas Halus", "Menyala & meredup perlahan elegan"),
    STROBE_FAST("Kedip Cepat", "Kedipan cepat tegas untuk pesan penting"),
    SOLID("Menyala Terus", "Lampu LED menyala stabil tanpa berkedip")
}

enum class LedPlacement(val label: String, val subtitle: String) {
    CAMERA_RING_A56("Melingkar Kamera (Samsung A56)", "Cincin LED bercahaya melingkari lubang kamera depan ala Samsung A56"),
    TOP_RIGHT_PAIR("Pojok Kanan Atas (BlackBerry)", "Posisi klasik 2 lampu LED di sudut kanan atas"),
    TOP_LEFT_PAIR("Pojok Kiri Atas", "Dua lampu LED sejajar di sudut kiri atas"),
    BESIDE_DYNAMIC_ISLAND("Mengapit Dynamic Island", "LED 1 di kiri & LED 2 di kanan Dynamic Island"),
    TOP_CENTER_PAIR("Tengah Atas", "Dua lampu LED berdampingan di tepi atas tengah")
}

enum class LedColorPreset(val label: String, val hex: Long) {
    BB_RED("Merah BlackBerry", 0xFFFF1744L),
    BBM_GREEN("Hijau BBM", 0xFF00E676L),
    CYAN_PULSE("Biru Sian", 0xFF00E5FFL),
    ROYAL_BLUE("Biru Royal", 0xFF2979FFL),
    AMBER_GOLD("Kuning Amber", 0xFFFFCA28L),
    VIOLET_NEON("Ungu Violet", 0xFFD500F9L),
    HOT_PINK("Pink Neon", 0xFFFF4081L),
    FLAME_ORANGE("Oranye Api", 0xFFFF6D00L),
    LIME_SPARK("Hijau Lime", 0xFFC6FF00L),
    PURE_WHITE("Putih Kristal", 0xFFFFFFFFL)
}

enum class LedIndicatorTrigger(val label: String, val subtitle: String) {
    SMART_AUTO("Otomatis Pintar BB", "Cas=Amber, Penuh=Hijau, Lemah=Merah, Sinyal=Biru/Pilihan"),
    CHARGING("Pengisian Daya", "Menyala saat HP sedang diisi daya atau penuh"),
    LOW_BATTERY("Baterai Lemah (≤20%)", "Berkedip peringatan saat daya baterai rendah"),
    NETWORK_SIGNAL("Sinyal & Wi-Fi Aktif", "Kedip indikator jaringan aktif ala BlackBerry klasik"),
    DND_SILENT("Jangan Ganggu / Hening", "Menyala saat mode DND atau Hening aktif"),
    ALWAYS_ON("Selalu Menyala", "Lampu indikator 2 selalu aktif saat layar menyala")
}

data class LedAppRule(
    val packageName: String,
    val colorHex: Long = LedColorPreset.BBM_GREEN.hex
)

data class LedLampConfig(
    val enabled: Boolean = true,
    val colorHex: Long = LedColorPreset.BB_RED.hex,
    val pattern: LedBlinkPattern = LedBlinkPattern.CLASSIC_BB,
    val blinkIntervalMs: Int = 1800, // 400 .. 4500 ms
    val durationSeconds: Int = 0,    // 0 = Terus sampai dibuka/selesai, atau 5..900 detik
    val sizeDp: Float = 10f,         // 6 .. 18 dp
    val glowIntensity: Float = 0.88f // 0.2 .. 1.0
)

data class BlackBerryLedConfig(
    val masterEnabled: Boolean = true,
    val placement: LedPlacement = LedPlacement.CAMERA_RING_A56,
    val topOffsetDp: Float = 9f,
    val horizontalOffsetDp: Float = 0f,
    val spacingDp: Float = 10f,
    val cameraRingDiameterDp: Float = 30f, // 18 .. 56 dp (Samsung A56 punch-hole default)
    val cameraRingStrokeDp: Float = 3.2f,  // 1.5 .. 7.0 dp
    val showOnLockScreen: Boolean = true,
    val showOverOtherApps: Boolean = true,
    // LED 1: Notifikasi Aplikasi Tertentu
    val appLed: LedLampConfig = LedLampConfig(
        enabled = true,
        colorHex = LedColorPreset.BB_RED.hex,
        pattern = LedBlinkPattern.CLASSIC_BB,
        blinkIntervalMs = 1700,
        durationSeconds = 0,
        sizeDp = 10f,
        glowIntensity = 0.92f
    ),
    val appLedAllAppsFallback: Boolean = true,
    val appRules: List<LedAppRule> = emptyList(),
    // LED 2: Indikator Sistem HP
    val indicatorLed: LedLampConfig = LedLampConfig(
        enabled = true,
        colorHex = LedColorPreset.BBM_GREEN.hex,
        pattern = LedBlinkPattern.BREATHING,
        blinkIntervalMs = 2400,
        durationSeconds = 0,
        sizeDp = 9.5f,
        glowIntensity = 0.85f
    ),
    val indicatorTrigger: LedIndicatorTrigger = LedIndicatorTrigger.SMART_AUTO,
    val indicatorUseAutoStatusColor: Boolean = true
)

enum class LockClockStyle(val label: String, val subtitle: String) {
    ADAPTIVE_LIQUID("Adaptive Clock", "Otomatis menyesuaikan ukuran & ketebalan sesuai ruang & notifikasi"),
    IOS_DEPTH("Liquid Depth Pro", "Angka besar berkilau kaca ala iPhone 18 Pro Max"),
    EDITORIAL_THIN("Editorial Tipis", "Tipografi tipis minimalis & elegan"),
    BOLD_KINETIC("Kinetik Tebal", "Jam tebal tegas dan mudah dibaca cepat"),
    SPLIT_STACK("Bertumpuk Vertikal", "Jam di atas, menit di bawah gaya poster modern"),
    ANALOG_HYBRID("Analog + Digital", "Dial analog kaca dipadu jam digital ringkas")
}

enum class LockClockColor(val label: String, val primary: Color, val accent: Color) {
    PURE_WHITE("Putih Kristal", Color(0xFFF8FAFC), Color(0xFF38BDF8)),
    AURORA_CYAN("Aurora Sian", Color(0xFF67E8F9), Color(0xFF818CF8)),
    ROSE_PEARL("Mutiara Mawar", Color(0xFFFDA4AF), Color(0xFFF472B6)),
    SUNSET_AMBER("Sinar Senja", Color(0xFFFDE68A), Color(0xFFF59E0B)),
    MINT_CRYSTAL("Kristal Mint", Color(0xFF6EE7B7), Color(0xFF2DD4BF)),
    LAVENDER_GLASS("Kaca Lavender", Color(0xFFC4B5FD), Color(0xFF38BDF8))
}

enum class SearchBarPosition(val label: String) {
    BOTTOM("Bawah Layar"),
    ABOVE_DOCK("Di Atas Dock"),
    MIDDLE("Tengah Layar"),
    TOP("Atas Layar"),
    FLOATING_DIY("Bebas DIY (Mana Saja)")
}

enum class SearchBarShape(val label: String, val cornerDp: Float) {
    PILL("Kapsul Penuh", 28f),
    LIQUID_ROUNDED("Kaca Melengkung", 18f),
    SOFT_SQUARE("Kotak Lembut", 10f),
    MINIMAL_LINE("Garis Minimalis", 4f)
}

enum class SearchBarColorPreset(
    val label: String,
    val bgColor: Color,
    val borderColor: Color,
    val contentColor: Color
) {
    FROSTED_GLASS("Kaca Bening", Color(0xFFFFFFFF), Color(0x55FFFFFF), Color(0xFFF8FAFC)),
    OBSIDIAN("Obsidian Gelap", Color(0xFF080E1E), Color(0x5538BDF8), Color(0xFFF8FAFC)),
    AURORA_CYAN("Aurora Sian", Color(0xFF0E7490), Color(0x8867E8F9), Color(0xFFF8FAFC)),
    ROSE_QUARTZ("Kuarsa Mawar", Color(0xFF9F1239), Color(0x88FDA4AF), Color(0xFFFFF1F2)),
    EMERALD_GLASS("Zamrud Kaca", Color(0xFF065F46), Color(0x886EE7B7), Color(0xFFECFDF5)),
    AMBER_GOLD("Emas Hangat", Color(0xFF92400E), Color(0x88FDE68A), Color(0xFFFFFBEB)),
    PURE_LIGHT("Putih Salju", Color(0xFFF1F5F9), Color(0xAAFFFFFF), Color(0xFF0F172A))
}

data class SearchBarConfig(
    val enabledOnHome: Boolean = true,
    val position: SearchBarPosition = SearchBarPosition.ABOVE_DOCK,
    val shape: SearchBarShape = SearchBarShape.PILL,
    val colorPreset: SearchBarColorPreset = SearchBarColorPreset.FROSTED_GLASS,
    val opacity: Float = 0.24f,
    val widthPercent: Float = 0.94f, // 0.50f .. 1.0f DIY resizable
    val heightDp: Int = 48,          // 40 .. 68 dp DIY resizable
    val xOffsetDp: Float = 0f,
    val yOffsetDp: Float = 0f
)

enum class BuiltInNotifSound(val id: String, val label: String, val subtitle: String) {
    CRYSTAL_DEW("builtin_crystal_dew", "Embun Kaca", "Dentingan kristal jernih ala iOS Liquid"),
    LIQUID_HARP("builtin_liquid_harp", "Harpa Cair", "Arpeggio harpa lembut menenangkan"),
    AURORA_PULSE("builtin_aurora_pulse", "Denyut Aurora", "Harmoni synth hangat bercahaya"),
    ZEN_GLASS_BELL("builtin_zen_bell", "Lonceng Kaca Zen", "Resonansi lonceng kaca murni"),
    SILK_MARIMBA("builtin_silk_marimba", "Marimba Sutra", "Ketukan kayu lembut modern"),
    PRISM_DROP("builtin_prism_drop", "Tetes Prisma", "Tetesan air kaca bernada lembut"),
    SILENT("builtin_silent", "Hening", "Tanpa suara notifikasi tambahan")
}

data class CustomNotificationSound(
    val id: String,
    val name: String,
    val uriString: String
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
    TOP_STATUS_BAR("Status Bar Atas", "Baris atas klasik yang juga bisa digeser bebas"),
    DYNAMIC_PILL("Pulau Kaca Atas", "Kapsul kaca mengambang bebas atur posisi"),
    INSIDE_DOCK("Terpadu di Dock", "Indikator ringkas menyatu dengan Dock Bar"),
    BOTTOM_CAPSULE("Kapsul Sudut Bawah", "Lencana mengambang di area bawah layar"),
    FREE_DIY("Bebas DIY (Mana Saja)", "Geser bebas indikator ke titik mana pun di layar"),
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

enum class WallpaperCategory(val label: String) {
    ALL("Semua"),
    LIVE("Live Animasi"),
    GLASS("Liquid Glass"),
    HD_GALLERY("HD & Galeri")
}

enum class WallpaperPreset(
    val label: String,
    val subtitle: String,
    val category: WallpaperCategory,
    val isLive: Boolean = false
) {
    // Live 60fps Animated Wallpapers
    LIVE_AURORA_WAVE(
        "Aurora Cair Live",
        "Gelombang aurora kaca bercahaya bergerak lembut real-time",
        WallpaperCategory.LIVE,
        isLive = true
    ),
    LIVE_NEBULA_ORBS(
        "Nebula Kosmik Live",
        "Bola plasma kromatik & taburan bintang kosmik hidup",
        WallpaperCategory.LIVE,
        isLive = true
    ),
    LIVE_LIQUID_LAVA(
        "Lava Kaca Live",
        "Cairan kaca amber & indigo mengalir dinamis modern",
        WallpaperCategory.LIVE,
        isLive = true
    ),
    LIVE_CYBER_GRID(
        "Horizon Neon 3D",
        "Grid perspektif futuristik dengan matahari neon bergerak",
        WallpaperCategory.LIVE,
        isLive = true
    ),
    LIVE_RAIN_GLASS(
        "Hujan di Kaca Live",
        "Tetes hujan & pantulan bokeh malam mengalir di kaca",
        WallpaperCategory.LIVE,
        isLive = true
    ),
    LIVE_SOLAR_DYNAMIC(
        "Langit Waktu Nyata",
        "Berubah otomatis mengikuti jam HP (Pagi, Siang, Senja, Malam)",
        WallpaperCategory.LIVE,
        isLive = true
    ),

    // Liquid Glass & Architectural Glass Wallpapers
    GLASS_REFRACTION(
        "Refraksi Kaca Bergaris",
        "Lensa kaca bergaris vertikal dengan bias pelangi safir",
        WallpaperCategory.GLASS,
        isLive = true
    ),
    GLASS_PRISM_BLOOM(
        "Prisma Kristal Kaca",
        "Kelopak kristal transparan berkilau lembut untuk ikon kaca",
        WallpaperCategory.GLASS,
        isLive = true
    ),

    // High-Res Bundled & Custom Gallery / System Wallpapers
    AURORA(
        "Aurora Safir HD",
        "Latar aurora kaca gelap beresolusi tinggi",
        WallpaperCategory.HD_GALLERY
    ),
    PRISM(
        "Kristal Prisma HD",
        "Refraksi cahaya kristal lembut untuk frosted glass",
        WallpaperCategory.HD_GALLERY
    ),
    DUSK(
        "Senja Alpin HD",
        "Gradasi senja atmosferik tenang beresolusi tinggi",
        WallpaperCategory.HD_GALLERY
    ),
    OBSIDIAN_MESH(
        "Obsidian AMOLED",
        "Mesh gelap minimalis hemat baterai layar AMOLED",
        WallpaperCategory.GLASS
    ),
    CUSTOM_PHOTO(
        "Foto Galeri HP",
        "Gunakan foto pilihan dari galeri + efek kaca, blur & bokeh",
        WallpaperCategory.HD_GALLERY
    ),
    SYSTEM_LIVE(
        "Wallpaper Asli HP",
        "Sinkron langsung dengan wallpaper bawaan / live HP Anda",
        WallpaperCategory.HD_GALLERY
    )
}

enum class WallpaperGlassStyle(val label: String, val subtitle: String) {
    NONE("Jernih Asli", "Tanpa lapisan efek kaca tambahan"),
    FROSTED_GLASS("Kaca Buram (Frost)", "Lapisan embun kaca halus ala iOS Liquid"),
    FLUTED_REEDED("Kaca Bergaris (Fluted)", "Efek lensa kaca arsitektural bergaris vertikal"),
    PRISM_CRYSTAL("Faset Prisma", "Bias cahaya diagonal kristal modern"),
    RAIN_DROPLETS("Embun Tetes Kaca", "Tetes embun kaca transparan yang estetik"),
    LIQUID_SHIMMER("Kilau Kaca Cair", "Kilauan cahaya bergerak halus melintasi layar")
}

enum class WallpaperParticleOverlay(val label: String) {
    NONE("Tanpa Partikel"),
    STARDUST("Debu Bintang"),
    BOKEH_ORBS("Bokeh Cahaya"),
    FIREFLIES("Kunang Neon"),
    SNOW_CRYSTAL("Kristal Salju")
}

enum class WallpaperColorTone(val label: String, val tint: Color) {
    NATURAL("Natural", Color.Transparent),
    COOL_SAPPHIRE("Safir Dingin", Color(0xFF0EA5E9)),
    WARM_SUNSET("Senja Hangat", Color(0xFFF59E0B)),
    ROSE_DREAM("Mawar Pastel", Color(0xFFF43F5E)),
    EMERALD_MIST("Kabut Zamrud", Color(0xFF10B981)),
    MONOCHROME_NOIR("Noir Monokrom", Color(0xFF0F172A))
}

data class WallpaperConfig(
    val preset: WallpaperPreset = WallpaperPreset.LIVE_AURORA_WAVE,
    val customImagePath: String = "",
    val glassStyle: WallpaperGlassStyle = WallpaperGlassStyle.FROSTED_GLASS,
    val glassIntensity: Float = 0.35f,
    val blurRadiusDp: Float = 0f,
    val dimAlpha: Float = 0.16f,
    val particleOverlay: WallpaperParticleOverlay = WallpaperParticleOverlay.STARDUST,
    val colorTone: WallpaperColorTone = WallpaperColorTone.NATURAL,
    val parallaxEnabled: Boolean = true,
    val parallaxStrength: Float = 0.60f,
    val liveSpeed: Float = 1.0f
)

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
    QuickModuleConfig(QuickModuleType.RINGER_MODE, true, 0.48f, 74),
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
    HomeWidgetPlacement("w_clock_0", pageIndex = 0, builtInType = WidgetType.CLOCK_WEATHER, widthPercent = 1.0f, heightDp = 128),
    HomeWidgetPlacement("w_notif_0", pageIndex = 0, builtInType = WidgetType.LIVE_NOTIFICATIONS, widthPercent = 1.0f, heightDp = 156),
    HomeWidgetPlacement("w_analog_1", pageIndex = 1, builtInType = WidgetType.ANALOG_CLOCK, widthPercent = 0.48f, heightDp = 152),
    HomeWidgetPlacement("w_battery_1", pageIndex = 1, builtInType = WidgetType.BATTERY_SYSTEM, widthPercent = 0.48f, heightDp = 152),
    HomeWidgetPlacement("w_media_1", pageIndex = 1, builtInType = WidgetType.MEDIA_WAVEFORM, widthPercent = 1.0f, heightDp = 136),
    HomeWidgetPlacement("w_world_2", pageIndex = 2, builtInType = WidgetType.WORLD_CLOCK, widthPercent = 1.0f, heightDp = 124),
    HomeWidgetPlacement("w_storage_2", pageIndex = 2, builtInType = WidgetType.STORAGE_RAM, widthPercent = 1.0f, heightDp = 132)
)

fun defaultLockWidgets(): List<HomeWidgetPlacement> = listOf(
    HomeWidgetPlacement("lw_weather_0", pageIndex = 0, builtInType = WidgetType.WEATHER_PRO, widthPercent = 0.48f, heightDp = 96),
    HomeWidgetPlacement("lw_battery_0", pageIndex = 0, builtInType = WidgetType.BATTERY_SYSTEM, widthPercent = 0.48f, heightDp = 96)
)

data class LauncherConfig(
    // 1. Dock Bar
    val dockLayout: DockLayoutMode = DockLayoutMode.FLOATING_BOTTOM,
    val dockSideAlignment: DockSideAlignment = DockSideAlignment.CENTER,
    val dockSideVerticalOffsetDp: Float = 0f,
    val dockColor: DockColorPreset = DockColorPreset.FROSTED_CLEAR,
    val dockOpacity: Float = 0.24f,
    val dockCornerRadius: Float = 28f,
    val dockMaxIcons: Int = 5,
    val showDockLabels: Boolean = false,
    val pinnedDockPackages: List<String> = emptyList(),

    // 2. Widgets (14 Modern Glass + Real Android AppWidgets + DIY Layout)
    val widgetStackMode: WidgetStackMode = WidgetStackMode.SMART_STACK,
    val enabledWidgets: List<WidgetType> = listOf(
        WidgetType.CLOCK_WEATHER,
        WidgetType.ANALOG_CLOCK,
        WidgetType.MEDIA_WAVEFORM,
        WidgetType.WEATHER_PRO,
        WidgetType.BATTERY_SYSTEM,
        WidgetType.WORLD_CLOCK,
        WidgetType.LIVE_NOTIFICATIONS,
        WidgetType.STORAGE_RAM,
        WidgetType.CALENDAR_MINI,
        WidgetType.QUICK_NOTE
    ),
    val homeWidgets: List<HomeWidgetPlacement> = defaultHomeWidgetPlacements(),
    val lockWidgets: List<HomeWidgetPlacement> = defaultLockWidgets(),
    val widgetGlassOpacity: Float = 0.22f,
    val quickNoteText: String = "Tekan lama widget atau pencarian untuk mode DIY bebas atur ukuran & posisi!",

    // 3. Universal Customizable Search Bar (Home + App Drawer + Studio)
    val searchBar: SearchBarConfig = SearchBarConfig(),

    // 4. Quick Panel (Live Horizontal/Vertical & Direct Instant Modules)
    val quickPanelOrientation: QuickPanelOrientation = QuickPanelOrientation.VERTICAL_IOS,
    val quickPanelGlassOpacity: Float = 0.26f,
    val quickModules: List<QuickModuleConfig> = defaultQuickModules(),
    val showBrightnessSlider: Boolean = true,
    val showVolumeSlider: Boolean = true,
    val showMediaControl: Boolean = true,
    val sliderHeightDp: Int = 152,
    val replaceSystemControlPanel: Boolean = true,
    val edgeTriggerZone: EdgePanelTriggerZone = EdgePanelTriggerZone.TOP_RIGHT,

    // 5. Dynamic Island (iPhone 18 Pro Max Dynamic Expanding/Collapsing)
    val dynamicIsland: DynamicIslandConfig = DynamicIslandConfig(),

    // 5b. Dual BlackBerry LED Notification & System Indicator Lamps
    val ledConfig: BlackBerryLedConfig = BlackBerryLedConfig(),

    // 6. Status Indicators (Hide native status bar & free layout anywhere)
    val hideSystemStatusBar: Boolean = true,
    val statusPosition: StatusIndicatorPosition = StatusIndicatorPosition.TOP_STATUS_BAR,
    val statusAlignment: StatusSlotAlignment = StatusSlotAlignment.CLASSIC_SPLIT,
    val statusOffsetXDp: Float = 0f,
    val statusOffsetYDp: Float = 0f,
    val statusScale: Float = 1.0f,
    val statusWidthPercent: Float = 1.0f,
    val batteryStyle: BatteryIconStyle = BatteryIconStyle.CAPSULE,
    val showClockIndicator: Boolean = true,
    val showSignalIndicator: Boolean = true,
    val showWifiIndicator: Boolean = true,
    val showBatteryPercent: Boolean = true,

    // 7. Native Notification Replacement & Aesthetic Notification Sounds
    val replaceSystemNotifications: Boolean = true,
    val showSystemWideDynamicIsland: Boolean = true,
    val activeNotifSoundId: String = BuiltInNotifSound.CRYSTAL_DEW.id,
    val customNotifSounds: List<CustomNotificationSound> = emptyList(),
    val homePopupStyle: PopupStyle = PopupStyle.DYNAMIC_ISLAND,
    val lockPopupStyle: PopupStyle = PopupStyle.STACKED_DECK,
    val popupCornerRadius: Float = 26f,
    val popupGlassOpacity: Float = 0.30f,

    // 8. Pages, Double-Tap Lock, Adaptive Lock Clock & Security Passcode
    val homePageCount: Int = 3,
    val homeGridColumns: Int = 4,
    val homeGridRows: Int = 5,
    val homePageScale: Float = 1.0f,
    val lockPageCount: Int = 3,
    val lockClockStyle: LockClockStyle = LockClockStyle.ADAPTIVE_LIQUID,
    val lockClockColor: LockClockColor = LockClockColor.PURE_WHITE,
    val lockClockScale: Float = 1.0f,
    val lockClockVerticalOffsetDp: Float = 0f,
    val lockClockWeight: Int = 700,
    val lockPageScale: Float = 1.0f,
    val replaceSystemLockScreen: Boolean = true,
    val doubleTapToLock: Boolean = true,
    val lockSecurityMode: LockSecurityMode = LockSecurityMode.NONE,
    val lockPasscodeHash: String = "",
    val lockPasscodeLength: Int = 4,

    // 9. Page Transitions
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.FLUID_GLASS,
    val transitionSpeed: TransitionSpeed = TransitionSpeed.BALANCED,

    // 10. Customizable Glassy Sidebar v7
    val showGlassySidebar: Boolean = true,
    val sidebarSide: SidebarSide = SidebarSide.LEFT,
    val sidebarWidthDp: Float = 228f,
    val sidebarGlassOpacity: Float = 0.84f,
    val sidebarItems: List<SidebarItemConfig> = defaultSidebarItems(),
    val ambientGlowIntensity: Float = 0.80f,

    // 11. Visual Theme & Modern Wallpaper Engine (Home & Optional Separate Lock Screen)
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.AURORA,
    val wallpaperConfig: WallpaperConfig = WallpaperConfig(
        preset = WallpaperPreset.AURORA,
        glassStyle = WallpaperGlassStyle.NONE,
        particleOverlay = WallpaperParticleOverlay.NONE
    ),
    val separateLockWallpaper: Boolean = false,
    val lockWallpaperConfig: WallpaperConfig = WallpaperConfig(
        preset = WallpaperPreset.LIVE_AURORA_WAVE,
        glassStyle = WallpaperGlassStyle.FROSTED_GLASS,
        particleOverlay = WallpaperParticleOverlay.STARDUST
    )
) {
    fun effectiveHomeWallpaper(): WallpaperConfig = wallpaperConfig
    fun effectiveLockWallpaper(): WallpaperConfig =
        if (separateLockWallpaper) lockWallpaperConfig else wallpaperConfig
}

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
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
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
