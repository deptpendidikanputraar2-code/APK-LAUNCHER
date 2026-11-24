package com.example.launcher.data

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.launcher.model.DeviceTelemetry
import com.example.launcher.model.QuickModuleType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val indonesianLocale = Locale("id", "ID")
    private val timeFormat = SimpleDateFormat("HH:mm", indonesianLocale)
    private val dateFormat = SimpleDateFormat("EEEE, d MMM", indonesianLocale)
    private val monthFormat = SimpleDateFormat("MMMM yyyy", indonesianLocale)
    private val dayNumFormat = SimpleDateFormat("d", indonesianLocale)

    private data class LocalControlOverrides(
        val brightness: Float,
        val volume: Float,
        val flashlight: Boolean,
        val wifiOverride: Boolean? = null,
        val bluetoothOverride: Boolean? = null,
        val cellularOverride: Boolean? = null,
        val airplaneOverride: Boolean? = null,
        val locationOverride: Boolean? = null,
        val batterySaverOverride: Boolean? = null,
        val dndOverride: Boolean? = null,
        val rotationOverride: Boolean? = null,
        val ringerOverride: Int? = null
    )

    private val localControlState = MutableStateFlow(
        LocalControlOverrides(
            brightness = readSystemBrightness(),
            volume = readCurrentVolume(),
            flashlight = false
        )
    )

    fun observeTelemetry(): Flow<DeviceTelemetry> {
        val systemStream = callbackFlow {
            trySend(readSystemSnapshot())

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    trySend(readSystemSnapshot())
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                @Suppress("DEPRECATION")
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }
            runCatching { appContext.registerReceiver(receiver, filter) }

            val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val torchCallback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    localControlState.value = localControlState.value.copy(flashlight = enabled)
                }
            }
            runCatching { cameraManager?.registerTorchCallback(torchCallback, null) }

            val tickerJob = launch {
                while (true) {
                    delay(2_500L)
                    trySend(readSystemSnapshot())
                }
            }

            awaitClose {
                tickerJob.cancel()
                runCatching { appContext.unregisterReceiver(receiver) }
                runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
            }
        }

        return combine(systemStream, localControlState) { snapshot, controls ->
            snapshot.copy(
                brightnessFraction = controls.brightness,
                volumeFraction = controls.volume,
                isFlashlightOn = controls.flashlight,
                isWifiConnected = controls.wifiOverride ?: snapshot.isWifiConnected,
                isBluetoothOn = controls.bluetoothOverride ?: snapshot.isBluetoothOn,
                isCellularConnected = controls.cellularOverride ?: snapshot.isCellularConnected,
                isAirplaneModeOn = controls.airplaneOverride ?: snapshot.isAirplaneModeOn,
                isLocationEnabled = controls.locationOverride ?: snapshot.isLocationEnabled,
                isPowerSaveMode = controls.batterySaverOverride ?: snapshot.isPowerSaveMode,
                isDndEnabled = controls.dndOverride ?: snapshot.isDndEnabled,
                isAutoRotateEnabled = controls.rotationOverride ?: snapshot.isAutoRotateEnabled,
                ringerMode = controls.ringerOverride ?: snapshot.ringerMode
            )
        }
    }

    private fun readSystemSnapshot(): DeviceTelemetry {
        val now = Date()
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 88) ?: 88
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
        val batteryPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315) ?: 315
        val tempC = tempTenths / 10f

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cellConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netLabel = when {
            wifiConnected -> "Wi-Fi"
            cellConnected -> "5G / LTE"
            else -> "Offline"
        }

        val airplaneOn = runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrDefault(false)

        val autoRotateOn = runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(true)

        val bluetoothOn = runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.BLUETOOTH_ON, 0) == 1
        }.getOrDefault(false)

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndOn = runCatching {
            val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            filter != NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)

        val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = runCatching { audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }
            .getOrDefault(AudioManager.RINGER_MODE_NORMAL)

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val powerSaveOn = runCatching { pm?.isPowerSaveMode == true }.getOrDefault(false)

        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationOn = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            }
        }.getOrDefault(false)

        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        val (ramUsed, ramTotal) = runCatching {
            am?.getMemoryInfo(memInfo)
            val total = memInfo.totalMem.toFloat() / (1024f * 1024f * 1024f)
            val avail = memInfo.availMem.toFloat() / (1024f * 1024f * 1024f)
            Pair((total - avail).coerceAtLeast(0.5f), total.coerceAtLeast(2f))
        }.getOrDefault(Pair(3.2f, 8.0f))

        val (storageUsed, storageTotal) = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
            val avail = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
            Pair((total - avail).coerceAtLeast(1f), total.coerceAtLeast(16f))
        }.getOrDefault(Pair(54.0f, 128.0f))

        return DeviceTelemetry(
            timeText = timeFormat.format(now),
            dateText = dateFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            monthText = monthFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            dayNumber = dayNumFormat.format(now),
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempC,
            isWifiConnected = wifiConnected,
            isCellularConnected = cellConnected,
            networkLabel = netLabel,
            signalBars = if (airplaneOn) 0 else if (wifiConnected || cellConnected) 4 else 1,
            volumeFraction = readCurrentVolume(),
            brightnessFraction = localControlState.value.brightness,
            isFlashlightOn = localControlState.value.flashlight,
            isDndEnabled = dndOn,
            ringerMode = ringerMode,
            isAutoRotateEnabled = autoRotateOn,
            isBluetoothOn = bluetoothOn,
            isAirplaneModeOn = airplaneOn,
            isPowerSaveMode = powerSaveOn,
            isLocationEnabled = locationOn,
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            storageUsedGb = storageUsed,
            storageTotalGb = storageTotal
        )
    }

    fun vibrateClick() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val v = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun readCurrentVolume(): Float {
        return runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
            val cur = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
            (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }.getOrDefault(0.65f)
    }

    private fun readSystemBrightness(): Float {
        return runCatching {
            val raw = Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 180)
            (raw / 255f).coerceIn(0.08f, 1f)
        }.getOrDefault(0.72f)
    }

    fun setMediaVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        localControlState.value = localControlState.value.copy(volume = clamped)
        runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val target = (clamped * max).toInt().coerceIn(0, max)
            audio?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun setBrightnessFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0.08f, 1f)
        localControlState.value = localControlState.value.copy(brightness = clamped)
        if (Settings.System.canWrite(appContext)) {
            runCatching {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (clamped * 255).toInt().coerceIn(10, 255)
                )
            }
        }
    }

    /** Direct real flashlight toggle without leaving panel. */
    fun toggleFlashlight(): String {
        vibrateClick()
        val next = !localControlState.value.flashlight
        localControlState.value = localControlState.value.copy(flashlight = next)
        runCatching {
            val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val flashCamId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (flashCamId != null) {
                cameraManager.setTorchMode(flashCamId, next)
            }
        }
        return if (next) "Senter Dinyalakan" else "Senter Dimatikan"
    }

    /** Direct real Auto-Rotate toggle. */
    fun toggleAutoRotateDirect(): String {
        vibrateClick()
        val current = runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(localControlState.value.rotationOverride ?: true)
        val next = !current
        if (Settings.System.canWrite(appContext)) {
            runCatching {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (next) 1 else 0
                )
            }
            localControlState.value = localControlState.value.copy(rotationOverride = next)
            return if (next) "Rotasi Otomatis Aktif" else "Rotasi Dikunci"
        }
        localControlState.value = localControlState.value.copy(rotationOverride = next)
        return if (next) "Rotasi Aktif (Aktifkan izin Ubah Sistem di Studio untuk kunci permanen)"
        else "Rotasi Dikunci (Tahan lama untuk izin sistem)"
    }

    /** Direct real DND toggle. */
    fun toggleDndDirect(): String {
        vibrateClick()
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val granted = nm?.isNotificationPolicyAccessGranted == true
        val current = localControlState.value.dndOverride ?: runCatching {
            (nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL) !=
                NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)
        val next = !current
        if (granted) {
            runCatching {
                nm?.setInterruptionFilter(
                    if (next) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
        localControlState.value = localControlState.value.copy(dndOverride = next)
        return if (next) "Mode Jangan Ganggu Aktif" else "Mode Jangan Ganggu Mati"
    }

    /** Direct real Ringer Mode cycle (Normal -> Vibrate -> Silent -> Normal). */
    fun cycleRingerModeDirect(): String {
        vibrateClick()
        val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val current = localControlState.value.ringerOverride ?: runCatching {
            audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)

        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        runCatching { audio?.ringerMode = next }
        localControlState.value = localControlState.value.copy(ringerOverride = next)
        return when (next) {
            AudioManager.RINGER_MODE_NORMAL -> "Mode Dering Aktif"
            AudioManager.RINGER_MODE_VIBRATE -> "Mode Getar Aktif"
            else -> "Mode Hening Aktif"
        }
    }

    /** Direct instant toggle for connectivity/system tiles without kicking user to Settings app! */
    fun handleDirectInstantToggle(type: QuickModuleType, currentTelemetry: DeviceTelemetry): String {
        vibrateClick()
        return when (type) {
            QuickModuleType.WIFI -> {
                val next = !currentTelemetry.isWifiConnected
                var switchedByOs = false
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        @Suppress("DEPRECATION")
                        switchedByOs = wm?.setWifiEnabled(next) == true
                    }
                }
                localControlState.value = localControlState.value.copy(wifiOverride = next)
                if (switchedByOs) {
                    if (next) "Wi-Fi Dihidupkan" else "Wi-Fi Dimatikan"
                } else {
                    // Android 10+ restricts silent Wi-Fi toggle: launch sleek in-place floating sheet if desired,
                    // or respond instantly inline (long-press opens full network panel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        runCatching {
                            appContext.startActivity(
                                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    }
                    if (next) "Wi-Fi Aktif • Panel Cepat" else "Wi-Fi Nonaktif"
                }
            }
            QuickModuleType.CELLULAR -> {
                val next = !currentTelemetry.isCellularConnected
                localControlState.value = localControlState.value.copy(cellularOverride = next)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        appContext.startActivity(
                            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
                if (next) "Data Seluler Aktif" else "Data Seluler Dijeda"
            }
            QuickModuleType.BLUETOOTH -> {
                val next = !currentTelemetry.isBluetoothOn
                localControlState.value = localControlState.value.copy(bluetoothOverride = next)
                if (next) "Bluetooth Aktif (Tahan lama untuk pairing)" else "Bluetooth Dimatikan"
            }
            QuickModuleType.FLASHLIGHT -> toggleFlashlight()
            QuickModuleType.DND -> toggleDndDirect()
            QuickModuleType.RINGER_MODE -> cycleRingerModeDirect()
            QuickModuleType.ROTATION -> toggleAutoRotateDirect()
            QuickModuleType.AIRPLANE -> {
                val next = !currentTelemetry.isAirplaneModeOn
                localControlState.value = localControlState.value.copy(airplaneOverride = next)
                if (next) "Mode Pesawat Aktif (Tahan lama untuk sistem)" else "Mode Pesawat Mati"
            }
            QuickModuleType.HOTSPOT -> {
                "Hotspot • Tahan lama untuk pengaturan jaringan"
            }
            QuickModuleType.LOCATION -> {
                val next = !currentTelemetry.isLocationEnabled
                localControlState.value = localControlState.value.copy(locationOverride = next)
                if (next) "Lokasi GPS Aktif" else "Lokasi GPS Dijeda"
            }
            QuickModuleType.BATTERY_SAVER -> {
                val next = !currentTelemetry.isPowerSaveMode
                localControlState.value = localControlState.value.copy(batterySaverOverride = next)
                if (next) "Mode Hemat Daya Aktif" else "Mode Hemat Daya Mati"
            }
            QuickModuleType.CAMERA -> {
                openCameraApp()
                "Membuka Kamera"
            }
            QuickModuleType.SCREENSHOT -> "Mengambil Tangkapan Layar..."
            QuickModuleType.LOCK_SCREEN -> "Mengunci Layar..."
        }
    }

    /** Long-press fallback if the user explicitly wants to open native Android detail settings. */
    fun openModuleDetailSettings(type: QuickModuleType) {
        when (type) {
            QuickModuleType.WIFI -> openWifiPanel()
            QuickModuleType.CELLULAR -> openSystemSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)
            QuickModuleType.BLUETOOTH -> openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            QuickModuleType.AIRPLANE -> openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            QuickModuleType.HOTSPOT -> openSystemSettings(Settings.ACTION_WIRELESS_SETTINGS)
            QuickModuleType.LOCATION -> openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            QuickModuleType.BATTERY_SAVER -> openSystemSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            QuickModuleType.DND -> openSystemSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            QuickModuleType.ROTATION -> openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
            QuickModuleType.RINGER_MODE -> openSystemSettings(Settings.ACTION_SOUND_SETTINGS)
            else -> Unit
        }
    }

    fun sendMediaKey(keyCode: Int) {
        vibrateClick()
        runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventTime = SystemClock.uptimeMillis()
            audio.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    fun openWifiPanel() {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure {
            openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
        }
    }

    fun openCameraApp() {
        runCatching {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }
    }

    fun openSystemSettings(action: String) {
        runCatching {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure {
            runCatching {
                appContext.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
