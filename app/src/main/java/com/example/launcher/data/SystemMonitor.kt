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
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.launcher.model.DeviceTelemetry
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

    private val localControlState = MutableStateFlow(
        LocalControlOverrides(
            brightness = readSystemBrightness(),
            volume = readCurrentVolume(),
            flashlight = false
        )
    )

    private data class LocalControlOverrides(
        val brightness: Float,
        val volume: Float,
        val flashlight: Boolean
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
                    delay(3_000L)
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
                isFlashlightOn = controls.flashlight
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

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var wifiConnected = false
        var cellularConnected = false
        if (cm != null) {
            val activeNetwork = cm.activeNetwork
            val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (caps != null) {
                wifiConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellularConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }

        val currentVol = readCurrentVolume()
        if (kotlin.math.abs(currentVol - localControlState.value.volume) > 0.04f) {
            localControlState.value = localControlState.value.copy(volume = currentVol)
        }
        val sysBright = readSystemBrightness()
        if (kotlin.math.abs(sysBright - localControlState.value.brightness) > 0.08f) {
            localControlState.value = localControlState.value.copy(brightness = sysBright)
        }

        val (ramUsed, ramTotal) = readRamStatsGb()
        val (storageUsed, storageTotal) = readStorageStatsGb()

        val networkLabel = when {
            readAirplaneMode() -> "Pesawat"
            wifiConnected -> "Wi-Fi"
            cellularConnected -> "Seluler"
            else -> "Offline"
        }

        return DeviceTelemetry(
            timeText = timeFormat.format(now),
            dateText = dateFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            monthText = monthFormat.format(now).replaceFirstChar { it.uppercase(indonesianLocale) },
            dayNumber = dayNumFormat.format(now),
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempTenths / 10f,
            isWifiConnected = wifiConnected,
            isCellularConnected = cellularConnected,
            networkLabel = networkLabel,
            signalBars = if (readAirplaneMode()) 0 else if (cellularConnected || wifiConnected) 4 else 1,
            volumeFraction = currentVol,
            brightnessFraction = localControlState.value.brightness,
            isFlashlightOn = localControlState.value.flashlight,
            isDndEnabled = readDndEnabled(),
            isAutoRotateEnabled = readAutoRotate(),
            isBluetoothOn = readBluetoothEnabled(),
            isAirplaneModeOn = readAirplaneMode(),
            isPowerSaveMode = readPowerSaveMode(),
            isLocationEnabled = readLocationEnabled(),
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            storageUsedGb = storageUsed,
            storageTotalGb = storageTotal
        )
    }

    fun setMediaVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        localControlState.value = localControlState.value.copy(volume = clamped)
        runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = (clamped * max).toInt().coerceIn(0, max)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun setBrightnessFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0.05f, 1f)
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
                    (clamped * 255f).toInt().coerceIn(10, 255)
                )
            }
        }
    }

    fun toggleFlashlight(): Boolean {
        val next = !localControlState.value.flashlight
        runCatching {
            val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val flashCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (flashCameraId != null) {
                cameraManager.setTorchMode(flashCameraId, next)
            }
        }
        localControlState.value = localControlState.value.copy(flashlight = next)
        return next
    }

    fun toggleDndReal(): Boolean {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val currentlyOn = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            val target = if (currentlyOn) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY
            runCatching { nm.setInterruptionFilter(target) }
            return !currentlyOn
        }
        openSystemSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        return false
    }

    fun toggleAutoRotateReal(): Boolean {
        if (Settings.System.canWrite(appContext)) {
            val next = if (readAutoRotate()) 0 else 1
            runCatching {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    next
                )
            }
            return next == 1
        }
        openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
        return readAutoRotate()
    }

    fun sendMediaKey(keyCode: Int) {
        runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val now = SystemClock.uptimeMillis()
            audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    fun openCameraApp() {
        runCatching {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure {
            openSystemSettings(Settings.ACTION_SETTINGS)
        }
    }

    fun openWifiPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                appContext.startActivity(
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                return
            }
        }
        openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
    }

    fun openSystemSettings(action: String) {
        runCatching {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(fallback)
            }
        }
    }

    private fun readCurrentVolume(): Float {
        return runCatching {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.65f
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }.getOrDefault(0.65f)
    }

    private fun readSystemBrightness(): Float {
        return runCatching {
            val raw = Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 180)
            (raw / 255f).coerceIn(0.05f, 1f)
        }.getOrDefault(0.70f)
    }

    private fun readAutoRotate(): Boolean {
        return runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(true)
    }

    private fun readBluetoothEnabled(): Boolean {
        return runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.BLUETOOTH_ON, 0) == 1
        }.getOrDefault(false)
    }

    private fun readAirplaneMode(): Boolean {
        return runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrDefault(false)
    }

    private fun readDndEnabled(): Boolean {
        return runCatching {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)
    }

    private fun readPowerSaveMode(): Boolean {
        return runCatching {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        }.getOrDefault(false)
    }

    private fun readLocationEnabled(): Boolean {
        return runCatching {
            val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            }
        }.getOrDefault(true)
    }

    private fun readRamStatsGb(): Pair<Float, Float> {
        return runCatching {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return 3.0f to 8.0f
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalGb = info.totalMem / (1024f * 1024f * 1024f)
            val availGb = info.availMem / (1024f * 1024f * 1024f)
            (totalGb - availGb).coerceAtLeast(0.1f) to totalGb.coerceAtLeast(1f)
        }.getOrDefault(3.2f to 8.0f)
    }

    private fun readStorageStatsGb(): Pair<Float, Float> {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalGb = stat.totalBytes / (1024f * 1024f * 1024f)
            val availGb = stat.availableBytes / (1024f * 1024f * 1024f)
            (totalGb - availGb).coerceAtLeast(0.1f) to totalGb.coerceAtLeast(1f)
        }.getOrDefault(48f to 128f)
    }
}
